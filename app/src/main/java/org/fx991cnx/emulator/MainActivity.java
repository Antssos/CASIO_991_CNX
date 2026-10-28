package org.fx991cnx.emulator;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.util.AtomicFile;
import android.view.*;
import android.widget.Toast;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
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
 @Override public void onCreate(Bundle b){super.onCreate(b);ui=new UiPreferences(this);initFeedback();if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);panel=new CalculatorView(this);setContentView(panel);getWindow().setStatusBarColor(0xfff4f4f2);getWindow().setNavigationBarColor(0xfff4f4f2);getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility()|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);if(rom().isFile())worker.execute(this::open);worker.scheduleWithFixedDelay(this::tick,0,16,TimeUnit.MILLISECONDS);}
 private File rom(){return new File(getFilesDir(),"rom.bin");}
 private void tick(){if(!ready||!running)return;try{
  long now=SystemClock.uptimeMillis();
  synchronized(keys){if(!keys.isEmpty()){
   int[] event=keys.peek();
   if(event[1]==1 && held<0){keys.remove();held=event[0];heldAt=now;NativeCore.key(held,true);}
   else if(event[1]==0 && now-heldAt>=64){keys.remove();NativeCore.key(event[0],false);held=-1;}
  }}
  byte[] lcd=NativeCore.frame(32000);panel.lcd=lcd;panel.postInvalidate();
  if(++frames%300==0){save();String e=NativeCore.error();if(!e.isEmpty())fail(e);}
 }catch(Exception e){fail("运行失败："+e.getMessage());}}
 void key(int code,boolean down){if(!ready)return;synchronized(keys){keys.add(new int[]{code,down?1:0});}}
 private void release(){synchronized(keys){keys.clear();if(held>=0)NativeCore.key(held,false);held=-1;}}
 private void open(){try{release();ready=false;digest=hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(rom().toPath())));String e=NativeCore.open(rom().getPath());if(!e.isEmpty()){fail(e);return;}File state=stateFile();if(state.isFile()){
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
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
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
 private TextView section(String text){TextView title=new TextView(this);title.setText(text);title.setTextSize(13);title.setTextColor(0xff666866);title.setTypeface(null,Typeface.BOLD);title.setPadding(0,dp(18),0,dp(8));return title;}
 private TextView note(String text){TextView view=new TextView(this);view.setText(text);view.setTextSize(12);view.setTextColor(0xff757775);view.setPadding(0,dp(4),0,dp(4));return view;}
 private void themeButton(Button button,int index){
  boolean selected=ui.theme()==index;button.setText(UiPreferences.THEME_NAMES[index]+(selected?"  ✓":""));button.setTextSize(13);button.setTextColor(index==3?Color.WHITE:0xff343734);button.setAllCaps(false);button.setPadding(dp(4),0,dp(4),0);button.setSelected(selected);
  GradientDrawable background=new GradientDrawable();background.setColor(UiPreferences.THEME_SWATCHES[index]);background.setCornerRadius(dp(6));background.setStroke(dp(selected?2:1),selected?0xff4c6254:0x22909090);button.setBackground(background);button.setContentDescription(UiPreferences.THEME_NAMES[index]+(selected?"，已选中":"，点按切换主题"));
 }
 void showSettings(){
  if(isFinishing())return;
  LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),0,dp(22),dp(16));
  body.addView(section("主题"));Button[] themes=new Button[4];
  for(int row=0;row<2;row++){
   LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
   for(int col=0;col<2;col++){final int index=row*2+col;Button button=new Button(this);themes[index]=button;themeButton(button,index);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1);lp.setMargins(col==0?0:dp(5),0,col==0?dp(5):0,dp(10));line.addView(button,lp);button.setOnClickListener(v->{ui.theme(index);for(int i=0;i<4;i++)themeButton(themes[i],i);panel.appearanceChanged();});}
   body.addView(line);
  }
  body.addView(section("按键反馈"));
  Switch vibration=new Switch(this);vibration.setText("按键震动");vibration.setTextSize(15);vibration.setChecked(ui.vibration());body.addView(vibration,new LinearLayout.LayoutParams(-1,dp(48)));
  String[] strengthNames={"轻","标准","强"};TextView strengthLabel=note("震动强度："+strengthNames[ui.strength()]);body.addView(strengthLabel);
  SeekBar strength=new SeekBar(this);strength.setMax(2);strength.setProgress(ui.strength());strength.setEnabled(ui.vibration());body.addView(strength,new LinearLayout.LayoutParams(-1,dp(40)));
  vibration.setOnCheckedChangeListener((b,on)->{ui.vibration(on);strength.setEnabled(on);if(on)keyFeedback();});
  strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean fromUser){if(fromUser){ui.strength(value);strengthLabel.setText("震动强度："+strengthNames[value]);}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){if(ui.vibration())keyFeedback();}});
  Switch sound=new Switch(this);sound.setText("按键音");sound.setTextSize(15);sound.setChecked(ui.sound());body.addView(sound,new LinearLayout.LayoutParams(-1,dp(48)));
  TextView volumeLabel=note("按键音量："+ui.volume()+"%");body.addView(volumeLabel);SeekBar volume=new SeekBar(this);volume.setMax(100);volume.setProgress(ui.volume());volume.setEnabled(ui.sound());body.addView(volume,new LinearLayout.LayoutParams(-1,dp(40)));
  sound.setOnCheckedChangeListener((b,on)->{ui.sound(on);volume.setEnabled(on);if(on)keyFeedback();});
  volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean fromUser){if(fromUser){ui.volume(value);volumeLabel.setText("按键音量："+value+"%");}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){keyFeedback();}});
  body.addView(note("静音或振动模式下不播放按键音。设置自动保存。"));
  body.addView(section("计算器"));
  Button choose=new Button(this);choose.setText("导入 / 更换 ROM");choose.setAllCaps(false);body.addView(choose,new LinearLayout.LayoutParams(-1,dp(48)));
  Button reset=new Button(this);reset.setText("清除计算器状态");reset.setAllCaps(false);body.addView(reset,new LinearLayout.LayoutParams(-1,dp(48)));
  Button about=new Button(this);about.setText("关于与许可证");about.setAllCaps(false);body.addView(about,new LinearLayout.LayoutParams(-1,dp(48)));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(body);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("设置").setView(scroll).setPositiveButton("完成",null).create();
  choose.setOnClickListener(v->{dialog.dismiss();importRom();});
  reset.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("清除计算器状态？已导入的 ROM、主题和反馈设置会保留。").setPositiveButton("清除",(a,b)->worker.execute(()->{release();ready=false;new AtomicFile(stateFile()).delete();if(rom().isFile())open();})).setNegativeButton("取消",null).show());
  about.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("fx-991CN X Emulator · 0.3.0").setMessage("四套真机配色主题，原生离线界面。\n独立开源项目，非 Casio 官方应用。\n模拟核心：CasioEmuNeo（GPL-3.0）。\n不内置或下载专有 ROM，所有计算由导入固件执行。\nSVG 渲染：AndroidSVG（Apache-2.0）。\n源码：github.com/Antssos/cloudcodex，feat/fx991cnx-android 分支。").setPositiveButton("GPL-3.0 全文",(a,b)->license()).setNegativeButton("关闭",null).show());
  dialog.show();if(dialog.getWindow()!=null){int max=Math.round(getResources().getDisplayMetrics().heightPixels*.88f);scroll.setLayoutParams(new android.widget.FrameLayout.LayoutParams(-1,Math.min(dp(610),max-dp(120))));}
 }
 void tools(){showSettings();}
 private void license(){try(InputStream in=getAssets().open("LICENSE")){new AlertDialog.Builder(this).setTitle("GNU GPL v3").setMessage(new String(readAll(in),java.nio.charset.StandardCharsets.UTF_8)).setPositiveButton("关闭",null).show();}catch(IOException ignored){}}
 private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
 static String hex(byte[] data){StringBuilder s=new StringBuilder();for(byte b:data)s.append(String.format(java.util.Locale.ROOT,"%02x",b));return s.toString();}
 @Override protected void onResume(){super.onResume();running=true;}
 @Override protected void onPause(){running=false;panel.cancelTouch();worker.execute(()->{release();save();});super.onPause();}
 @Override protected void onDestroy(){if(clickPool!=null){clickPool.release();clickPool=null;}running=false;worker.execute(()->{release();save();});worker.shutdown();super.onDestroy();}
 @Override public void onBackPressed(){finish();}
}
