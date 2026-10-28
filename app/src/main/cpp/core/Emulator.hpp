#pragma once
#include "Data/HardwareId.hpp"
#include "Chipset/Chipset.hpp"
#include <map>
#include <string>
namespace casioemu {
class Emulator {
public:
 HardwareId hardware_id = HW_CLASSWIZ;
 std::map<std::string,std::string> argv_map;
 Chipset chipset;
 std::string romPath;
 bool paused=false;
 explicit Emulator(const std::string& path):chipset(*this),romPath(path) {}
 int GetModelInfo(const char* key) { return std::string(key)=="csr_mask"?15:std::string(key)=="real_hardware"?1:0; }
 std::string GetModelFilePath(int) { return romPath; }
 unsigned long long GetCyclesPerSecond(){return 2000000;}
 void HandleMemoryError() {}
 void SetPaused(bool value){paused=value;}
};
}
