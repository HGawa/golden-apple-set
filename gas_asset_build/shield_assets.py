"""Native pixel/model source for ShieldModel (MC 1.20.1), not an enlarged icon.

plate: texOffs(0,0), box(-6,-11,-2, 12,22,1)
handle: texOffs(26,0), box(-1,-3,-1, 2,6,6)
Coordinates verified against the locally mapped ShieldModel.createLayer bytecode.
"""
from pathlib import Path
import json
import pngkit

PALETTE = {
    'L': (252,233,122,255), 'G': (246,201,69,255), 'D': (168,123,27,255),
    '*': (255,255,255,255), 'u': (124,200,91,255), 'V': (78,155,60,255),
    'v': (47,107,39,255), 'W': (122,78,40,255), 'w': (84,51,27,255),
    'l': (95,175,62,255), 'R': (226,59,50,255), 'Y': (248,211,74,255),
}
# Rectangles are half-open, matching the six ModelPart.Cube faces exactly.
UV = {
    'plate_top': (1,0,12,1), 'plate_bottom': (13,0,12,1),
    'plate_left': (0,1,1,22), 'plate_front': (1,1,12,22),
    'plate_right': (13,1,1,22), 'plate_back': (14,1,12,22),
    'handle_top': (32,0,2,6), 'handle_bottom': (34,0,2,6),
    'handle_left': (26,6,6,6), 'handle_front': (32,6,2,6),
    'handle_right': (34,6,6,6), 'handle_back': (40,6,2,6),
}
APPLE = [
    '...lu..',
    '...W...',
    '.DD.DD.',
    'DGGLGGD',
    'DGLLGGD',
    'DGGYGGD',
    '.DGGGD.',
    '..DDD..',
]
LEAF = ['.uV','uVV','Vv.']

def build_texture():
    image = [[(0,0,0,0) for _ in range(64)] for _ in range(64)]
    for name, (ox,oy,w,h) in UV.items():
        for y in range(h):
            for x in range(w):
                if name == 'plate_front':
                    edge = min(x,y,w-1-x,h-1-y)
                    color = 'D' if edge == 0 else ('L' if edge == 1 else 'G')
                    if edge > 1 and x == 2: color = 'L'
                elif name == 'plate_back':
                    color = 'D' if min(x,y,w-1-x,h-1-y) == 0 else ('w' if x % 3 == 0 else 'W')
                    if y in (3,18): color = 'G'
                elif name.startswith('handle_'):
                    color = 'D' if y in (0,h-1) else ('w' if x % 3 == 0 else 'W')
                else:
                    color = 'L' if name == 'plate_top' else 'D'
                image[oy+y][ox+x] = PALETTE[color]
    def stamp(x,y,rows):
        for dy,row in enumerate(rows):
            for dx,ch in enumerate(row):
                if ch != '.': image[y+dy][x+dx] = PALETTE[ch]
    stamp(3,8,APPLE)
    stamp(2,3,LEAF)
    stamp(9,3,[row[::-1] for row in LEAF])
    # Small gold rivets on the wood back, away from the handle.
    for x in (16,23):
        for y in (4,19): image[y][x] = PALETTE['L']
    return image

def models():
    textures = {'particle': 'gas:item/golden_apple_shield',
                'shield': 'gas:entity/shield/golden_apple_shield'}
    return {
        'golden_apple_shield.json': {
            'parent': 'minecraft:item/shield', 'textures': textures,
            'overrides': [{'predicate': {'blocking': 1},
                           'model': 'gas:item/blocking/golden_apple_shield_blocking'}],
        },
        'blocking/golden_apple_shield_blocking.json': {
            'parent': 'minecraft:item/shield_blocking', 'textures': textures,
        },
    }

def write(assets):
    assets = Path(assets)
    dest = assets / 'textures/entity/shield/golden_apple_shield.png'
    dest.parent.mkdir(parents=True, exist_ok=True)
    pngkit.write(str(dest), build_texture())
    for name,model in models().items():
        dest = assets / 'models/item' / name
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')

if __name__ == '__main__':
    write(Path(__file__).resolve().parent.parent / 'gas_mod/src/main/resources/assets/gas')
    print('Shield texture and idle/blocking models rebuilt for vanilla ShieldModel UV.')
