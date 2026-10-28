#include "Emulator.hpp"
#include "Chipset/CPU.hpp"
#include "Chipset/MMU.hpp"
#include <fstream>
#include <cassert>
#include <iostream>
#include <cstdarg>
namespace casioemu::logger {void Info(const char*,...) {}}
using namespace casioemu;
int main(int argc,char** argv){
 std::string path=argc>1?argv[1]:"/tmp/fx-test.rom";
 std::vector<unsigned char> rom(262144,0xff);
 auto w=[&](int a,int v){rom[a]=v&255;rom[a+1]=(v>>8)&255;};
 w(0,0xE000);w(2,0x0100);
 // Original test firmware: MOV r0,#5; MOV r1,#7; ADD r0,r1;
 // ST r0,0xD000; ST r0,0xF820; branch forever.
 int code[]={0x0005,0x0107,0x8011,0x9011,0xD000,0x9011,0xF820,0xCEFF};
 for(int i=0;i<8;i++)w(0x100+i*2,code[i]);
 std::ofstream out(path,std::ios::binary);out.write((char*)rom.data(),rom.size());out.close();
 Emulator e(path);e.chipset.Setup();e.chipset.SetupInternals();e.chipset.Reset();
 for(int i=0;i<50;i++)e.chipset.Tick();
 assert((uint8_t)e.chipset.cpu.reg_r[0]==12);
 assert(e.chipset.mmu.ReadData(0xD000)==12);assert(e.chipset.mmu.ReadData(0xF820)==12);
 auto& mmu=e.chipset.mmu;
 // Drive actual KO scan lines then verify KI active-low for all 49 matrix keys.
 int codes[]={0x02,0x12,0x22,0x32,0x42,0x01,0x11,0x21,0x31,0x41,0x00,0x10,0x20,0x30,0x40,0x64,0x63,0x62,0x61,0x60,0x05,0x15,0x25,0x35,0x45,0x55,0x04,0x14,0x24,0x34,0x44,0x54,0x03,0x13,0x23,0x33,0x43,0x53,0x06,0x16,0x46,0x56,0x07,0x17,0x47,0x26,0x37,0x27,0x36};
 mmu.WriteData(0xF044,0);mmu.WriteData(0xF045,0);
 for(int code:codes){mmu.WriteData(0xF046,1<<(code>>4));SDL_Event down{code,true};e.chipset.UIEvent(down);assert(mmu.ReadData(0xF040)==(uint8_t)~(1<<(code&7)));SDL_Event up{code,false};e.chipset.UIEvent(up);assert(mmu.ReadData(0xF040)==255);}
 // Serialize whole core, mutate, restore, verify byte-exact canonical state.
 std::vector<uint8_t> data;StateIO writer{data,false};e.chipset.State(writer);
 mmu.WriteData(0xD000,99);e.chipset.cpu.reg_r[0]=123;
 StateIO reader{data,true};e.chipset.State(reader);
 assert(mmu.ReadData(0xD000)==12);assert((uint8_t)e.chipset.cpu.reg_r[0]==12);
 std::vector<uint8_t> again;StateIO writer2{again,false};e.chipset.State(writer2);assert(data==again);
 // Cross-instance restoration after a cold reset.
 Emulator e2(path);e2.chipset.Setup();e2.chipset.SetupInternals();e2.chipset.Reset();StateIO reader2{data,true};e2.chipset.State(reader2);assert(e2.chipset.mmu.ReadData(0xD000)==12);
 std::cout<<"PASS: MCU instructions, RAM, LCD VRAM, 49 physical matrix keys, full-state round trip and cold-instance restoration ("<<data.size()<<" bytes)\n";
}
