package org.fx991cnx.emulator;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.util.AtomicFile;
import android.view.*;
import android.widget.Toast;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.zip.CRC32;

public class MainActivity extends Activity {
 private CalculatorView panel;
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
 private volatile boolean running=false,ready=false;
 private String digest="";
 private int held=-1;
 private long heldAt;
 private final java.util.ArrayDeque<int[]> keys=new java.util.ArrayDeque<>();
 private long frames=0;
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);panel=new CalculatorView(this);setContentView(panel);if(rom().isFile())worker.execute(this::open);worker.scheduleWithFixedDelay(this::tick,0,16,TimeUnit.MILLISECONDS);}
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
 private void fail(String message){ready=false;panel.ready=false;runOnUiThread(()->new AlertDialog.Builder(this).setTitle("ROM 未能运行").setMessage(message+"\n请确认是 fx-991CN X 的完整原机 ROM。本版本尚未完成专有固件验收。").setPositiveButton("重新导入",(d,w)->importRom()).setNegativeButton("关闭",null).show());}
 void importRom(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=1||result!=RESULT_OK||data==null)return;Uri uri=data.getData();worker.execute(()->{
  try(InputStream in=getContentResolver().openInputStream(uri)){
   if(in==null)throw new IOException("无法读取文件");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))!=-1){out.write(chunk,0,n);if(out.size()>262144)throw new IOException("仅支持 256 KiB 原机 ROM");}byte[] bytes=out.toByteArray();
   if(bytes.length!=262144)throw new IOException("ROM 必须是 262144 字节（256 KiB），不支持压缩包或 CW/ES 固件");
   int sp=(bytes[0]&255)|((bytes[1]&255)<<8),pc=(bytes[2]&255)|((bytes[3]&255)<<8);
   if(sp<0xD000||sp>0xF000||(sp&1)!=0||pc==0||pc==0xFFFF||(pc&1)!=0)throw new IOException("ROM 向量不符合 ClassWiz X 原机格式");
   String ascii=new String(bytes,java.nio.charset.StandardCharsets.ISO_8859_1);boolean identified=ascii.contains("991CN")||ascii.contains("991cn");
   runOnUiThread(()->new AlertDialog.Builder(this).setTitle(identified?"检测到 fx-991CN 系列标识":"检测到 ClassWiz X 格式候选 ROM")
    .setMessage((identified?"仍需确认这是 fx-991CN X。":"无法仅凭文件大小可靠识别具体机型。请确认这是 fx-991CN X 原机 ROM；其他 X 机型不会转换为中文版。")+"\n\n仅导入您有权使用的固件。文件只保存在本应用内部，不上传。")
    .setPositiveButton("确认并导入",(d,w)->worker.execute(()->install(bytes))).setNegativeButton("取消",null).show());
  }catch(Exception e){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("未导入").setMessage(e.getMessage()).setPositiveButton("知道了",null).show());}
 });}
 private void install(byte[] bytes){release();save();ready=false;try{AtomicFile f=new AtomicFile(rom());FileOutputStream out=f.startWrite();try{out.write(bytes);f.finishWrite(out);}catch(Exception e){f.failWrite(out);throw e;}open();}catch(Exception e){fail(e.getMessage());}}
 void tools(){new AlertDialog.Builder(this).setTitle("fx-991CN X Emulator").setItems(new String[]{"导入／更换合法 ROM","重新开机（清除当前状态）","许可与版本"},(d,w)->{if(w==0)importRom();else if(w==1)new AlertDialog.Builder(this).setMessage("清除计算器状态？导入的 ROM 会保留。").setPositiveButton("清除",(a,b)->worker.execute(()->{release();ready=false;new AtomicFile(stateFile()).delete();open();})).setNegativeButton("取消",null).show();else new AlertDialog.Builder(this).setTitle("0.1.0-preview · 固件验收待完成").setMessage("独立开源项目，非 Casio 官方应用。\n模拟核心：CasioEmuNeo（GPL-3.0），Android 移植及面板有修改。\n不内置、不下载专有 ROM；所有计算由导入固件执行。\n源码及许可证：github.com/Antssos/cloudcodex，feat/fx991cnx-android 分支。\n完整 GPL 见随 APK 发布的源代码及 LICENSE。").setPositiveButton("GPL-3.0 全文",(a,b)->license()).setNegativeButton("关闭",null).show();}).show();}
 private void license(){try(InputStream in=getAssets().open("LICENSE")){new AlertDialog.Builder(this).setTitle("GNU GPL v3").setMessage(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).setPositiveButton("关闭",null).show();}catch(IOException ignored){}}
 static String hex(byte[] data){StringBuilder s=new StringBuilder();for(byte b:data)s.append(String.format(java.util.Locale.ROOT,"%02x",b));return s.toString();}
 @Override protected void onResume(){super.onResume();running=true;}
 @Override protected void onPause(){running=false;panel.cancelTouch();worker.execute(()->{release();save();});super.onPause();}
 @Override protected void onDestroy(){running=false;worker.execute(()->{release();save();});worker.shutdown();super.onDestroy();}
 @Override public void onBackPressed(){finish();}
}
