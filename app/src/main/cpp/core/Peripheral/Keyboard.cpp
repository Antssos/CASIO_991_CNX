#include "Keyboard.hpp"

#include "../Data/HardwareId.hpp"
#include "../Logger.hpp"
#include "../Chipset/MMU.hpp"
#include "../Emulator.hpp"
#include "../Chipset/Chipset.hpp"


#include <SDL.h>

namespace casioemu
{
	void Keyboard::Initialise()
	{

	    require_frame = true;

		/*
		 * When real_hardware is false, the program should emulate the behavior of the
		 * calculator emulator provided by Casio, which has different keyboard input
		 * interface.
		 */
		real_hardware = emulator.GetModelInfo("real_hardware");

		interrupt_source.Setup(5, emulator);

		region_ki.Setup(0xF040, 1, "Keyboard/KI", &keyboard_in, MMURegion::DefaultRead<uint8_t>, MMURegion::IgnoreWrite, emulator);

		region_input_filter.Setup(0xF042, 1, "Keyboard/InputFilter", &input_filter, MMURegion::DefaultRead<uint8_t>, MMURegion::DefaultWrite<uint8_t>, emulator);

		region_ko_mask.Setup(0xF044, 2, "Keyboard/KOMask", this, [](MMURegion *region, size_t offset) {
			offset -= region->base;
			Keyboard *keyboard = ((Keyboard *)region->userdata);
			return (uint8_t)((keyboard->keyboard_out_mask & 0x03FF) >> (offset * 8));
		}, [](MMURegion *region, size_t offset, uint8_t data) {
			offset -= region->base;
			Keyboard *keyboard = ((Keyboard *)region->userdata);
			keyboard->keyboard_out_mask &= ~(((uint16_t)0xFF) << (offset * 8));
			keyboard->keyboard_out_mask |= ((uint16_t)data) << (offset * 8);
			keyboard->keyboard_out_mask &= 0x03FF;
			if (!offset)
				keyboard->RecalculateKI();
		}, emulator);

		region_ko.Setup(0xF046, 2, "Keyboard/KO", this, [](MMURegion *region, size_t offset) {
			offset -= region->base;
			Keyboard *keyboard = ((Keyboard *)region->userdata);
			return (uint8_t)((keyboard->keyboard_out & 0x03FF) >> (offset * 8));
		}, [](MMURegion *region, size_t offset, uint8_t data) {
			offset -= region->base;
			Keyboard *keyboard = ((Keyboard *)region->userdata);
			keyboard->keyboard_out &= ~(((uint16_t)0xFF) << (offset * 8));
			keyboard->keyboard_out |= ((uint16_t)data) << (offset * 8);
			keyboard->keyboard_out &= 0x03FF;
			if (!offset)
				keyboard->RecalculateKI();
		}, emulator);

		if (!real_hardware)
		{
			keyboard_pd_emu = emulator.GetModelInfo("pd_value");
int offset = emulator.hardware_id == HW_ES_PLUS ? 0 : emulator.hardware_id == HW_CLASSWIZ ? 0x40000 : 0x80000;			region_ready_emu.Setup(offset + 0x8E00, 1, "Keyboard/ReadyStatusEmulator", &keyboard_ready_emu, MMURegion::DefaultRead<uint8_t>, MMURegion::DefaultWrite<uint8_t>, emulator);
			region_ki_emu.Setup(offset + 0x8E01, 1, "Keyboard/KIEmulator", &keyboard_in_emu, MMURegion::DefaultRead<uint8_t>, MMURegion::IgnoreWrite, emulator);
			region_ko_emu.Setup(offset + 0x8E02, 1, "Keyboard/KOEmulator", &keyboard_out_emu, MMURegion::DefaultRead<uint8_t>, MMURegion::IgnoreWrite, emulator);
			region_pd_emu.Setup(0xF050, 1, "Keyboard/PdValue", &keyboard_pd_emu, MMURegion::DefaultRead<uint8_t>, MMURegion::IgnoreWrite, emulator);
		}

        for (int code=0;code<128;code++) {
            if((code&15)>7 || (code>>4)>7) continue;
            auto& b=buttons[((code>>1)&0x38)|(code&7)];
            b.type=Button::BT_BUTTON; b.ko_bit=1<<(code>>4); b.ki_bit=1<<(code&7);
            b.pressed=b.stuck=false;
        }
    }

	void Keyboard::Reset()
	{
		p0 = false;
		p1 = false;
		p146 = false;
		keyboard_out = 0;
		keyboard_out_mask = 0;

		if (!real_hardware)
		{
			keyboard_in_emu = 0;
			keyboard_out_emu = 0;
		}

		RecalculateGhost();
	}

	void Keyboard::Tick()
	{
		if (has_input && interrupt_source.Enabled())
			interrupt_source.TryRaise();
	}





