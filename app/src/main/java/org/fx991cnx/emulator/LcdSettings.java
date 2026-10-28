package org.fx991cnx.emulator;


/** UI extension: only consumes 3/4 on the actual native SETUP language/contrast page.
 * All mutation and key routing run on the MCU worker, in physical event order.
 */
final class LcdSettings {
 private final MainActivity host;
 private String page="";
 private volatile byte[] display;
 LcdSettings(MainActivity host){this.host=host;}
 boolean active(){return display!=null;}
 byte[] compose(byte[] nativeFrame){
  byte[] menu=display;if(menu!=null)return menu;
  if(!NativeSetupPage.matches(nativeFrame))return nativeFrame;
  byte[] extended=nativeFrame.clone();draw(extended,"3:主题",32);draw(extended,"4:按键",48);return extended;
 }
 // Called before a queued key-down reaches the MCU. Its matching key-up follows
 // the same route in MainActivity; no UI-thread interception or stale LCD guess.
 boolean keyDown(int code,byte[] nativeFrame){
  if(page.isEmpty()){
   if(!NativeSetupPage.matches(nativeFrame))return false;
   if(code==0x20){go("theme",nativeFrame);return true;}
   if(code==0x01){go("keys",nativeFrame);return true;}
   return false;
  }
  if(code==0x42||code==0xff){close();return false;}
  if(code==0x32||code==0x47){back();return true;}
  int choice=code==0x00?0:code==0x10?1:code==0x20?2:code==0x01?3:-1;
  if(choice<0)return true;
  switch(page){
   case "theme":host.ui.theme(choice);host.appearanceChanged();close();break;
   case "keys":if(choice==0)go("strength",nativeFrame);else if(choice==1)go("volume",nativeFrame);break;
   case "strength":host.ui.vibrationLevel(choice);host.keyFeedback();go("keys",nativeFrame);break;
   case "volume":host.ui.soundLevel(choice);host.keyFeedback();go("keys",nativeFrame);break;
  }
  return true;
 }
 private byte[] base;
 private void go(String name,byte[] nativeFrame){
  if(page.isEmpty())base=nativeFrame.clone();page=name;
  byte[] next=new byte[2050];System.arraycopy(base,0,next,0,32);next[2048]=5;next[2049]=base[2049];
  String[] labels=page.equals("theme")?new String[]{"雾霾蓝","珍珠白","樱花粉","经典黑"}:page.equals("keys")?new String[]{"震动强度","按键音量"}:new String[]{"关","轻","中","重"};
  for(int row=0;row<labels.length;row++)draw(next,(row+1)+":"+labels[row],row*16);
  display=next;
 }
 void back(){if(page.equals("strength")||page.equals("volume"))go("keys",base);else close();}
 void close(){page="";display=null;base=null;}
 private void draw(byte[] frame,String text,int y){
  int x=0;
  for(int i=0;i<text.length();i++){
   int[] glyph=LcdGlyphs.get(text.charAt(i));
   if(glyph!=null)for(int gy=0;gy<12&&y+gy<63;gy++){
    int bits=glyph[gy];for(int gx=0;gx<12&&x+gx<192;gx++)if((bits&(1<<gx))!=0)frame[(y+gy+1)*32+((x+gx)>>3)]|=(byte)(128>>((x+gx)&7));
   }
   x+=12;if(x>=192)break;
  }
 }
}
