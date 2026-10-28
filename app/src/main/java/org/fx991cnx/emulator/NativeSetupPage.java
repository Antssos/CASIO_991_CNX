package org.fx991cnx.emulator;

import java.security.MessageDigest;

/** Matches rendered language/contrast labels, never ROM addresses or a key-count guess. */
final class NativeSetupPage {
 private static final byte[] LABEL_HASH=hex("ded0826e2f82ee4042a12ff26cdfd6eda571ac9455035a564882829a5e60d176");
 static boolean matches(byte[] frame){
  if(frame==null||frame.length!=2050||(frame[2048]&7)!=5)return false;
  try{MessageDigest hash=MessageDigest.getInstance("SHA-256");
   // Keep status icons, contrast and the scrollbar outside the recognition region.
   for(int y=0;y<32;y++)hash.update(frame,(y+1)*32,12);
   return MessageDigest.isEqual(hash.digest(),LABEL_HASH);
  }catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
 }
 private static byte[] hex(String s){byte[] bytes=new byte[s.length()/2];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return bytes;}
}
