package org.fx991cnx.emulator;
import java.nio.file.*;
/** Uses privately supplied native screen dumps; contains no firmware or glyph data. */
public final class SetupPageCheck {
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[0]);
  for(int page=1;page<=4;page++){
   byte[] frame=Files.readAllBytes(dir.resolve("setup-page"+page+".lcd"));
   if(NativeSetupPage.matches(frame)!=(page==4))throw new AssertionError("wrong SETUP page routing: "+page);
   if(page==4){
    frame[0]^=1;frame[2049]=4;frame[32+23]^=3;
    if(!NativeSetupPage.matches(frame))throw new AssertionError("status/contrast/scrollbar must not break recognition");
    frame[32]^=1;
    if(NativeSetupPage.matches(frame))throw new AssertionError("unrelated text must not be intercepted");
   }
  }
  byte[] result=Files.readAllBytes(dir.resolve("result.lcd"));
  if(NativeSetupPage.matches(result))throw new AssertionError("calculation mistaken for SETUP");
  if(NativeSetupPage.matches(null)||NativeSetupPage.matches(new byte[2049]))throw new AssertionError("invalid frame");
  System.out.println("Native SETUP recognition: 4 pages, calculation, dynamic status and negative cases passed.");
 }
}
