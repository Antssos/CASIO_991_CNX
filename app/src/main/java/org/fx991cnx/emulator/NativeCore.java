package org.fx991cnx.emulator;
final class NativeCore {
 static { System.loadLibrary("fx991cnx"); }
 static native String open(String path);
 static native byte[] frame(int ticks);
 static native void key(int code, boolean down);
 static native byte[] save();
 static native boolean restore(byte[] data);
 static native String error();
}
