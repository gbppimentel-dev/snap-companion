#!/usr/bin/env python3
"""Generate the authored Eden Duo UI. Includes no game artwork or game-derived geometry."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BUILD='DDB900FF04A912ED42430508389853CFAF1ACCCE000000000000000000000000'
PAPER='#FFF4F2E5'; INK='#FF254C4D'; TEAL='#FF246E72'; ORANGE='#FFDF893C'

def rect(box,bg,**kw): return dict(type='rect',rect=box,bg=bg,color='#00000000',frame=0,**kw)
def label(box,text=None,scale=4,**kw):
    w=dict(type='label',rect=box,text_scale=scale,color=INK,wrap_width=box[2],max_lines=1,fit_text=True,text_min_scale=2)
    w.update(kw)
    if text is not None:w['text']=text
    return w
def button(box,text,action,**kw): return dict(type='button',rect=box,text=text,on_tap=action,text_scale=4,
    color='#FFFFFFFF',bg=TEAL,align='center',pill=True,border=0,text_center_h=box[3],**kw)
def header():
    return [rect([0,0,1240,92],TEAL),label([28,22,780,52],'Snap Companion Next',scale=7,color='#FFFFFFFF'),
            label([960,24,80,40],bind_text='scn.connection_label',scale=3,color='#FFE2F0DD',align='right'),
            dict(type='value',rect=[1060,24,60,40],bind='@clock.hour',pad=2,text_scale=4,color='#FFFFFFFF'),
            label([1116,24,12,40],':',color='#FFFFFFFF'),
            dict(type='value',rect=[1130,24,60,40],bind='@clock.minute',pad=2,text_scale=4,color='#FFFFFFFF')]
def map_widget(box,id,interactive):
    return dict(type='map',id=id,rect=box,area='course_canvas',area_label=False,bg='#FFE3E9D6',
        need_bind='scn.map_valid',pan_zoom=interactive,min_zoom=1,max_zoom=5,
        overlays=[dict(src_bind='scn.map_src',x0=0,y0=-2160,x1=3840,y1=0,src_hold=False),
                  dict(src_bind='scn.route_src',x0=0,y0=-2160,x1=3840,y1=0,show_bind='ui.routes',src_hold=False),
                  dict(src_bind='scn.pokemon_src',x0=0,y0=-2160,x1=3840,y1=0,show_bind='ui.pokemon',src_hold=False)])
def manifest():
    home=header()+[
        rect([24,116,1192,144],'#FFFFFFFF'),rect([24,116,9,144],ORANGE),
        label([52,130,1136,64],bind_text='scn.course_name',scale=8),
        label([52,205,1136,38],bind_text='scn.course_detail',scale=4),
        rect([24,280,1192,670],'#FFE3E9D6'),map_widget([24,280,1192,670],'course_preview',False),
        label([130,575,980,50],bind_text='scn.status',scale=5,align='center',need_bind='!scn.map_valid'),
        button([960,870,224,56],'Expand Map','expand',need_bind='scn.map_valid'),
        rect([24,974,1192,76],'#FFFFFFFF'),
        label([48,994,920,32],'Development • Live course mapping pending',scale=3,color='#FF687D77'),
        dict(type='value',rect=[1052,992,140,36],bind='@perf.fps',suffix=' FPS',text_scale=4,color=TEAL)]
    expanded=header()+[
        button([24,112,120,56],'Back','home'),
        label([172,120,1000,48],bind_text='scn.course_name',scale=6),
        map_widget([24,188,1192,760],'course_map',True),
        button([24,974,246,64],'Pokémon','pokemon',need_bind='scn.pokemon_available'),
        button([292,974,246,64],'Routes','routes',need_bind='scn.route_available'),
        button([992,974,224,64],'Reset View','reset',need_bind='scn.map_valid')]
    return dict(format=1,title_id='0100F4300BF2C000',name='Snap Companion Next',version='0.1.0-dev.1',min_runtime=25,
        canvas_w=1240,canvas_h=1080,background=PAPER,requires_module=True,module_tick_hidden=False,
        module=dict(abi=1,build_ids=[BUILD],libraries={}),
        flags={'map.pokemon':1,'map.routes':1},
        derived=[dict(name='ui.pokemon',expr='scn.pokemon_available && @flag:map.pokemon'),
                 dict(name='ui.routes',expr='scn.route_available && @flag:map.routes')],
        map=dict(areas={'course_canvas':dict(min=[0,-2160],max=[3840,0],dynamic_markers=[
            dict(group='player',count=1,x='scn.map_x',y='scn.map_y',kind='scn.player_valid',hide_when_kind=0,
                 icon_src_bind='scn.player_pin_src',size=38,size_max=48)])}),
        actions=dict(expand=dict(kind='page',page='map',transition='fade',duration_ms=120,enabled_bind='scn.map_valid'),
                     home=dict(kind='page',page='home',transition='fade',duration_ms=120),
                     pokemon=dict(kind='flag',flag='map.pokemon',enabled_bind='scn.pokemon_available'),
                     routes=dict(kind='flag',flag='map.routes',enabled_bind='scn.route_available'),
                     reset=dict(kind='view_reset',view='course_map')),
        pages=[dict(id='home',title='Course',widgets=home),dict(id='map',title='Map',no_auto_leave=True,widgets=expanded)])
def main():
    out=ROOT/'package/dualscreen/manifest.json';out.write_text(json.dumps(manifest(),indent=2)+'\n')
    package=dict(format=1,type='dual-screen-mod',title_id='0100F4300BF2C000',name='Snap Companion Next',version='0.1.0-dev.1',min_runtime=25)
    (ROOT/'package/package.json').write_text(json.dumps(package,indent=2)+'\n')
if __name__=='__main__':main()
