"""Regression checks for real handheld parents, shield state routing and UV coverage.

Independent of the asset generator: UV rectangles below are derived from the
vanilla 1.20.1 ShieldModel plate(12,22,1) / handle(2,6,6) geometry.
Run: python -B scripts/verify_rendering.py
"""
from pathlib import Path
import json
import re
import sys
import tempfile
import shutil

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT.parent / 'gas_asset_build'))
import pngkit

ASSETS = ROOT / 'src/main/resources/assets/gas'

def models_check(assets):
    models = assets / 'models/item'
    for name in ('sword', 'pickaxe', 'axe', 'shovel', 'hoe'):
        model = json.loads((models / f'golden_apple_{name}.json').read_text())
        assert model['parent'] == 'minecraft:item/handheld', f'{name}: not a handheld model'
        assert model['textures']['layer0'] == f'gas:item/golden_apple_{name}'
    idle = json.loads((models / 'golden_apple_shield.json').read_text())
    block = json.loads((models / 'blocking/golden_apple_shield_blocking.json').read_text())
    assert idle['parent'] == 'minecraft:item/shield'
    assert block['parent'] == 'minecraft:item/shield_blocking'
    assert idle['overrides'] == [{'predicate': {'blocking': 1},
                                'model': 'gas:item/blocking/golden_apple_shield_blocking'}]
    for model in (idle, block):
        assert model['textures']['shield'] == 'gas:entity/shield/golden_apple_shield'
        assert model['textures']['particle'] == 'gas:item/golden_apple_shield'
        assert not model.get('display'), 'Use canonical vanilla left/right-hand transforms'

def shield_uv_check(pixels):
    assert len(pixels) == 64 and all(len(row) == 64 for row in pixels)
    # Independent geometry-derived unfold; north face is the outside/front.
    boxes = [(0,0,12,22,1), (26,0,2,6,6)]
    occupied = set()
    for u,v,w,h,d in boxes:
        rects = [(u+d,v,w,d), (u+d+w,v,w,d),
                 (u,v+d,d,h), (u+d,v+d,w,h),
                 (u+d+w,v+d,d,h), (u+2*d+w,v+d,w,h)]
        for x,y,rw,rh in rects:
            for yy in range(y,y+rh):
                for xx in range(x,x+rw):
                    assert (xx,yy) not in occupied, 'UV islands overlap'
                    occupied.add((xx,yy))
                    assert pixels[yy][xx][3] == 255, f'Transparent model texel {xx},{yy}'
    for y,row in enumerate(pixels):
        for x,px in enumerate(row):
            if (x,y) not in occupied:
                assert px[3] == 0, f'Color outside ShieldModel UV: {x},{y}'
    wood = {(122,78,40), (84,51,27)}
    assert sum(pixels[y][x][:3] in wood for y in range(1,23) for x in range(14,26)) > 100, 'Wood back missing'
    assert sum(pixels[y][x][:3] in wood for y in range(6,12) for x in range(26,42)) > 40, 'Handle missing'
    assert any(pixels[y][x][:3] == (95,175,62) for y in range(1,23) for x in range(1,13)), 'Apple leaf missing'

def client_wiring_check():
    src = (ROOT / 'src/main/java/com/gas/mod/GoldenAppleClientSetup.java').read_text(encoding='utf-8')
    assert 'value = Dist.CLIENT' in src
    assert 'new ResourceLocation("blocking")' in src
    assert re.search(r'ItemProperties.register\(ModItems.GOLDEN_APPLE_SHIELD.get\(\), BLOCKING,', src)
    assert 'registerShieldBlockingProperty();' in src
    shield = src.split('private static void registerShieldBlockingProperty()',1)[1].split('private static void',1)[0]
    assert 'entity != null && entity.isUsingItem() && entity.getUseItem() == stack' in shield
    assert '1.0F : 0.0F' in shield
    renderer = (ROOT / 'src/main/java/com/gas/mod/GoldenAppleShieldRenderer.java').read_text(encoding='utf-8')
    assert 'new ShieldModel(' in renderer and 'ModelLayers.SHIELD' in renderer
    assert 'textures/entity/shield/golden_apple_shield.png' in renderer
    assert 'this.model.handle().render(' in renderer and 'this.model.plate().render(' in renderer
    assert 'getFoilBufferDirect(' in renderer

def must_fail(fn, description):
    try: fn()
    except AssertionError: return
    raise AssertionError('Regression check failed to detect ' + description)

def main():
    models_check(ASSETS)
    image = pngkit.read(str(ASSETS / 'textures/entity/shield/golden_apple_shield.png'))
    shield_uv_check(image)
    client_wiring_check()
    print('PASS: handheld parents; both-hand vanilla shield poses; blocking property wiring; full plate/back/handle UV coverage')
    # Prove the checks reject the reported regressions, without touching sources.
    with tempfile.TemporaryDirectory(prefix='gas-render-check-') as directory:
        assets = Path(directory)
        shutil.copytree(ASSETS / 'models', assets / 'models')
        p = assets / 'models/item/golden_apple_pickaxe.json'
        model = json.loads(p.read_text()); model['parent'] = 'minecraft:item/generated'
        p.write_text(json.dumps(model))
        must_fail(lambda: models_check(assets), 'ordinary-item tool grip')
    hole = [row[:] for row in image]; hole[10][5] = (0,0,0,0)
    must_fail(lambda: shield_uv_check(hole), 'missing shield-front texel')
    icon = pngkit.read(str(ASSETS / 'textures/item/golden_apple_shield.png'))
    old = [[icon[y//4][x//4] for x in range(64)] for y in range(64)]
    must_fail(lambda: shield_uv_check(old), 'old upscaled inventory-icon texture')
    print('PASS: mutation checks reject wrong tool parent, UV holes and the old 4x shield icon')

if __name__ == '__main__': main()
