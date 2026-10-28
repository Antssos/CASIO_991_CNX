#pragma once
#include "../Config.hpp"

#include "Peripheral.hpp"
#include "../Chipset/MMURegion.hpp"

namespace casioemu
{
	class BatteryBackedRAM : public Peripheral
	{
		MMURegion region, region_2;

		size_t ram_size;
		bool ram_file_requested{};
		uint8_t *ram_buffer = nullptr;


	public:
		static char* rom_addr;
	
		using Peripheral::Peripheral;
 void State(StateIO& io) override {io.bytes(ram_buffer,ram_size);}
		void Initialise();
		void Uninitialise();
		void SaveRAMImage();
		void LoadRAMImage();
	};
}

