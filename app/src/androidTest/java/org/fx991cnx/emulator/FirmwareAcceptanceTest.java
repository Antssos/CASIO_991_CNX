package org.fx991cnx.emulator;

import android.test.ActivityInstrumentationTestCase2;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import org.json.*;

/** Optional acceptance against a separately supplied ROM. No firmware in this APK. */
public class FirmwareAcceptanceTest extends ActivityInstrumentationTestCase2<MainActivity> {
 private MainActivity current;
 private CalculatorView view;
 private JSONObject goldens;
 private static final String ROM_SHA="8b6b6430f96858a8b3e765796e2d6f340bc7073ac9b0aa464775f15e53a10788";
 public FirmwareAcceptanceTest(){super(MainActivity.class);}
 private void screenshot(String name)throws Exception{
  Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
  ContentValues v=new ContentValues();v.put(MediaStore.Images.Media.DISPLAY_NAME,name+".png");v.put(MediaStore.Images.Media.MIME_TYPE,"image/png");v.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/fx991-screens");
  ContentResolver r=getInstrumentation().getTargetContext().getContentResolver();Uri uri=r.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);assertNotNull(uri);
  try(OutputStream out=r.openOutputStream(uri)){assertNotNull(out);assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
 }
 private void attach(MainActivity activity)throws Exception{
  current=activity;view=(CalculatorView)((android.view.ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
  long end=SystemClock.uptimeMillis()+15000;
  while(SystemClock.uptimeMillis()<end&&(!view.ready||view.lcd[2048]!=5)){assertEquals("",NativeCore.error());SystemClock.sleep(50);}
  assertTrue("ROM did not boot",view.ready);assertEquals(5,view.lcd[2048]);SystemClock.sleep(500);
 }
 private void touch(int index,boolean down){
  float width=view.getWidth()-view.getPaddingLeft()-view.getPaddingRight(),height=view.getHeight()-view.getPaddingTop()-view.getPaddingBottom()-36*view.getResources().getDisplayMetrics().density;
  float scale=Math.min(width/SkinGeometry.WIDTH,height/SkinGeometry.HEIGHT),ox=view.getPaddingLeft()+(width-SkinGeometry.WIDTH*scale)/2,oy=view.getPaddingTop()+36*view.getResources().getDisplayMetrics().density+(height-SkinGeometry.HEIGHT*scale)/2;
  RectF rect=CalculatorView.RECTS[index];float x=ox+rect.centerX()*scale,y=oy+rect.centerY()*scale;
  getInstrumentation().runOnMainSync(()->{MotionEvent e=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),down?MotionEvent.ACTION_DOWN:MotionEvent.ACTION_UP,x,y,0);view.dispatchTouchEvent(e);e.recycle();});
 }
 private void keys(int... codes){for(int code:codes){int index=-1;for(int i=0;i<CalculatorView.CODES.length;i++)if(CalculatorView.CODES[i]==code)index=i;assertTrue(index>=0);touch(index,true);SystemClock.sleep(120);touch(index,false);SystemClock.sleep(120);}}
 private String regionHash(byte[] lcd,JSONArray region)throws Exception{
  int x=region.getInt(0),y=region.getInt(1),width=region.getInt(2),height=region.getInt(3);MessageDigest sha=MessageDigest.getInstance("SHA-256");
  for(int row=y;row<y+height;row++)for(int col=x;col<x+width;col++)sha.update((byte)(((lcd[(row+1)*32+(col>>3)]&(128>>(col&7)))!=0)?1:0));
  return MainActivity.hex(sha.digest());
 }
 private void expect(String name)throws Exception{
  JSONObject expected=goldens.getJSONObject(name);JSONArray region=expected.getJSONArray("region");String sha=expected.getString("sha256");String actual="";long end=SystemClock.uptimeMillis()+15000;
  while(SystemClock.uptimeMillis()<end){assertEquals("",NativeCore.error());actual=regionHash(view.lcd,region);if(sha.equals(actual)){System.out.println("ROM ACCEPTANCE PASS: "+name);return;}SystemClock.sleep(60);}
  screenshot("failure-real-"+name);assertEquals(name,sha,actual);
 }
 private void clickText(String text)throws Exception{
  long end=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<end){android.view.accessibility.AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();if(root!=null){java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(text);if(!nodes.isEmpty()){assertTrue(nodes.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));return;}}SystemClock.sleep(100);}fail("Missing text: "+text);
 }
 private void reopen()throws Exception{
  MainActivity previous=current;getInstrumentation().runOnMainSync(previous::finish);SystemClock.sleep(700);
  Intent intent=new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  attach((MainActivity)getInstrumentation().startActivitySync(intent));
 }
 public void testRealFirmwareAcceptance()throws Exception{
  File rom=new File(getInstrumentation().getTargetContext().getFilesDir(),"rom.bin");assertTrue("Supply a ROM separately; none is bundled",rom.isFile());
  assertEquals(ROM_SHA,MainActivity.hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(rom.toPath()))));
  try(InputStream in=getInstrumentation().getContext().getAssets().open("rom-acceptance-goldens.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1)out.write(b,0,n);goldens=new JSONObject(out.toString("UTF-8"));}
  attach(getActivity());screenshot("10-real-boot");
  // The app import handler must identify the exact known ROM without a filename guess.
  MainActivity initial=current;getInstrumentation().runOnMainSync(()->initial.onActivityResult(1,android.app.Activity.RESULT_OK,new Intent().setData(Uri.fromFile(rom))));
  SystemClock.sleep(500);android.view.accessibility.AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();assertNotNull(root);assertFalse(root.findAccessibilityNodeInfosByText("已识别 fx-991CN X").isEmpty());screenshot("11-real-rom-identified");clickText("确认并导入");SystemClock.sleep(1000);attach(current);
  keys(0x42,0x00,0x64,0x31,0x64,0x63,0x10,0x11,0x41,0x23,0x00,0x64,0x30,0x10,0x63,0x11,0x33,0x60);
  expect("result");screenshot("12-real-one-fifth");
  reopen();expect("result");screenshot("13-real-result-restored");
  keys(0x43);expect("decimal");screenshot("14-real-decimal");
  keys(0x07,0x47);expect("setup-menu");screenshot("15-real-chinese-setup");
  keys(0x42,0x42,0x17,0x04);expect("alpha-variable");screenshot("16-real-alpha-A");keys(0x32);expect("delete");
  keys(0x42,0x15,0x10,0x37,0x30,0x20,0x25,0x60);expect("roots-result");screenshot("17-real-root-and-power");
  keys(0x47,0x01,0x00,0x10,0x10,0x00,0x60,0x10,0x60,0x20,0x60,0x01,0x60,0x42,0x06,0x20,0x31,0x10,0x60);
  expect("matrix-result");screenshot("18-real-matrix-times-two");
  keys(0x47,0x12,0x10,0x10,0x00,0x60,0x04,0x11,0x60,0x21,0x60,0x60);
  expect("equation-root2");screenshot("19-real-equation-root-three");keys(0x60);expect("equation-second-root");screenshot("20-real-equation-root-two");
  keys(0x47,0x00,0x42,0x11,0x03,0x04,0x42,0x17,0x04,0x60);expect("variable-five");
  reopen();expect("variable-five");keys(0x42,0x17,0x04,0x60);expect("variable-five");screenshot("21-real-variable-restored");
  System.out.println("REAL ROM ACCEPTANCE COMPLETE: 1/5, 0.2, Chinese SETUP, ALPHA, DEL, root/power, matrix, polynomial, persistent result and A=5");
  MainActivity last=current;getInstrumentation().runOnMainSync(last::finish);SystemClock.sleep(500);
 }
}
