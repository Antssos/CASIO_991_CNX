package org.fx991cnx.emulator;
import android.test.ActivityInstrumentationTestCase2;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import android.content.Intent;
import java.io.*;
import java.util.*;

/** Only original synthetic MCU firmware. Never asserts Casio math works. */
public class EmulatorTest extends ActivityInstrumentationTestCase2<MainActivity> {
 public EmulatorTest(){super(MainActivity.class);}
 private void shot(String name)throws Exception{
  Bitmap b=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);
  android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,name+".png");values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/fx991-screens");
  android.content.ContentResolver resolver=getInstrumentation().getTargetContext().getContentResolver();android.net.Uri uri=resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);assertNotNull(uri);
  try(OutputStream out=resolver.openOutputStream(uri)){assertNotNull(out);assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();
 }
 private void awaitPixel(CalculatorView v,int expected)throws Exception{long end=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<end&&(v.lcd[32*41]&255)!=expected){assertEquals("",NativeCore.error());SystemClock.sleep(30);}if((v.lcd[32*41]&255)!=expected)shot("failure-keyboard");assertEquals(expected,v.lcd[32*41]&255);}
 private static void word(byte[] b,int a,int v){b[a]=(byte)v;b[a+1]=(byte)(v>>8);}
 private static byte[] fixture(){
  byte[] b=new byte[262144];Arrays.fill(b,(byte)0xff);word(b,0,0xE000);word(b,2,0x100);
  ArrayList<Integer> words=new ArrayList<>();
  // Store immediate byte by actual MOV/ST instructions.
  java.util.function.BiConsumer<Integer,Integer> store=(a,v)->{words.add(v);words.add(0x9011);words.add(a);};
  store.accept(0xF031,5);store.accept(0xF032,12);store.accept(0xF046,127);
  // Font pattern TEST ROM; test glyphs are not a Casio calculator result.
  int[][] glyphs={{31,4,4,4,4,4,4},{31,16,16,30,16,16,31},{15,16,16,14,1,1,30},{31,4,4,4,4,4,4},{0,0,0,0,0,0,0},{30,17,17,30,20,18,17},{14,17,17,17,17,17,14},{17,27,21,21,17,17,17}};
  for(int y=0;y<7;y++){for(int col=0;col<6;col++){int value=0;for(int bit=0;bit<8;bit++){int x=col*8+bit;int g=x/6,offset=x%6;if(g<glyphs.length&&offset<5&&(glyphs[g][y]&(1<<(4-offset)))!=0)value|=128>>bit;}store.accept(0xF820+(y+15)*32+col,value);}}
  int loop=0x100+words.size()*2;words.add(0x9010);words.add(0xF040);words.add(0x9011);words.add(0xF820+40*32);words.add(0xCEFB);
  for(int i=0;i<words.size();i++)word(b,0x100+i*2,words.get(i));return b;
 }
 private CalculatorView view(MainActivity a){return (CalculatorView)((android.view.ViewGroup)a.findViewById(android.R.id.content)).getChildAt(0);}
 private void touch(MainActivity a,CalculatorView v,int index,boolean down){
  float width=v.getWidth()-v.getPaddingLeft()-v.getPaddingRight(),height=v.getHeight()-v.getPaddingTop()-v.getPaddingBottom();float s=Math.min(width/344f,height/720f),ox=v.getPaddingLeft()+(width-344*s)/2,oy=v.getPaddingTop()+(height-720*s)/2;
  android.graphics.RectF r=CalculatorView.RECTS[index];float x=ox+r.centerX()*s,y=oy+r.centerY()*s;
  getInstrumentation().runOnMainSync(()->{MotionEvent e=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),down?MotionEvent.ACTION_DOWN:MotionEvent.ACTION_UP,x,y,0);v.dispatchTouchEvent(e);e.recycle();});
 }
 @Override protected void setUp()throws Exception{super.setUp();File[] files=getInstrumentation().getTargetContext().getFilesDir().listFiles();if(files!=null)for(File f:files)if(f.getName().equals("rom.bin")||f.getName().startsWith("state-v1-"))f.delete();}
 private void shell(String command)throws Exception{try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(getInstrumentation().getUiAutomation().executeShellCommand(command))){byte[] b=new byte[2048];while(in.read(b)!=-1){}}}
 private void clickText(String text)throws Exception{long deadline=SystemClock.uptimeMillis()+10000;android.view.accessibility.AccessibilityNodeInfo root=null;while(SystemClock.uptimeMillis()<deadline){root=getInstrumentation().getUiAutomation().getRootInActiveWindow();if(root!=null){java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(text);if(!nodes.isEmpty()){assertTrue(nodes.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));return;}}SystemClock.sleep(200);}shot("failure-ui");fail("Missing UI text: "+text+" active window: "+root);}
 public void testOfflineNativeAndTouch()throws Exception{
  MainActivity a=getActivity();SystemClock.sleep(500);shot("01-no-rom-portrait");
  // Fixture is written only by the separately installed test APK.
  byte[] firmware=fixture();File file=new File(a.getFilesDir(),"test-only.rom");try(FileOutputStream out=new FileOutputStream(file)){out.write(firmware);}
  assertEquals("",NativeCore.open(file.getPath()));byte[] frame=NativeCore.frame(3000);assertEquals(5,frame[2048]);assertEquals(255,frame[32*41]&255);
  byte[] state=NativeCore.save();assertTrue(state.length>10000);
  for(int code:CalculatorView.CODES){NativeCore.key(code,true);NativeCore.frame(100);NativeCore.key(code,false);}
  assertTrue(NativeCore.restore(state));assertFalse(NativeCore.restore(new byte[3]));assertEquals("",NativeCore.error());
  // Exercise the real SAF picker and its import-result handler, including model confirmation.
  getInstrumentation().runOnMainSync(a::importRom);SystemClock.sleep(600);shot("01b-system-file-picker");
  shell("input keyevent 4");SystemClock.sleep(600);
  getInstrumentation().getTargetContext().startActivity(new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));SystemClock.sleep(400);
  getInstrumentation().runOnMainSync(()->a.onActivityResult(1,android.app.Activity.RESULT_OK,new Intent().setData(android.net.Uri.fromFile(file))));
  SystemClock.sleep(500);shot("01c-rom-model-confirmation");clickText("确认并导入");SystemClock.sleep(700);assertTrue(view(a).ready);
  // Restart through normal app startup, then drive rendered keys.

  getInstrumentation().runOnMainSync(a::finish);SystemClock.sleep(400);
  Intent intent=new Intent(getInstrumentation().getTargetContext(),MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  MainActivity live=(MainActivity)getInstrumentation().startActivitySync(intent);SystemClock.sleep(700);CalculatorView v=view(live);assertTrue(v.ready);shot("02-synthetic-rom-portrait");
  for(int i=0;i<CalculatorView.RECTS.length;i++){
   android.graphics.RectF r=CalculatorView.RECTS[i];assertEquals(i,v.keyAt(r.centerX(),r.centerY()));
   touch(live,v,i,true);SystemClock.sleep(100);touch(live,v,i,false);SystemClock.sleep(100);
  }
  // Verify a visible key press travels through Android touch, queue, KO/KI and MCU to VRAM.
  assertTrue(v.ready);touch(live,v,10,true);awaitPixel(v,254);shot("03-touch-key-1");touch(live,v,10,false);awaitPixel(v,255);
  live.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);SystemClock.sleep(700);shot("04-synthetic-rom-landscape");
  live.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);SystemClock.sleep(500);
  // Background / resume. Core and persisted app state must remain available offline.
  shell("input keyevent 3");SystemClock.sleep(400);getInstrumentation().getTargetContext().startActivity(new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));SystemClock.sleep(500);assertTrue(v.ready);touch(live,v,10,true);awaitPixel(v,254);touch(live,v,10,false);awaitPixel(v,255);
  getInstrumentation().runOnMainSync(live::finish);SystemClock.sleep(500);MainActivity reopened=(MainActivity)getInstrumentation().startActivitySync(intent);SystemClock.sleep(700);assertTrue(view(reopened).ready);shot("05-restored-state");
  assertEquals(5,view(reopened).lcd[2048]);assertEquals("",NativeCore.error());
  getInstrumentation().runOnMainSync(reopened::finish);SystemClock.sleep(300);
 }
}
