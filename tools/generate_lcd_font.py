#!/usr/bin/env python3
"""Reproducible 13px monochrome menu glyphs from OFL Noto Sans CJK SC.
This contains no ROM fonts. Full license is shipped in assets/licenses.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parents[1]
source=(root/'app/src/main/java/org/fx991cnx/emulator/LcdSettings.java').read_text()
font=ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc',12,index=2)
ascii_font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf',11)
glyphs={}
for char in sorted(set(source)|set(chr(i) for i in range(32,127))):
    if ord(char)<32:continue
    width=7 if ord(char)<128 else 13
    image=Image.new('L',(width,13));ImageDraw.Draw(image).text((0,0) if ord(char)<128 else (0,-3),char,font=ascii_font if ord(char)<128 else font,fill=255)
    glyphs[char]=[sum((1<<x) for x in range(width) if image.getpixel((x,y))>=100) for y in range(13)]
(root/'app/src/main/assets/lcd-font.json').write_text(json.dumps(glyphs,ensure_ascii=False,separators=(',',':'))+'\n')
