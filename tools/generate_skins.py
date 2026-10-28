#!/usr/bin/env python3
"""GPL-3.0: source-drawn CN X faceplates, measured from user reference photos.
No product photograph, ROM display or downloaded font is embedded.
Run from any directory. Geometry and native touch regions share this source.
"""
from pathlib import Path
from html import escape
import random
from functools import lru_cache
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
# Only glyph outlines are generated; the full fonts are never bundled.
FONT_ROOT=Path('/usr/share/fonts')
@lru_cache(maxsize=8)
def font(face,bold):
 if face=='math':return TTFont(str(FONT_ROOT/'truetype/dejavu/DejaVuSerif-Italic.ttf'),lazy=True)
 if face=='cjk':return TTFont(str(FONT_ROOT/'opentype/noto'/('NotoSansCJK-Bold.ttc' if bold else 'NotoSansCJK-Regular.ttc')),fontNumber=2,lazy=True)
 suffix=('-Bold' if bold else '')+'.ttf'
 return TTFont(str(FONT_ROOT/'truetype/dejavu'/(('DejaVuSansCondensed' if face=='condensed' else 'DejaVuSans')+suffix)),lazy=True)
@lru_cache(maxsize=1024)
def glyph(character,face,bold):
 selected=font(face,bold)
 if ord(character) not in selected.getBestCmap():selected=font('cjk',bold)
 name=selected.getBestCmap().get(ord(character))
 if name is None:raise ValueError('Missing faceplate character '+repr(character))
 glyphs=selected.getGlyphSet();pen=SVGPathPen(glyphs);glyphs[name].draw(pen)
 return pen.getCommands(),selected['hmtx'][name][0],selected['head'].unitsPerEm
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'app/src/main/assets/skins'
DEST.mkdir(parents=True,exist_ok=True)
WIDTH,HEIGHT=344,736
KEYS=[]
for y in (507,558,609,660):
 for x in (22,84,145,207,269):KEYS.append([x,y,53,36])
for y in (379,421,463):
 for x in (23,74,126,177,228,279):KEYS.append([x,y,43,26])
for x in (23,74,228,279):KEYS.append([x,340,43,26])
for x in (29,76,239,286):KEYS.append([x,280,30,30])
KEYS += [[118,294,31,42],[195,294,31,42],[146,273,51,30],[146,327,51,29]]
DPAD=[[(137,294),(149,302),(141,314),(149,326),(137,335),(123,326),(119,314),(123,303)],[(207,294),(195,302),(203,314),(195,326),(207,335),(221,326),(225,314),(221,303)],[(149,281),(172,273),(195,281),(184,299),(172,292),(160,299)],[(149,347),(172,355),(195,347),(184,329),(172,336),(160,329)]]
THEMES=[
 dict(id='blue',name='雾霾蓝',body='#a9c8dc',bottom='#87a9c2',edge='#6e8ba2',key='#a1bed2',number='#7295af',ink='#f8fbfd',brand='#f7f9fb',dpad='#89a9c1',ac='#e9c8b4',acink='#575456'),
 dict(id='ivory',name='珍珠白',body='#eee8e2',bottom='#dad3cd',edge='#c5bcb5',key='#ece7df',number='#f4f1e9',ink='#5f6060',brand='#677078',dpad='#f1ede5',ac='#e4c6b4',acink='#696262'),
 dict(id='pink',name='樱花粉',body='#edb9c3',bottom='#d99baa',edge='#ad7f91',key='#e7b8c0',number='#bd8c9d',ink='#fff8f7',brand='#fff6f6',dpad='#ce96a8',ac='#e7c4b0',acink='#6d6260'),
 dict(id='black',name='经典黑',body='#3c3e3e',bottom='#333536',edge='#c4c7c7',key='#3a3c3d',number='#f2f1ec',ink='#f5f4ef',brand='#e4e5e3',dpad='#909497',ac='#125e9d',acink='#f6f8fa'),
]
MAIN=['7','8','9','DEL','AC','4','5','6','×','÷','1','2','3','+','−','0','.','×10','Ans','=','fraction','sqrt','square','power','log','ln','(−)','° ′ ″','x⁻¹','sin','cos','tan','STO','ENG','(',')','S⇔D','M+','OPTN','CALC','integral','x','','','','','','','','']
SHIFT=['科学常数','单位换算','复位','插入','关机','','','','nPr','nCr','','','','Pol','Rec','Rnd','Ran#','π','%','≈','a b/c','³√','x³','ⁿ√','10ˣ','eˣ','log','FACT','x!','sin⁻¹','cos⁻¹','tan⁻¹','调用','←','Abs',',','a b/c','M−','','SOLVE','d/dx','Σ','','','','','','','','']
ALPHA=['','','','撤消','','','','','','','','','','','','','RanInt','e','','','÷R','','','','','','A','B','C','D','E','F','','i','','x','y','M','','=',':','','','','','','','','','']
BASE={22:'DEC',23:'HEX',24:'BIN',25:'OCT'}

