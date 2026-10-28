package org.fx991cnx.emulator;

import android.graphics.*;
import android.view.*;
import android.os.*;
import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;

final class CalculatorView extends View {
 final MainActivity activity;
 volatile byte[] lcd=new byte[2050];
 volatile boolean ready=false;
 private final Paint p=new Paint(3);
 private float scale=1,ox,oy;
 private int active=-1,pointer=-1;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable showTools=this::showToolsNow;
 private void showToolsNow(){cancelTouch();activity.tools();}
 static final int[] CODES={0x02,0x12,0x22,0x32,0x42,0x01,0x11,0x21,0x31,0x41,0x00,0x10,0x20,0x30,0x40,0x64,0x63,0x62,0x61,0x60,0x05,0x15,0x25,0x35,0x45,0x55,0x04,0x14,0x24,0x34,0x44,0x54,0x03,0x13,0x23,0x33,0x43,0x53,0x06,0x16,0x46,0x56,0x07,0x17,0x47,0xFF,0x26,0x37,0x27,0x36};
 static final String[] MAIN={"7","8","9","DEL","AC","4","5","6","×","÷","1","2","3","+","−","0",".","×10ˣ","Ans","=","▯/▯","√","x²","x▯","log","ln","(−)","° ′ ″","x⁻¹","sin","cos","tan","STO","ENG","(",")","S⇔D","M+","OPTN","CALC","∫","x","SHIFT","ALPHA","MENU","ON","◀","▶","▲","▼"};
 static final String[] SHIFT={"CONST","CONV","RESET","INS","OFF","","","","","","","","","","","Rnd","Ran#","π","PreAns","","a b/c","³√","x³","ⁿ√","10ˣ","eˣ","Abs","FACT","x!","sin⁻¹","cos⁻¹","tan⁻¹","RCL","←","%",",","a b/c","M−","QR","SOLVE","d/dx","Σ","","","SETUP","","","","",""};
 static final String[] ALPHA={"","","","","","","","","","","","","","","","","RanInt","e","","","","","","","","","A","B","C","D","E","F","","i","","","","M","","=",":","","","","","","","","",""};
 static final RectF[] RECTS=rectangles();
 private static RectF[] rectangles(){ArrayList<RectF> a=new ArrayList<>();for(int y:new int[]{498,549,600,650})for(int x:new int[]{22,84,145,207,269})a.add(new RectF(x,y,x+53,y+36));for(int y:new int[]{370,412,455})for(int x:new int[]{23,74,126,177,228,279})a.add(new RectF(x,y,x+43,y+26));for(int x:new int[]{23,74,228,279})a.add(new RectF(x,330,x+43,356));for(int x:new int[]{27,73,239,284})a.add(new RectF(x,269,x+30,299));a.add(new RectF(117,291,147,318));a.add(new RectF(195,291,225,318));a.add(new RectF(149,269,191,294));a.add(new RectF(149,319,191,342));return a.toArray(new RectF[0]);}
 CalculatorView(Context c){super(c);activity=(MainActivity)c;setLayerType(View.LAYER_TYPE_SOFTWARE,null);setFocusable(true);setContentDescription("fx-991CN X，长按顶部型号打开 ROM 与设置");}
 @Override public WindowInsets onApplyWindowInsets(WindowInsets insets){if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());setPadding(bars.left,bars.top,bars.right,bars.bottom);}return insets;}
 private void fill(Canvas c,int color,float x,float y,float w,float h,float r){p.setShader(null);p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
 private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setShader(null);p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(size);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(0xff131820);scale=Math.min((getWidth()-getPaddingLeft()-getPaddingRight())/344f,(getHeight()-getPaddingTop()-getPaddingBottom())/720f);ox=getPaddingLeft()+(getWidth()-getPaddingLeft()-getPaddingRight()-344*scale)/2;oy=getPaddingTop()+(getHeight()-getPaddingTop()-getPaddingBottom()-720*scale)/2;c.save();c.translate(ox,oy);c.scale(scale,scale);
  fill(c,0xff07090c,6,5,332,708,32);p.setShader(new LinearGradient(0,0,344,720,0xff454953,0xff20242c,Shader.TileMode.CLAMP));c.drawRoundRect(10,6,334,710,30,30,p);p.setShader(null);
  fill(c,0xffbec2c8,17,14,310,220,22);text(c,"CASIO",65,44,22,0xff151923,true);text(c,"fx-991CN X",251,42,15,0xff151923,true);text(c,"CLASSWIZ",84,75,17,0xff343b48,true);text(c,"中文科学计算器",245,82,10,0xff333b43,false);
  fill(c,0xff222018,170,52,142,18,2);for(int i=1;i<5;i++){p.setColor(0xff65584a);c.drawLine(170+i*28,53,170+i*28,69,p);}
  fill(c,0xff262e32,26,97,292,123,10);fill(c,0xffc3d4b6,33,104,278,109,5);drawLCD(c);
  text(c,"TWO WAY POWER",173,248,9,0xffaeb4bc,false);
  fill(c,0xff12151b,112,264,117,82,40);fill(c,0xff777e89,117,267,108,76,37);fill(c,0xff4e5662,147,287,46,33,10);text(c,"REPLAY",170,308,7,0xffdce0e5,false);
  for(int i=0;i<RECTS.length;i++){RectF b=RECTS[i];if(i<46){int color=i==42?0xffb89d60:i==43?0xffa98191:i==3||i==4?0xffa2a9b3:i<20?0xffd4d7dc:0xff252b34;fill(c,0xff090c11,b.left,b.top+3,b.width(),b.height(),i>=42?15:5);fill(c,active==i?0xff687482:color,b.left,b.top,b.width(),b.height(),i>=42?15:5);}
   int ink=i<20||i==42||i==43?0xff141820:0xfff2f3f5;
   text(c,MAIN[i],b.centerX(),b.centerY()+(i<20?7:4),i<20?(i==17?15:22):i>=42?8:13,ink,i<20);
   if(i<42||i==44){text(c,SHIFT[i],b.centerX(),b.top-4,i<20?8:8,0xffd8b66d,false);if(!ALPHA[i].isEmpty())text(c,ALPHA[i],b.right-1,b.top-4,8,0xffc98c9e,false);}
   if(active==i){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(0xffe3c88b);c.drawRoundRect(b,5,5,p);p.setStyle(Paint.Style.FILL);}
  }
  text(c,"fx-991CN X Emulator",172,704,8,0xff8e97a3,false);c.restore();
 }
 private void drawLCD(Canvas c){if(!ready){text(c,"导入 fx-991CN X ROM",172,143,16,0xff34442f,true);text(c,"点此选择一次 · 以后离线使用",172,169,11,0xff4d5c43,false);text(c,"不内置专有固件 · 预览版",172,193,10,0xff59654f,false);return;}
  byte[] frame=lcd;int mode=frame[2048]&7,contrast=frame[2049]&63;if(mode!=5&&mode!=6&&mode!=4)return;
  String[] names={"S","A","M","STO","","Math","D","R","G","FIX","SCI","E","i","∠","","▼","◀","▼","▲","▶","","Ⅱ","☀"};int x=0;
  for(int i=0;i<names.length;i++){if(i<frame.length&&(frame[i]&1)!=0&&(mode==5||mode==6))text(c,names[i],42+x,118,6.4f,0xff263b22,false);x+=11;}
  float dot=266f/192f;int alpha=mode==6?60:Math.min(255,20+contrast*16);p.setColor(Color.argb(alpha,28,44,25));p.setAntiAlias(false);
  if(mode==5)for(int y=0;y<63;y++)for(int col=0;col<192;col++)if((frame[(y+1)*32+(col>>3)]&(0x80>>(col&7)))!=0){float px=39+col*dot,py=122+y*dot;c.drawRect(px,py,px+dot*.9f,py+dot*.9f,p);}p.setAntiAlias(true);
 }
 int keyAt(float x,float y){for(int i=0;i<RECTS.length;i++)if(RECTS[i].contains(x,y))return i;return -1;}
 void cancelTouch(){handler.removeCallbacks(showTools);if(active>=0)activity.key(CODES[active],false);active=-1;pointer=-1;invalidate();}
 @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;switch(e.getActionMasked()){
 case MotionEvent.ACTION_DOWN:pointer=e.getPointerId(0);if(y<95){handler.postDelayed(showTools,650);return true;}if(!ready&&y>=97&&y<220){activity.importRom();return true;}active=keyAt(x,y);if(active>=0){activity.key(CODES[active],true);performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);announceForAccessibility(MAIN[active]);invalidate();}return true;
 case MotionEvent.ACTION_POINTER_DOWN:cancelTouch();return true;
 case MotionEvent.ACTION_MOVE:if(active>=0&&!RECTS[active].contains(x,y))cancelTouch();if(y>=95)handler.removeCallbacks(showTools);return true;
 case MotionEvent.ACTION_UP:performClick();cancelTouch();return true;
 case MotionEvent.ACTION_CANCEL:cancelTouch();return true;
 }return true;}
 @Override public boolean performClick(){super.performClick();return true;}
 @Override public boolean onKeyDown(int keyCode,KeyEvent e){int i=-1;if(keyCode>=KeyEvent.KEYCODE_0&&keyCode<=KeyEvent.KEYCODE_9){String n=""+(keyCode-KeyEvent.KEYCODE_0);for(int k=0;k<20;k++)if(MAIN[k].equals(n))i=k;}else if(keyCode==KeyEvent.KEYCODE_ENTER)i=19;else if(keyCode==KeyEvent.KEYCODE_DEL)i=3;else if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT)i=46;else if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT)i=47;else if(keyCode==KeyEvent.KEYCODE_DPAD_UP)i=48;else if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN)i=49;if(i>=0){if(e.getRepeatCount()==0){activity.key(CODES[i],true);activity.key(CODES[i],false);}return true;}return super.onKeyDown(keyCode,e);}
}
