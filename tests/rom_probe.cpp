// GPL-3.0. Diagnostic runner for a separately supplied ROM; never embeds firmware.
#include "Emulator.hpp"
#include "Chipset/CPU.hpp"
#include "Chipset/MMU.hpp"
#include <fstream>
#include <iostream>
#include <sstream>
#include <vector>
namespace casioemu::logger {void Info(const char*,...) {}}
int main(int argc,char** argv){
 if(argc!=3){std::cerr<<"usage: rom-probe ROM SCRIPT\n";return 2;}
 try{
 casioemu::Emulator e(argv[1]);e.chipset.Setup();e.chipset.SetupInternals();e.chipset.Reset();
 auto tick=[&](int n){for(int i=0;i<n;i++)e.chipset.Tick();};
 std::ifstream script(argv[2]);std::string line;
 while(std::getline(script,line)){
  if(line.empty()||line[0]=='#')continue;std::istringstream in(line);std::string op;in>>op;
  if(op=="tick"){int n;in>>n;tick(n);}
  else if(op=="dump"){std::string path;in>>path;std::ofstream out(path,std::ios::binary);for(int i=0;i<2048;i++)out.put(e.chipset.mmu.ReadData(0xF800+i));out.put(e.chipset.mmu.ReadData(0xF031));out.put(e.chipset.mmu.ReadData(0xF032));std::cout<<"LCD "<<path<<" PC="<<std::hex<<(uint16_t)e.chipset.cpu.reg_csr<<":"<<(uint16_t)e.chipset.cpu.reg_pc<<std::dec<<"\n";}
  else if(op=="tap"){std::string value;in>>value;int code=std::stoi(value,nullptr,0);SDL_Event down{code,true};e.chipset.UIEvent(down);tick(160000);SDL_Event up{code,false};e.chipset.UIEvent(up);tick(80000);}
 }
 }catch(const std::exception& ex){std::cerr<<ex.what()<<"\n";return 1;}
}