def rgb(c):return tuple(int(c[i:i+2],16) for i in (1,3,5))
def tint(c,d):return '#'+''.join(f'{max(0,min(255,v+d)):02x}' for v in rgb(c))
def theme_svg(t):
 out=[]
 def add(s):out.append(s)
 def rect(x,y,w,h,r,fill,extra=''):add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" {extra}/>')
 def path(d,fill,extra=''):add(f'<path d="{d}" fill="{fill}" {extra}/>')
 def text(s,x,y,size,ink,weight=400,anchor='middle',font='sans-serif-condensed'):
  face='math' if font=='math' else 'condensed' if font=='sans-serif-condensed' else 'latin';parts=[glyph(ch,face,weight>=600) for ch in s]
  width=sum(advance/upem*size for _,advance,upem in parts)
  current=x-width/2 if anchor=='middle' else x
  for outline,advance,upem in parts:
   scale=size/upem
   if outline:add(f'<path d="{outline}" transform="translate({current:.4f} {y}) scale({scale:.8f} {-scale:.8f})" fill="{ink}"/>')
   current+=advance*scale
 # Keycap figures use custom squared, rounded-corner outlines, not a system font.
 def digit(value,cx,cy,ink,scale=1):
  outlines={
   '0':'M4.1 1H8.9Q11.3 1 11.3 3.5V14.5Q11.3 17 8.9 17H4.1Q1.7 17 1.7 14.5V3.5Q1.7 1 4.1 1Z',
   '1':'M3.4 4L6.9 1H7.3V17M3.8 17H10.7',
   '2':'M1.7 4V3.5Q1.7 1 4.2 1H8.8Q11.3 1 11.3 3.5V5.2Q11.3 7.1 9.5 8.5L3.5 12.8Q1.7 14.1 1.7 17H11.5',
   '3':'M1.8 1H8.7Q11.3 1 11.3 3.5V6.3Q11.3 9 8.6 9H4.6M8.6 9Q11.3 9 11.3 11.5V14.5Q11.3 17 8.7 17H1.8',
   '4':'M2.2 1L1.6 11H12M9.3 1V17',
   '5':'M11.4 1H1.8V8.5H8.8Q11.3 8.5 11.3 11.1V14.5Q11.3 17 8.8 17H1.8',
   '6':'M10.9 1H4.2Q1.7 1 1.7 3.5V14.5Q1.7 17 4.2 17H8.8Q11.3 17 11.3 14.5V11Q11.3 8.5 8.8 8.5H1.7',
   '7':'M1.2 1H11.6L5.7 17',
   '8':'M4.2 1H8.8Q11.2 1 11.2 3.5V6.1Q11.2 8.5 8.8 8.5H4.2Q1.8 8.5 1.8 6.1V3.5Q1.8 1 4.2 1ZM4.2 8.5H8.8Q11.3 8.5 11.3 11V14.5Q11.3 17 8.8 17H4.2Q1.7 17 1.7 14.5V11Q1.7 8.5 4.2 8.5Z',
   '9':'M11.3 9H4.2Q1.7 9 1.7 6.5V3.5Q1.7 1 4.2 1H8.8Q11.3 1 11.3 3.5V14.5Q11.3 17 8.8 17H2.2'
  }
  weight=2.45 if t['id']=='black' else 2.05
  add(f'<path d="{outlines[value]}" transform="translate({cx-6.5*scale} {cy-9*scale}) scale({scale})" fill="none" stroke="{ink}" stroke-width="{weight}" stroke-linejoin="round" stroke-linecap="square"/>')
 def arithmetic(value,cx,cy,ink,scale=1):
  weight=2.6 if t['id']=='black' else 2.25
  if value=='+':d=f'M{cx-7.8*scale} {cy}H{cx+7.8*scale}M{cx} {cy-7.8*scale}V{cy+7.8*scale}'
  elif value=='−':d=f'M{cx-7.6*scale} {cy}H{cx+7.6*scale}'
  elif value=='=':d=f'M{cx-7.7*scale} {cy-3.5*scale}H{cx+7.7*scale}M{cx-7.7*scale} {cy+3.5*scale}H{cx+7.7*scale}'
  elif value=='×':d=f'M{cx-5.8*scale} {cy-5.8*scale}L{cx+5.8*scale} {cy+5.8*scale}M{cx-5.8*scale} {cy+5.8*scale}L{cx+5.8*scale} {cy-5.8*scale}'
  else:
   d=f'M{cx-7.8*scale} {cy}H{cx+7.8*scale}'
   for dy in (-6.5,6.5):add(f'<circle cx="{cx}" cy="{cy+dy*scale}" r="{1.65*scale}" fill="{ink}"/>')
  path(d,'none',f'stroke="{ink}" stroke-width="{weight*scale}" stroke-linecap="square" stroke-linejoin="round"')
 add(f'<svg xmlns="http://www.w3.org/2000/svg" width="344" height="736" viewBox="0 0 344 736">')
 add('<defs>')
 for ident,a,b in [('body',t['body'],t['bottom']),('edge',tint(t['edge'],25),t['edge']),('key',tint(t['key'],20),tint(t['key'],-12)),('number',tint(t['number'],14),tint(t['number'],-8)),('ac',tint(t['ac'],17),tint(t['ac'],-10))]:
  add(f'<linearGradient id="{ident}" x1="0" y1="0" x2="0" y2="1" gradientUnits="objectBoundingBox"><stop stop-color="{a}"/><stop offset="1" stop-color="{b}"/></linearGradient>')
 add(f'<radialGradient id="round" cx=".27" cy=".2" r=".9"><stop stop-color="{tint(t["dpad"],40)}"/><stop offset=".5" stop-color="{t["dpad"]}"/><stop offset="1" stop-color="{tint(t["dpad"],-45)}"/></radialGradient>')
 add('<linearGradient id="solar" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#2e2424"/><stop offset="1" stop-color="#312c23"/></linearGradient>')
 add('<linearGradient id="glass" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#c0cec0"/><stop offset="1" stop-color="#a6bcae"/></linearGradient>')
 add('<pattern id="carbon" width="10" height="10" patternUnits="userSpaceOnUse"><rect width="10" height="10" fill="#363838"/><path d="M0 1L4 3V6L0 4ZM5 6L9 8V11L5 9Z" fill="#404241"/><path d="M5 1L9 3V6L5 4ZM0 6L4 8V11L0 9Z" fill="#2f3130"/><path d="M0 0L4 2M5 5L9 7" stroke="#51534f" stroke-opacity=".16" stroke-width=".7"/></pattern>')
 add('<pattern id="grain" width="24" height="24" patternUnits="userSpaceOnUse">')
 rng=random.Random(991)
 for _ in range(32):
  x,y=rng.randrange(24),rng.randrange(24)
  rect(x,y,.5,.35,.1,'#ffffff',f'opacity="{rng.uniform(.08,.26):.2f}"')
 add('</pattern>')
 add('<clipPath id="faceClip"><path d="M57 13C30 13 17 28 16 58L14 678C14 703 25 714 53 716H291C319 714 330 703 330 678L328 58C327 28 314 13 287 13Z"/></clipPath>')
 add('</defs>')
 path('M57 8C27 8 12 25 11 57L8 680C8 714 23 730 53 732H291C322 730 337 714 337 680L333 57C332 25 317 8 287 8Z','url(#edge)')
 path('M57 13C30 13 17 28 16 58L14 678C14 703 25 714 53 716H291C319 714 330 703 330 678L328 58C327 28 314 13 287 13Z','url(#body)', 'stroke="#ffffff" stroke-opacity=".38" stroke-width="1"')
 rect(14,13,316,703,0,'url(#carbon)' if t['id']=='black' else 'url(#grain)','clip-path="url(#faceClip)"')
 path('M28 687C29 707 41 710 57 710H287C309 709 319 701 321 687','none','stroke="#000000" stroke-opacity=".12" stroke-width="1"')
 # CASIO's compact geometric wordmark is drawn as shapes, not substituted system text.
 add(f'<g transform="translate(35 30) scale(.8)" fill="none" stroke="{t["brand"]}" stroke-width="4" stroke-linecap="square" stroke-linejoin="round">')
 path('M14 3H6Q2 3 2 7V15Q2 19 6 19H14','none',f'stroke="{t["brand"]}"')
 path('M20 19L25 3H32L37 19M23 13H34','none',f'stroke="{t["brand"]}"')
 path('M57 3H45Q41 3 41 7Q41 11 45 11H51Q55 11 55 15Q55 19 51 19H41','none',f'stroke="{t["brand"]}"')
 path('M64 3V19','none',f'stroke="{t["brand"]}" stroke-width="5"')
 path('M78 3H84Q88 3 88 7V15Q88 19 84 19H78Q74 19 74 15V7Q74 3 78 3Z','none',f'stroke="{t["brand"]}"')
 add('</g>')
 text('fx-991CN X 中文版',35,68,8.8,t['brand'],400,'start','sans-serif')
 for i,ch in enumerate('CLASSWIZ'):text(ch,35+i*10.8,84,10.4,'#b23558' if t['id']=='black' else t['brand'],400,'start','sans-serif')
 rect(148,24,155,72,9,tint(t['edge'],-20))
 rect(151,27,149,66,6,'url(#solar)','stroke="#e9dbcc" stroke-opacity=".35" stroke-width=".8"')
 for x in (188,225,262):path(f'M{x} 29V91','none','stroke="#8c7770" stroke-opacity=".35" stroke-width=".6"')
 rect(309,39,12,47,1,'#a99d64')
 for j in range(17):
  x=310+j%3*3;y=40+j*2.5;rect(x,y,3,2,0,['#777d4a','#e4d496','#5b7770','#c7d7b0'][j%4], 'opacity=".8"')
 # A recessed LCD, shared across all four faceplates; the live raster is overlaid by Android.
 rect(26,109,292,126,8,tint(t['edge'],-22))
 path('M31 115L39 123H305L313 115Z','#3b4540','opacity=".65"')
 path('M31 115L39 123V220L31 230Z','#222d2a','opacity=".7"')
 path('M313 115L305 123V220L313 230Z','#d7ddd0','opacity=".65"')
 rect(35,119,274,108,5,'url(#glass)','stroke="#6f8278" stroke-width="1"')
 # Top round keys and four separate directional petals.
 for i in range(42,46):
  x,y,w,h=KEYS[i];rect(x-1,y,w+2,h+2,16,tint(t['edge'],-47));rect(x,y,w,h,15,'url(#round)','stroke="#f5f1e4" stroke-opacity=".45" stroke-width=".7"')
  if t['id']=='black':
   cx=x+w/2;cy=y+h/2
   for points,opacity in [(f'{cx},{cy} {x+4},{y+4} {x+27},{y+7}',.45),(f'{cx},{cy} {x+4},{y+27} {x+27},{y+23}',.25)]:add(f'<polygon points="{points}" fill="#ffffff" opacity="{opacity}"/>')
 text('SHIFT',44,273,7.8,'#c8aa4f',700)
 text('ALPHA',91,273,7.8,'#b83c5f',700)
 text('菜单',244,273,7.7,t['brand'])
 text('设置',263,273,7.7,'#c7a145')
 text('开机',301,273,7.7,t['brand'])
 for pts in DPAD:
  d='M'+'L'.join(f'{x} {y}' for x,y in pts)+'Z'
  path(d,tint(t['edge'],-48),'stroke="#000000" stroke-opacity=".2" stroke-width="3.5" stroke-linejoin="round"')
  path(d,'url(#round)','stroke="#ffffff" stroke-opacity=".48" stroke-width="1" stroke-linejoin="round"')
 # Functional and numeric keys, each with a thin physical bezel rather than a large UI shadow.
 for i,(x,y,w,h) in enumerate(KEYS[:42]):
  typ='number' if i<20 else 'key'
  if i in (3,4):typ='ac'
  rect(x-1,y,w+2,h+2,4.7,tint(t['edge'],-47),'stroke="#ffffff" stroke-opacity=".15" stroke-width=".5"')
  rect(x,y,w,h,4.1,'url(#'+typ+')')
  path(f'M{x+4} {y+.8}H{x+w-4}','none','stroke="#ffffff" stroke-opacity=".45" stroke-width=".7"')
  path(f'M{x+4} {y+h-.6}H{x+w-4}','none','stroke="#000000" stroke-opacity=".17" stroke-width=".6"')
  ink=t['acink'] if i in (3,4) else '#202124' if t['id']=='black' and i<20 else t['ink']
  cx=x+w/2;cy=y+h/2
  if i==20:
   path(f'M{cx-8} {cy}H{cx+8}','none',f'stroke="{ink}" stroke-width="1.2"')
   rect(cx-4,cy-7,8,4,0,'none',f'stroke="{ink}" stroke-width=".9"');rect(cx-4,cy+3,8,4,0,'none',f'stroke="{ink}" stroke-width=".9"')
  elif i==21:path(f'M{cx-9} {cy}L{cx-6} {cy}L{cx-3} {cy+6}L{cx} {cy-7}H{cx+10}','none',f'stroke="{ink}" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"')
  elif i in (22,23):
   text('x',cx-3,cy+5,15,ink,font='math')
   if i==22:digit('2',cx+6,cy-5,ink,.36)
   else:rect(cx+3,cy-8,5,6,.3,'none',f'stroke="{ink}" stroke-width=".9"')
  elif i==28:
   text('x',cx-4,cy+5,15,ink,font='math');path(f'M{cx+3} {cy-6.5}H{cx+6}', 'none',f'stroke="{ink}" stroke-width=".9"');digit('1',cx+9,cy-5.5,ink,.31)
  elif i==41:text('x',cx,cy+5,16,ink,font='math')
  elif i==24:
   text('log',cx-4,cy+4,11.7,ink)
   rect(cx+4,cy+2,3.5,4,.2,'none',f'stroke="{ink}" stroke-width=".8"');rect(cx+10,cy-5,4.5,7,.2,'none',f'stroke="{ink}" stroke-width=".8"')
  elif i==40:
   # The actual key is a definite-integral template: limits on the left, argument on the right.
   path(f'M{cx-1} {cy-9}C{cx-5} {cy-11} {cx-6} {cy-7} {cx-6} {cy-2}L{cx-7} {cy+5}C{cx-7} {cy+10} {cx-10} {cy+10} {cx-12} {cy+8}','none',f'stroke="{ink}" stroke-width="1.8" stroke-linecap="round"')
   rect(cx-12,cy-11,3.5,3.5,.2,'none',f'stroke="{ink}" stroke-width="1"')
   rect(cx-16,cy+5,3.5,3.5,.2,'none',f'stroke="{ink}" stroke-width="1"')
   rect(cx+1.5,cy-3.5,7.5,7,.2,'none',f'stroke="{ink}" stroke-width="1.2"')
  elif i==17:
   arithmetic('×',cx-15,cy+1,ink,.5);digit('1',cx-4,cy+1,ink,.7);digit('0',cx+6,cy+1,ink,.7);text('x',cx+15,cy-5,7.4,ink)
  elif i<20 and MAIN[i].isdigit():digit(MAIN[i],cx,cy,ink)
  elif i<20 and MAIN[i] in ('+','−','×','÷','='):arithmetic(MAIN[i],cx,cy,ink)
  elif i==16:add(f'<circle cx="{cx}" cy="{cy+6}" r="1.55" fill="{ink}"/>')
  else:text(MAIN[i],cx,cy+(6 if i<20 else 4.2),17 if i in (3,4) else 15.2 if i==18 else 13,ink,500 if i<20 else 400)
  # The lettering is part of the faceplate, never a fake functional overlay.
  if SHIFT[i]:
   size=6.9 if i<5 else 7.4 if i<20 else 7
   if i in BASE:
    text(SHIFT[i],cx-8,y-4,size,'#bc942c');text(BASE[i],cx+12,y-4,5.7,'#2696c8')
   else:text(SHIFT[i],cx-(5 if ALPHA[i] else 0),y-4,size,'#bc942c')
  if ALPHA[i]:text(ALPHA[i],x+w-1,y-4,6.7,'#c93a5c')
 add('</svg>')
 return '\n'.join(out)
