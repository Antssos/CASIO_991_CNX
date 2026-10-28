package org.fx991cnx.emulator;

import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Frontend-only SETUP extension. Calculation and the original SETUP remain in firmware. */
final class LcdSettings {
 private final MainActivity host;
 private JSONObject font;
 private String page="";
 private int selected=0;
 private byte[] display;
 LcdSettings(MainActivity host){this.host=host;try(InputStream in=host.getAssets().open("lcd-font.json")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1)b.write(chunk,0,n);font=new JSONObject(b.toString(StandardCharsets.UTF_8.name()));}catch(Exception e){throw new IllegalStateException("LCD font unavailable",e);}}
 boolean active(){return !page.isEmpty();}
 byte[] frame(){return display;}
 void open(){go("root");}
 private void go(String name){page=name;selected=0;render();}
 private String[] items(){UiPreferences ui=host.ui;switch(page){
  case "root":return new String[]{"原机设置","主题","按键","关于"};
  case "theme":return new String[]{"雾霾蓝"+(ui.theme()==0?" *":""),"珍珠白"+(ui.theme()==1?" *":""),"樱花粉"+(ui.theme()==2?" *":""),"经典黑"+(ui.theme()==3?" *":"")};
  case "keys":return new String[]{"震动："+(ui.vibration()?"开":"关"),"按键音："+(ui.sound()?"开":"关"),"震动强度","按键音量"};
  case "vibration":case "sound":return new String[]{"开","关"};
  case "strength":return new String[]{"轻"+(ui.strength()==0?" *":""),"标准"+(ui.strength()==1?" *":""),"强"+(ui.strength()==2?" *":"")};
  case "about":return new String[]{"使用说明","ROM 管理","版本与许可"};
  case "rom":return new String[]{"导入或更换 ROM","清除计算器状态"};
  case "reset":return new String[]{"确认清除状态","取消"};
  default:return new String[0];
 }}
 void press(int code){
  if(code==0x32||code==0x42||code==0x47||code==0xff){back();return;}
  if(page.equals("volume")){
   if(code==0x26||code==0x36)host.ui.volume(Math.max(0,host.ui.volume()-5));
   if(code==0x37||code==0x27)host.ui.volume(Math.min(100,host.ui.volume()+5));
   if(code==0x02)host.ui.volume(100);if(code==0x64)host.ui.volume(0);
   if(code==0x60){host.keyFeedback();go("keys");return;}render();return;
  }
  if(page.equals("version")&&code==0x60){host.license();return;}
  String[] options=items();if(options.length==0){if(code==0x60)back();return;}
  if(code==0x27){selected=(selected+options.length-1)%options.length;render();return;}
  if(code==0x36){selected=(selected+1)%options.length;render();return;}
  int number=code==0x00?0:code==0x10?1:code==0x20?2:code==0x01?3:-1;
  if(number>=0&&number<options.length){selected=number;choose();}else if(code==0x60)choose();
 }
 private void choose(){int choice=selected;switch(page){
  case "root":if(choice==0){page="";display=null;}else go(new String[]{"","theme","keys","about"}[choice]);break;
  case "theme":host.ui.theme(choice);host.appearanceChanged();render();break;
  case "keys":go(new String[]{"vibration","sound","strength","volume"}[choice]);break;
  case "vibration":host.ui.vibration(choice==0);host.keyFeedback();go("keys");break;
  case "sound":host.ui.sound(choice==0);host.keyFeedback();go("keys");break;
  case "strength":host.ui.strength(choice);host.keyFeedback();render();break;
  case "about":go(new String[]{"help","rom","version"}[choice]);break;
  case "rom":if(choice==0){host.importRom();}else go("reset");break;
  case "reset":if(choice==0)host.resetCalculator();go("rom");break;
 }}
 void back(){switch(page){
  case "root":page="";display=null;host.closeSettingsToCalculator();return;
  case "theme":case "keys":case "about":go("root");return;
  case "vibration":case "sound":case "strength":case "volume":go("keys");return;
  case "reset":go("rom");return;
  default:go("about");
 }}
 private void render(){
  display=new byte[2050];display[2048]=5;display[2049]=20;
  String[] lines;
  if(page.equals("volume"))lines=new String[]{"按键音量："+host.ui.volume()+"%","方向键：调整 5%","7：最大  0：静音","=：保存  DEL：返回"};
  else if(page.equals("help"))lines=new String[]{"SHIFT + MENU：设置","数字或方向键选择","=：确认 DEL：返回","设置自动保存，离线运行"};
  else if(page.equals("version"))lines=new String[]{"fx-991CN X  0.4.0","独立项目，非官方应用","核心 CasioEmuNeo GPL3","=：查看许可全文"};
  else{String[] choices=items();lines=new String[choices.length];for(int i=0;i<lines.length;i++)lines[i]=(i==selected?">":" ")+(i+1)+":"+choices[i];}
  for(int row=0;row<Math.min(4,lines.length);row++)draw(lines[row],row*16);
 }
 private void draw(String text,int y){int x=0;for(int i=0;i<text.length();i++){
  String c=text.substring(i,i+1);JSONArray glyph=font.optJSONArray(c);int width=c.charAt(0)<128?7:13;
  if(glyph!=null)for(int gy=0;gy<13&&y+gy<63;gy++){int bits=glyph.optInt(gy);for(int gx=0;gx<width&&x+gx<192;gx++)if((bits&(1<<gx))!=0)display[(y+gy+1)*32+((x+gx)>>3)]|=(byte)(128>>((x+gx)&7));}
  x+=width;if(x>=192)break;
 }}
}
