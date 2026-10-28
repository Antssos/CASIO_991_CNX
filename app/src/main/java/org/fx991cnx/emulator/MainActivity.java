package org.fx991cnx.emulator;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.util.AtomicFile;
import android.view.*;
import android.widget.Toast;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.concurrent.*;
import java.util.zip.CRC32;

public class MainActivity extends Activity {
 private CalculatorView panel;
 UiPreferences ui;
 private SoundPool clickPool;
 private int clickSample;
 private volatile boolean clickLoaded=false;
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
 private volatile boolean running=false,ready=false;
 private String digest="";
 private int held=-1;
 private long heldAt;
 private final java.util.ArrayDeque<int[]> keys=new java.util.ArrayDeque<>();
 private long frames=0;
 LcdSettings settings;
 private boolean heldConsumed=false;
 @Override public void onCreate(Bundle b){super.onCreate(b);ui=new UiPreferences(this);settings=new LcdSettings(this);initFeedback();getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().getAttributes().layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;panel=new CalculatorView(this);setContentView(panel);fullscreen();worker.execute(()->{installBundledRom();if(rom().isFile())open();});worker.scheduleWithFixedDelay(this::tick,0,16,TimeUnit.MILLISECONDS);}
 private void installBundledRom(){
  if(rom().isFile())return;
  // Only a separately supplied, licensed firmware asset can enable first-run provisioning.
  try(InputStream in=getAssets().open("firmware/fx991cnx.rom")){
   byte[] bytes=readAll(in);if(bytes.length!=262144)throw new IOException("内置 ROM 长度不正确");
   try(InputStream grant=getAssets().open("firmware/REDISTRIBUTION-LICENSE.txt")){if(readAll(grant).length==0)throw new IOException("缺少固件授权声明");}
   AtomicFile file=new AtomicFile(rom());FileOutputStream out=file.startWrite();try{out.write(bytes);file.finishWrite(out);}catch(IOException e){file.failWrite(out);throw e;}
  }catch(FileNotFoundException absent){}catch(IOException error){runOnUiThread(()->Toast.makeText(this,"内置固件安装失败，请手动导入："+error.getMessage(),Toast.LENGTH_LONG).show());}
 }
 private File rom(){return new File(getFilesDir(),"rom.bin");}
 private void tick(){if(!ready||!running)return;try{
  long now=SystemClock.uptimeMillis();
  synchronized(keys){if(!keys.isEmpty()){
   int[] event=keys.peek();
   if(event[1]==1 && held<0){keys.remove();held=event[0];heldAt=now;heldConsumed=settings.keyDown(held,NativeCore.frame(0));if(!heldConsumed)NativeCore.key(held,true);}
   else if(event[1]==0 && now-heldAt>=64){keys.remove();if(!heldConsumed)NativeCore.key(event[0],false);held=-1;heldConsumed=false;}
  }}
  byte[] lcd=NativeCore.frame(32000);panel.lcd=lcd;panel.postInvalidate();
  if(++frames%300==0){save();String e=NativeCore.error();if(!e.isEmpty())fail(e);}
 }catch(Exception e){fail("运行失败："+e.getMessage());}}
 void key(int code,boolean down){if(!ready)return;synchronized(keys){keys.add(new int[]{code,down?1:0});}}
 private void release(){synchronized(keys){keys.clear();if(held>=0&&!heldConsumed)NativeCore.key(held,false);held=-1;heldConsumed=false;}}
 private void open(){try{release();settings.close();ready=false;digest=hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(rom().toPath())));String e=NativeCore.open(rom().getPath());if(!e.isEmpty()){fail(e);return;}File state=stateFile();if(state.isFile()){
  try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(new AtomicFile(state).readFully()))){
   if(in.readInt()!=0x46585331)throw new IOException();int n=in.readInt();if(n<1||n>65536)throw new IOException();long crc=in.readLong();byte[] data=new byte[n];in.readFully(data);CRC32 sum=new CRC32();sum.update(data);if(sum.getValue()!=crc||!NativeCore.restore(data))throw new IOException();
  }catch(Exception bad){runOnUiThread(()->Toast.makeText(this,"状态文件不可用，已冷启动；ROM 已保留",Toast.LENGTH_LONG).show());}
 }ready=true;panel.ready=true;panel.postInvalidate();}catch(Exception e){fail("无法加载 ROM："+e.getMessage());}}
 private File stateFile(){return new File(getFilesDir(),"state-v1-"+digest+".bin");}
 private void save(){if(!ready)return;byte[] data=NativeCore.save();if(data.length==0)return;AtomicFile f=new AtomicFile(stateFile());FileOutputStream out=null;try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);CRC32 crc=new CRC32();crc.update(data);d.writeInt(0x46585331);d.writeInt(data.length);d.writeLong(crc.getValue());d.write(data);out=f.startWrite();out.write(bytes.toByteArray());f.finishWrite(out);}catch(IOException e){if(out!=null)f.failWrite(out);runOnUiThread(()->Toast.makeText(this,"保存状态失败",Toast.LENGTH_SHORT).show());}}
 private void fail(String message){ready=false;panel.ready=false;runOnUiThread(()->new AlertDialog.Builder(this).setTitle("ROM 未能运行").setMessage(message+"\n请确认是 fx-991CN X 的完整原机 ROM。请检查固件型号和完整性。").setPositiveButton("重新导入",(d,w)->importRom()).setNegativeButton("关闭",null).show());}
 void importRom(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=1||result!=RESULT_OK||data==null)return;Uri uri=data.getData();worker.execute(()->{
  try(InputStream in=getContentResolver().openInputStream(uri)){
   if(in==null)throw new IOException("无法读取文件");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))!=-1){out.write(chunk,0,n);if(out.size()>262144)throw new IOException("仅支持 256 KiB 原机 ROM");}byte[] bytes=out.toByteArray();
   if(bytes.length!=262144)throw new IOException("ROM 必须是 262144 字节（256 KiB），不支持压缩包或 CW/ES 固件");
   int sp=(bytes[0]&255)|((bytes[1]&255)<<8),pc=(bytes[2]&255)|((bytes[3]&255)<<8);
   if(sp<0xD000||sp>0xF000||(sp&1)!=0||pc==0||pc==0xFFFF||(pc&1)!=0)throw new IOException("ROM 向量不符合 ClassWiz X 原机格式");
   String ascii=new String(bytes,java.nio.charset.StandardCharsets.ISO_8859_1);boolean known=hex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals("8b6b6430f96858a8b3e765796e2d6f340bc7073ac9b0aa464775f15e53a10788");boolean identified=known||ascii.contains("991CN")||ascii.contains("991cn");
   runOnUiThread(()->new AlertDialog.Builder(this).setTitle(known?"已识别 fx-991CN X":identified?"检测到 fx-991CN 系列标识":"检测到 ClassWiz X 格式候选 ROM")
    .setMessage((known?"已匹配 fx-991CN X 固件指纹。":identified?"仍需确认这是 fx-991CN X。":"无法仅凭文件大小可靠识别具体机型。请确认这是 fx-991CN X 原机 ROM；其他 X 机型不会转换为中文版。")+"\n\n仅导入您有权使用的固件。文件只保存在本应用内部，不上传。")
    .setPositiveButton("确认并导入",(d,w)->worker.execute(()->install(bytes))).setNegativeButton("取消",null).show());
  }catch(Exception e){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("未导入").setMessage(e.getMessage()).setPositiveButton("知道了",null).show());}
 });}
 private void install(byte[] bytes){release();save();ready=false;try{AtomicFile f=new AtomicFile(rom());FileOutputStream out=f.startWrite();try{out.write(bytes);f.finishWrite(out);}catch(Exception e){f.failWrite(out);throw e;}open();}catch(Exception e){fail(e.getMessage());}}
 private void initFeedback(){
  clickPool=new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
  clickPool.setOnLoadCompleteListener((pool,id,status)->{if(status==0)clickLoaded=true;});clickSample=clickPool.load(this,R.raw.key_click,1);
 }
 void keyFeedback(){
  if(ui.vibration()){
   Vibrator vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
   if(vibrator!=null&&vibrator.hasVibrator())try{int level=ui.strength();vibrator.vibrate(VibrationEffect.createOneShot(new long[]{8,14,22}[level],new int[]{45,95,160}[level]));}catch(SecurityException ignored){}
  }
  AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
  if(ui.sound()&&ui.volume()>0&&clickLoaded&&clickPool!=null&&audio!=null&&audio.getRingerMode()==AudioManager.RINGER_MODE_NORMAL){float volume=ui.volume()/100f;clickPool.play(clickSample,volume,volume,1,0,1f);}
 }
 void appearanceChanged(){runOnUiThread(()->panel.appearanceChanged());}
 void tools(){importRom();}
 private void fullscreen(){
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.systemBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}
 }
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)fullscreen();}
 private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
 static String hex(byte[] data){StringBuilder s=new StringBuilder();for(byte b:data)s.append(String.format(java.util.Locale.ROOT,"%02x",b));return s.toString();}
 @Override protected void onResume(){super.onResume();running=true;fullscreen();}
 @Override protected void onPause(){running=false;panel.cancelTouch();worker.execute(()->{release();save();});super.onPause();}
 @Override protected void onDestroy(){if(clickPool!=null){clickPool.release();clickPool=null;}running=false;worker.execute(()->{release();save();});worker.shutdown();super.onDestroy();}
 @Override public void onBackPressed(){if(settings.active())worker.execute(()->{settings.back();panel.postInvalidate();});else finish();}
}
