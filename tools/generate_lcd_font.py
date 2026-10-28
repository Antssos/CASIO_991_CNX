#!/usr/bin/env python3
"""Subset the OFL Fusion Pixel 12px BDF into the LCD menu's 12x12 cells.
Usage: python tools/generate_lcd_font.py /path/to/fusion-pixel-12px-monospaced-zh_hans.bdf
Upstream version 2026.10.25. No ROM fonts are read or embedded.
"""
from pathlib import Path
import hashlib,sys
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parents[1]
needed=set('1234:主题按键雾霾蓝珍珠白樱花粉经典黑震动强度音量关轻中重')
path=Path(sys.argv[1]);lines=path.read_text().splitlines();glyphs={};i=0
while i<len(lines):
    if lines[i].startswith('STARTCHAR'):
        block=[];i+=1
        while lines[i]!='ENDCHAR':block.append(lines[i]);i+=1
        code=int(next(s.split()[1] for s in block if s.startswith('ENCODING ')))
        if code>=0 and chr(code) in needed:
            w,h,xoff,yoff=map(int,next(s.split()[1:] for s in block if s.startswith('BBX ')))
            advance=int(next(s.split()[1] for s in block if s.startswith('DWIDTH ')))
            start=block.index('BITMAP')+1;rows=[0]*12
            for gy,value in enumerate(block[start:start+h]):
                bits=int(value,16);y=10-yoff-h+gy
                if not 0<=y<12:continue
                for gx in range(w):
                    if bits & (1<<(((w+7)//8)*8-1-gx)):
                        # Native SETUP uses full-width, heavier numbers/colon.
                        xs=[(gx+xoff)*2,(gx+xoff)*2+1] if advance==6 else [gx+xoff]
                        for x in xs:
                            if 0<=x<12:rows[y]|=1<<x
            glyphs[chr(code)]=rows
    i+=1
# Match the native menu's 12px high, full-cell numerals rather than
# doubling the shorter Latin glyphs of the CJK pixel font.
number_font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf',16)
for char in '1234':
    im=Image.new('L',(12,12));ImageDraw.Draw(im).text((1,-3),char,font=number_font,fill=255)
    glyphs[char]=[sum(1<<x for x in range(12) if im.getpixel((x,y))>=100) for y in range(12)]
glyphs[':']=[sum(1<<x for x in range(4,7)) if y in (2,3,4,8,9,10) else 0 for y in range(12)]
assert needed<=glyphs.keys(),needed-glyphs.keys()
chars=''.join(sorted(glyphs))
java='package org.fx991cnx.emulator;\n\n// Generated 12px OFL pixel glyph subset; see licenses/Fusion-Pixel.\nfinal class LcdGlyphs {\n static final String CHARS="'+chars+'";\n static final int[][] ROWS={\n'
for char in chars:java+='  {'+','.join(map(str,glyphs[char]))+'},\n'
java+=' };\n static int[] get(char c){int index=CHARS.indexOf(c);return index<0?null:ROWS[index];}\n}\n'
(root/'app/src/main/java/org/fx991cnx/emulator/LcdGlyphs.java').write_text(java)
print('BDF SHA-256:', hashlib.sha256(path.read_bytes()).hexdigest())
