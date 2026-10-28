package org.fx991cnx.emulator;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import com.caverock.androidsvg.SVG;

/** Native bitmap-cached SVG faceplate; live display is always read from MCU VRAM. */
final class CalculatorView extends View {
 final MainActivity activity;
 volatile byte[] lcd=new byte[2050];
 volatile boolean ready=false;
 static final int[] CODES={0x02,0x12,0x22,0x32,0x42,0x01,0x11,0x21,0x31,0x41,0x00,0x10,0x20,0x30,0x40,0x64,0x63,0x62,0x61,0x60,0x05,0x15,0x25,0x35,0x45,0x55,0x04,0x14,0x24,0x34,0x44,0x54,0x03,0x13,0x23,0x33,0x43,0x53,0x06,0x16,0x46,0x56,0x07,0x17,0x47,0xFF,0x26,0x37,0x27,0x36};
 static final String[] MAIN={"7","8","9","DEL","AC","4","5","6","×","÷","1","2","3","+","−","0",".","×10ˣ","Ans","=","分数","平方根","x²","xʸ","log","ln","负号","度分秒","x⁻¹","sin","cos","tan","STO","ENG","(",")","S⇔D","M+","OPTN","CALC","积分","x","SHIFT","ALPHA","菜单","ON","左","右","上","下"};
 static final RectF[] RECTS=SkinGeometry.KEYS;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF settingsRect=new RectF();
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable showSettings=this::openSettings;
 private Bitmap skin;
 private int renderedTheme=-1,renderedResolution=0,active=-1;
 private boolean settingsPressed=false,headerPressed=false,skinFailed=false;
 private float scale=1,ox,oy;
 CalculatorView(Context context){super(context);activity=(MainActivity)context;setLayerType(View.LAYER_TYPE_SOFTWARE,null);setFocusable(true);setContentDescription("fx-991CN X，右上角设置可更换主题、震动与按键音；长按机身顶部也可打开设置");}
 private float dp(float value){return value*getResources().getDisplayMetrics().density;}
 private void openSettings(){cancelTouch();activity.showSettings();}
 void appearanceChanged(){renderedTheme=-1;invalidate();}
 @Override public WindowInsets onApplyWindowInsets(WindowInsets insets){if(Build.VERSION.SDK_INT>=30){Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());setPadding(bars.left,bars.top,bars.right,bars.bottom);}return insets;}
 private void skin(int theme,int resolution){
  if(skin!=null&&renderedTheme==theme&&renderedResolution==resolution)return;
  try{
   SVG svg=SVG.getFromAsset(getContext().getAssets(),"skins/"+UiPreferences.THEME_FILES[theme]+".svg");
   Bitmap next=Bitmap.createBitmap((int)SkinGeometry.WIDTH*resolution,(int)SkinGeometry.HEIGHT*resolution,Bitmap.Config.ARGB_8888);
   Canvas canvas=new Canvas(next);canvas.scale(resolution,resolution);svg.renderToCanvas(canvas,new RectF(0,0,SkinGeometry.WIDTH,SkinGeometry.HEIGHT));
   Bitmap old=skin;skin=next;renderedTheme=theme;renderedResolution=resolution;skinFailed=false;if(old!=null)old.recycle();
  }catch(Exception ex){skinFailed=true;renderedTheme=theme;}
 }
 private void text(Canvas canvas,String value,float x,float y,float size,int color,boolean bold){paint.setShader(null);paint.setColor(color);paint.setTextSize(size);paint.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);canvas.drawText(value,x,y,paint);}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);canvas.drawColor(0xfff4f4f2);
  float toolbar=dp(36),availableWidth=getWidth()-getPaddingLeft()-getPaddingRight(),availableHeight=Math.max(1,getHeight()-getPaddingTop()-getPaddingBottom()-toolbar);
  scale=Math.min(availableWidth/SkinGeometry.WIDTH,availableHeight/SkinGeometry.HEIGHT);
  ox=getPaddingLeft()+(availableWidth-SkinGeometry.WIDTH*scale)/2;
  oy=getPaddingTop()+toolbar+(availableHeight-SkinGeometry.HEIGHT*scale)/2;
  settingsRect.set(getWidth()-getPaddingRight()-dp(88),getPaddingTop(),getWidth()-getPaddingRight()-dp(8),getPaddingTop()+toolbar);
  drawSettings(canvas);
  canvas.save();canvas.translate(ox,oy);canvas.scale(scale,scale);
  skin(activity.ui.theme(),Math.max(1,Math.min(4,(int)Math.ceil(scale))));
  if(skin!=null&&!skinFailed)canvas.drawBitmap(skin,null,new RectF(0,0,SkinGeometry.WIDTH,SkinGeometry.HEIGHT),paint);
  else{text(canvas,"面板载入失败",172,360,14,0xff555555,false);canvas.restore();return;}
  drawLCD(canvas);
  if(active>=0){
   paint.setColor(0x35000000);paint.setStyle(Paint.Style.FILL);
   if(active>=46){float[] coordinates=SkinGeometry.DPAD[active-46];Path path=new Path();path.moveTo(coordinates[0],coordinates[1]);for(int i=2;i<coordinates.length;i+=2)path.lineTo(coordinates[i],coordinates[i+1]);path.close();canvas.drawPath(path,paint);}
   else if(active>=42)canvas.drawOval(RECTS[active],paint);
   else canvas.drawRoundRect(RECTS[active],4,4,paint);
  }
  canvas.restore();
 }
 private void drawSettings(Canvas canvas){
  float cy=settingsRect.centerY(),cx=settingsRect.left+dp(18);paint.setColor(settingsPressed?0xff111111:0xff737574);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.5f));
  canvas.save();canvas.translate(cx,cy);for(int i=0;i<8;i++){canvas.save();canvas.rotate(i*45);canvas.drawLine(0,-dp(10),0,-dp(7),paint);canvas.restore();}canvas.drawCircle(0,0,dp(6.5f),paint);canvas.drawCircle(0,0,dp(2.3f),paint);canvas.restore();paint.setStyle(Paint.Style.FILL);
  text(canvas,"设置",settingsRect.right-dp(23),cy+dp(4.3f),dp(12),0xff626563,false);
 }
 private void drawLCD(Canvas canvas){
  RectF area=SkinGeometry.LCD;
  if(!ready){text(canvas,"导入 ROM",area.centerX(),166,12,0xff304336,true);text(canvas,"点击选择 fx-991CN X 固件",area.centerX(),185,7.8f,0xff526354,false);return;}
  byte[] frame=lcd;int mode=frame[2048]&7,contrast=frame[2049]&63;
  if(mode!=4&&mode!=5&&mode!=6)return;
  final String[] names={"S","A","M","STO","Math","D","R","G","FIX","SCI","E","i","∠","▼","◀","▼","▲","▶","Ⅱ","☀"};
  final int[] offsets={0,1,2,3,5,6,7,8,9,10,11,12,13,15,16,17,18,19,21,22};
  final int[] left={0,8,16,26,39,56,64,71,80,94,109,118,124,131,142,149,156,162,171,183};
  final int[] widths={8,8,10,13,17,8,7,9,14,15,9,6,7,11,7,7,6,9,12,9};
  float dot=area.width()/192f;int on=mode==6?80:Math.min(255,20+contrast*16);
  if(mode==5||mode==6)for(int i=0;i<names.length;i++)if((frame[offsets[i]]&1)!=0)text(canvas,names[i],area.left+(left[i]+widths[i]/2f)*dot,area.top+7*dot,5.7f*dot,Color.argb(on,27,43,31),false);
  paint.setColor(Color.argb(on,27,43,31));paint.setAntiAlias(false);
  if(mode==5)for(int y=0;y<63;y++)for(int x=0;x<192;x++)if((frame[(y+1)*32+(x>>3)]&(128>>(x&7)))!=0){float px=area.left+x*dot,py=area.top+(y+10)*dot;canvas.drawRect(px,py,px+.96f*dot,py+.96f*dot,paint);}
  paint.setAntiAlias(true);
 }
 int keyAt(float x,float y){
  for(int i=46;i<50;i++)if(SkinGeometry.inside(SkinGeometry.DPAD[i-46],x,y))return i;
  for(int i=0;i<46;i++){RectF b=RECTS[i];if(i>=42){float dx=x-b.centerX(),dy=y-b.centerY();if(dx*dx+dy*dy<=(b.width()/2+2)*(b.width()/2+2))return i;}else if(x>=b.left-2&&x<b.right+2&&y>=b.top-2&&y<b.bottom+2)return i;}
  return -1;
 }
 void cancelTouch(){handler.removeCallbacks(showSettings);if(active>=0)activity.key(CODES[active],false);active=-1;settingsPressed=false;headerPressed=false;invalidate();}
 @Override public boolean onTouchEvent(MotionEvent event){
  float px=event.getX(),py=event.getY(),x=(px-ox)/scale,y=(py-oy)/scale;
  switch(event.getActionMasked()){
   case MotionEvent.ACTION_DOWN:
    if(settingsRect.contains(px,py)){settingsPressed=true;invalidate();return true;}
    if(x>=10&&x<=334&&y>=10&&y<100){headerPressed=true;handler.postDelayed(showSettings,650);return true;}
    if(!ready&&SkinGeometry.LCD.contains(x,y)){activity.importRom();return true;}
    active=keyAt(x,y);if(active>=0){activity.key(CODES[active],true);activity.keyFeedback();announceForAccessibility(MAIN[active]);invalidate();}return true;
   case MotionEvent.ACTION_POINTER_DOWN:cancelTouch();return true;
   case MotionEvent.ACTION_MOVE:
    if(settingsPressed&&!settingsRect.contains(px,py))cancelTouch();
    if(headerPressed&&(y>=100||x<10||x>334))cancelTouch();
    if(active>=0&&keyAt(x,y)!=active)cancelTouch();return true;
   case MotionEvent.ACTION_UP:
    boolean open=settingsPressed&&settingsRect.contains(px,py);performClick();cancelTouch();if(open)activity.showSettings();return true;
   case MotionEvent.ACTION_CANCEL:cancelTouch();return true;
  }
  return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
 @Override public boolean onKeyDown(int code,KeyEvent event){
  int index=-1;if(code>=KeyEvent.KEYCODE_0&&code<=KeyEvent.KEYCODE_9){String n=""+(code-KeyEvent.KEYCODE_0);for(int i=0;i<20;i++)if(MAIN[i].equals(n))index=i;}
  else if(code==KeyEvent.KEYCODE_ENTER)index=19;else if(code==KeyEvent.KEYCODE_DEL)index=3;else if(code==KeyEvent.KEYCODE_DPAD_LEFT)index=46;else if(code==KeyEvent.KEYCODE_DPAD_RIGHT)index=47;else if(code==KeyEvent.KEYCODE_DPAD_UP)index=48;else if(code==KeyEvent.KEYCODE_DPAD_DOWN)index=49;
  if(index>=0){if(event.getRepeatCount()==0){activity.key(CODES[index],true);activity.key(CODES[index],false);activity.keyFeedback();}return true;}return super.onKeyDown(code,event);
 }
 @Override protected void onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);if(skin!=null){skin.recycle();skin=null;}super.onDetachedFromWindow();}
}
