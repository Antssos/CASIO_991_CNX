#include "Screen.hpp"
#include "../Emulator.hpp"
#include "../Chipset/MMURegion.hpp"
namespace casioemu {
class NativeScreen:public Peripheral {
 MMURegion vram,range,mode,contrast;
 uint8_t pixels[2048]{}; uint8_t r=0,m=0,c=0;
public:
 using Peripheral::Peripheral;
 void Initialise() override {
 vram.Setup(0xF800,2048,"LCD/VRAM",pixels,[](MMURegion* r,size_t a)->uint8_t {
  return (a-r->base)%32<24 ? ((uint8_t*)r->userdata)[a-r->base]:0;
 },[](MMURegion* r,size_t a,uint8_t d){if((a-r->base)%32<24)((uint8_t*)r->userdata)[a-r->base]=d;},emulator);
 range.Setup(0xF030,1,"LCD/Range",&r,MMURegion::DefaultRead<uint8_t,7>,MMURegion::DefaultWrite<uint8_t,7>,emulator);
 mode.Setup(0xF031,1,"LCD/Mode",&m,MMURegion::DefaultRead<uint8_t,7>,MMURegion::DefaultWrite<uint8_t,7>,emulator);
 contrast.Setup(0xF032,1,"LCD/Contrast",&c,MMURegion::DefaultRead<uint8_t,63>,MMURegion::DefaultWrite<uint8_t,63>,emulator);
 }
 void State(StateIO& io) override {io.bytes(pixels,sizeof pixels);io(r,m,c);}
};
Peripheral* CreateScreen(Emulator& e) {return new NativeScreen(e);}
}