for theme in THEMES:(DEST/(theme['id']+'.svg')).write_text(theme_svg(theme))
java=ROOT/'app/src/main/java/org/fx991cnx/emulator/SkinGeometry.java'
java.write_text('''// Generated by tools/generate_skins.py. GPL-3.0.
package org.fx991cnx.emulator;
import android.graphics.RectF;
final class SkinGeometry {
 static final float WIDTH=344f, HEIGHT=736f;
 static final RectF LCD=new RectF(38,122,306,224);
 static final RectF[] KEYS={
'''+',\n'.join(f'  new RectF({x},{y},{x+w},{y+h})' for x,y,w,h in KEYS)+'''\n };
 static final float[][] DPAD={
'''+',\n'.join('  new float[]{'+','.join(str(v)+'f' for pt in pts for v in pt)+'}' for pts in DPAD)+'''\n };
 static boolean inside(float[] p,float x,float y){boolean hit=false;for(int i=0,j=p.length-2;i<p.length;j=i,i+=2){float xi=p[i],yi=p[i+1],xj=p[j],yj=p[j+1];if((yi>y)!=(yj>y)&&x<(xj-xi)*(y-yi)/(yj-yi)+xi)hit=!hit;}return hit;}
 private SkinGeometry(){}
}
''')
print('Generated four native SVG faceplates and shared 50-key geometry.')
