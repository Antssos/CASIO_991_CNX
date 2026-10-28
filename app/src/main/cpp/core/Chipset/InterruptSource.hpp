#pragma once
#include "../Config.hpp"
#include "../StateIO.hpp"

namespace casioemu
{
	class Emulator;

	class InterruptSource
	{
		Emulator *emulator;
		bool raise_success{}, setup_done{};
		size_t interrupt_index;

	public:
		InterruptSource();
 void State(StateIO& io){io(raise_success);}
		void Setup(size_t interrupt_index, Emulator &_emulator);
		bool Enabled();
		bool TryRaise();
		bool Success();
	};
}

