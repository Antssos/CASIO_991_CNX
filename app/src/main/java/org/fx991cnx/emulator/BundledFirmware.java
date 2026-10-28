package org.fx991cnx.emulator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class BundledFirmware {
 private static final byte[] EXPECTED=decode("8b6b6430f96858a8b3e765796e2d6f340bc7073ac9b0aa464775f15e53a10788");
 static boolean valid(byte[] bytes){
  if(bytes==null||bytes.length!=262144)return false;
  try{return MessageDigest.isEqual(EXPECTED,MessageDigest.getInstance("SHA-256").digest(bytes));}
  catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
 }
 private static byte[] decode(String value){byte[] bytes=new byte[value.length()/2];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);return bytes;}
}