	void Keyboard::PressButton(Button& button, bool stick)
	{
		bool old_pressed = button.pressed;

		if (stick)
		{
			button.stuck = !button.stuck;
			button.pressed = button.stuck;
		}
		else
			button.pressed = true;

		require_frame = true;

		if (button.type == Button::BT_POWER && button.pressed && !old_pressed)
			emulator.chipset.Reset();
		if (button.type == Button::BT_BUTTON && button.pressed != old_pressed)
		{
			if (real_hardware)
				RecalculateGhost();
			else
			{
				// The emulator provided by Casio does not support multiple keys being pressed at once.
				if (button.pressed)
				{
					// Report an arbitrary pressed key.
					has_input = keyboard_in_emu = button.ki_bit;
					keyboard_out_emu = button.ko_bit;
				}
				else
				{
					// This key is released. There might still be other keys being held.
					has_input = keyboard_in_emu = keyboard_out_emu = 0;
				}
			}
		}
	}

	void Keyboard::PressAt(int x, int y, bool stick)
	{
		for (auto &button : buttons)
		{
			if (button.rect.x <= x && button.rect.y <= y && button.rect.x + button.rect.w > x && button.rect.y + button.rect.h > y)
			{
				PressButton(button, stick);
				break;
			}
		}
	}

	void Keyboard::RecalculateGhost()
	{
		struct KOColumn
		{
			uint8_t connections;
			bool seen;
		} columns[8];

		has_input = 0;
		for (auto &button : buttons)
			if (button.type == Button::BT_BUTTON && button.pressed && button.ki_bit & input_filter)
				has_input |= button.ki_bit;

		for (size_t cx = 0; cx != 8; ++cx)
		{
			columns[cx].seen = false;
			columns[cx].connections = 0;
			for (size_t rx = 0; rx != 8; ++rx)
			{
				Button &button = buttons[cx * 8 + rx];
				if (button.type == Button::BT_BUTTON && button.pressed)
				{
					for (size_t ax = 0; ax != 8; ++ax)
					{
						Button &sibling = buttons[ax * 8 + rx];
						if (sibling.type == Button::BT_BUTTON && sibling.pressed)
							columns[cx].connections |= 1 << ax;
					}
				}
			}
		}

		for (size_t cx = 0; cx != 8; ++cx)
		{
			if (!columns[cx].seen)
			{
				uint8_t to_visit = 1 << cx;
				uint8_t ghost_mask = 1 << cx;
				columns[cx].seen = true;

				while (to_visit)
				{
					uint8_t new_to_visit = 0;
					for (size_t vx = 0; vx != 8; ++vx)
					{
						if (to_visit & (1 << vx))
						{
							for (size_t sx = 0; sx != 8; ++sx)
							{
								if (columns[vx].connections & (1 << sx) && !columns[sx].seen)
								{
									new_to_visit |= 1 << sx;
									ghost_mask |= 1 << sx;
									columns[sx].seen = true;
								}
							}
						}
					}
					to_visit = new_to_visit;
				}

				for (size_t gx = 0; gx != 8; ++gx)
					if (ghost_mask & (1 << gx))
						keyboard_ghost[gx] = ghost_mask;
			}
		}

		RecalculateKI();
	}

	void Keyboard::RecalculateKI()
	{
		uint8_t keyboard_out_ghosted = 0;
		for (size_t ix = 0; ix != 7; ++ix)
			if (keyboard_out & ~keyboard_out_mask & (1 << ix))
				keyboard_out_ghosted |= keyboard_ghost[ix];

		keyboard_in = 0xFF;
		for (auto &button : buttons)
			if (button.type == Button::BT_BUTTON && button.pressed && button.ko_bit & keyboard_out_ghosted)
				keyboard_in &= ~button.ki_bit;

		if (keyboard_out & ~keyboard_out_mask & (1 << 7) && p0)
			keyboard_in &= 0x7F;
		if (keyboard_out & ~keyboard_out_mask & (1 << 8) && p1)
			keyboard_in &= 0x7F;
		if (keyboard_out & ~keyboard_out_mask & (1 << 9) && p146)
			keyboard_in &= 0x7F;
	}

	void Keyboard::ReleaseAll()
	{
		bool had_effect = false;
		for (auto &button : buttons)
		{
			if (!button.stuck && button.pressed)
			{
				button.pressed = false;
				if (button.type == Button::BT_BUTTON)
					had_effect = true;
			}
		}
		if (had_effect)
		{
			require_frame = true;
			if (real_hardware)
				RecalculateGhost();
			else
				has_input = keyboard_in_emu = keyboard_out_emu = 0;
		}
	}
}


namespace casioemu {
void Keyboard::Frame() { require_frame=false; }
void Keyboard::UIEvent(SDL_Event& e) {
 if(e.code==255) {if(e.down) emulator.chipset.Reset();return;}
 if(e.code<0 || e.code>0x77 || (e.code&15)>7) return;
 auto& b=buttons[((e.code>>1)&0x38)|(e.code&7)];
 b.pressed=e.down;RecalculateGhost();
}
}
