package org.fx991cnx.emulator;
import java.nio.file.*;
public final class BundledFirmwareCheck {
 public static void main(String[] args)throws Exception{
  byte[] rom=Files.readAllBytes(Path.of(args[0]));
  if(!BundledFirmware.valid(rom))throw new AssertionError("valid firmware rejected");
  byte[] changed=rom.clone();changed[4096]^=1;
  if(BundledFirmware.valid(changed)||BundledFirmware.valid(new byte[262143])||BundledFirmware.valid(null))throw new AssertionError("invalid firmware accepted");
  System.out.println("Bundled firmware validation passed: valid, altered, truncated and null inputs.");
 }
}
