#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
rom_input=$(realpath "${1:?Usage: tests/run-setup-integration.sh /path/to/private-fx991cnx.rom}")
scratch="$PWD/build/private-setup-check"
mkdir -p "$scratch/classes"
c++ -std=c++17 -O2 -Iapp/src/main/cpp/core tests/rom_probe.cpp app/src/main/cpp/core/Chipset/*.cpp app/src/main/cpp/core/Peripheral/*.cpp -o "$scratch/rom-probe"
python3 - "$scratch" <<'PY_SCRIPT'
from pathlib import Path
import sys
folder=Path(sys.argv[1]);lines=['tick 4000000',f'dump {folder}/result.lcd','tap 0x07','tap 0x47']
for i in range(1,5):
    lines.append(f'dump {folder}/setup-page{i}.lcd')
    if i<4:lines.append('tap 0x36')
(folder/'input.script').write_text('\n'.join(lines)+'\n')
PY_SCRIPT
"$scratch/rom-probe" "$rom_input" "$scratch/input.script"
javac -d "$scratch/classes" app/src/main/java/org/fx991cnx/emulator/NativeSetupPage.java app/src/main/java/org/fx991cnx/emulator/LcdSettings.java app/src/main/java/org/fx991cnx/emulator/LcdGlyphs.java tests/jvm/*.java tests/SetupPageCheck.java
java -cp "$scratch/classes" org.fx991cnx.emulator.SetupPageCheck "$scratch"
java -cp "$scratch/classes" org.fx991cnx.emulator.MenuCheck "$scratch"
