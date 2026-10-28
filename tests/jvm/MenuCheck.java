package org.fx991cnx.emulator;
import java.nio.file.*;
import java.util.*;
public final class MenuCheck {
 static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[0]);MainActivity host=new MainActivity();LcdSettings menu=new LcdSettings(host);
  for(int i=1;i<=3;i++){
   byte[] page=Files.readAllBytes(dir.resolve("setup-page"+i+".lcd"));
   require(menu.compose(page)==page,"unmodified native SETUP page "+i);
   require(!menu.keyDown(0x20,page)&&!menu.keyDown(0x01,page),"native 3/4 pass through on other pages");
  }
  byte[] page=Files.readAllBytes(dir.resolve("setup-page4.lcd"));byte[] original=page.clone();byte[] rendered=menu.compose(page);
  require(Arrays.equals(page,original),"never modify MCU VRAM");
  for(int y=0;y<32;y++)for(int x=0;x<32;x++)require(page[(y+1)*32+x]==rendered[(y+1)*32+x],"preserve native first two rows");
  int dots=0;for(int y=32;y<63;y++)for(int x=0;x<12;x++)dots+=Integer.bitCount(rendered[(y+1)*32+x]&255);
  require(dots>100,"theme and key rows visible");
  require(!menu.keyDown(0x00,page)&&!menu.keyDown(0x10,page),"language and contrast stay native");
  require(menu.keyDown(0x20,page)&&menu.active(),"3 opens themes");
  require(menu.keyDown(0x01,page)&&host.ui.theme==3&&!menu.active(),"4 selects black, returns to native last page");
  require(host.redraws==1,"theme redraw callback");
  require(menu.keyDown(0x01,page),"4 opens keys");
  menu.keyDown(0x00,page);menu.keyDown(0x00,page);require(host.ui.vibration==0,"1 disables vibration inside strength");
  menu.keyDown(0x00,page);menu.keyDown(0x01,page);require(host.ui.vibration==3,"4 chooses heavy vibration");
  menu.keyDown(0x10,page);menu.keyDown(0x00,page);require(host.ui.sound==0,"1 disables sound inside volume");
  menu.keyDown(0x10,page);menu.keyDown(0x20,page);require(host.ui.sound==2,"3 chooses medium sound");
  menu.keyDown(0x32,page);require(!menu.active(),"DEL returns to native page");
  menu.keyDown(0x01,page);require(!menu.keyDown(0x42,page)&&!menu.active(),"AC passes to firmware and closes extension");
  byte[] result=Files.readAllBytes(dir.resolve("result.lcd"));require(!menu.keyDown(0x20,result)&&menu.compose(result)==result,"calculation untouched");
  System.out.println("Menu regression passed: native pass-through, 3/4 insertion, themes, off/light/medium/heavy and back/AC routing.");
 }
}
