#include "core/Emulator.hpp"
#include "core/Chipset/MMU.hpp"
#include <jni.h>
#include <memory>
#include <mutex>
#include <fstream>
#include <cstdarg>
#include <chrono>
using namespace casioemu;
namespace casioemu::logger {void Info(const char*,...) {}}
static std::unique_ptr<Emulator> emu;
static std::mutex lock;
static std::string error;
static std::string str(JNIEnv* e,jstring s){const char* p=e->GetStringUTFChars(s,0);std::string r(p);e->ReleaseStringUTFChars(s,p);return r;}
extern "C" JNIEXPORT jstring JNICALL Java_org_fx991cnx_emulator_NativeCore_open(JNIEnv* e,jclass,jstring path){
 std::lock_guard<std::mutex> g(lock);error.clear();
 try {emu.reset();auto x=std::make_unique<Emulator>(str(e,path));x->chipset.Setup();x->chipset.SetupInternals();x->chipset.Reset();emu=std::move(x);}
 catch(const std::exception& ex){error=ex.what();emu.reset();}
 return e->NewStringUTF(error.c_str());
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_org_fx991cnx_emulator_NativeCore_frame(JNIEnv* e,jclass,jint ticks){
 std::lock_guard<std::mutex> g(lock);uint8_t frame[2050]{};
 if(emu && error.empty())try {
  for(int i=0;i<ticks;i++)emu->chipset.Tick();
  for(int i=0;i<2048;i++)frame[i]=emu->chipset.mmu.ReadData(0xF800+i);
  frame[2048]=emu->chipset.mmu.ReadData(0xF031);frame[2049]=emu->chipset.mmu.ReadData(0xF032);
 }catch(const std::exception& ex){error=ex.what();}
 auto a=e->NewByteArray(sizeof frame);e->SetByteArrayRegion(a,0,sizeof frame,(jbyte*)frame);return a;
}
extern "C" JNIEXPORT void JNICALL Java_org_fx991cnx_emulator_NativeCore_key(JNIEnv*,jclass,jint code,jboolean down){
 std::lock_guard<std::mutex> g(lock);if(!emu || !error.empty())return;
 try{SDL_Event event{code,(bool)down};emu->chipset.UIEvent(event);}catch(const std::exception& ex){error=ex.what();}
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_org_fx991cnx_emulator_NativeCore_save(JNIEnv* e,jclass){
 std::lock_guard<std::mutex> g(lock);std::vector<uint8_t> data;
 if(emu && error.empty()){StateIO io{data,false};emu->chipset.State(io);}
 auto a=e->NewByteArray(data.size());if(!data.empty())e->SetByteArrayRegion(a,0,data.size(),(jbyte*)data.data());return a;
}
extern "C" JNIEXPORT jboolean JNICALL Java_org_fx991cnx_emulator_NativeCore_restore(JNIEnv* e,jclass,jbyteArray a){
 std::lock_guard<std::mutex> g(lock);if(!emu)return false;
 std::vector<uint8_t> data(e->GetArrayLength(a));e->GetByteArrayRegion(a,0,data.size(),(jbyte*)data.data());
 try {std::vector<uint8_t> original;StateIO backup{original,false};emu->chipset.State(backup);
  if(data.size()!=original.size())return false;
  StateIO io{data,true};emu->chipset.State(io);return true;
 }catch(...){return false;}
}
extern "C" JNIEXPORT jstring JNICALL Java_org_fx991cnx_emulator_NativeCore_error(JNIEnv* e,jclass){std::lock_guard<std::mutex> g(lock);return e->NewStringUTF(error.c_str());}
