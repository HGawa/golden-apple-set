"""Native 64x32 UV atlas for GoldenShoulderLayer's symmetrical cuboid model."""
from pathlib import Path
import sys
import pngkit

ROOT = Path(__file__).resolve().parent.parent
TARGET = ROOT / 'gas_mod/src/main/resources/assets/gas/textures/entity/golden_apple_farmer/shoulders.png'
COLORS = {
    'gold': (246, 201, 69, 255), 'light': (252, 233, 122, 255),
    'edge': (168, 123, 27, 255), 'green': (78, 155, 60, 255),
    'leaf': (124, 200, 91, 255), 'shade': (47, 107, 39, 255),
}
# texOffs(u,v), box width, height, depth. Both shoulders share the same atlas.
BOXES = {'cap': (0, 0, 5, 2, 8), 'front': (0, 12, 5, 4, 1),
         'outer': (28, 0, 1, 5, 8), 'back': (0, 20, 5, 8, 1), 'tip': (16, 20, 3, 2, 1)}


def faces(u, v, w, h, d):
    return {'top': (u+d, v, w, d), 'bottom': (u+d+w, v, w, d),
            'left': (u, v+d, d, h), 'front': (u+d, v+d, w, h),
            'right': (u+d+w, v+d, d, h), 'back': (u+2*d+w, v+d, w, h)}


def build():
    pixels = [[(0, 0, 0, 0) for _ in range(64)] for _ in range(32)]
    occupied = set()
    for part, cube in BOXES.items():
        for face, (u, v, w, h) in faces(*cube).items():
            for y in range(h):
                for x in range(w):
                    assert (u+x, v+y) not in occupied, 'overlapping UV islands'
                    assert 0 <= u+x < 64 and 0 <= v+y < 32
                    occupied.add((u+x, v+y))
                    edge = min(x, y, w-1-x, h-1-y)
                    if part == 'cap':
                        color = 'light' if y == 0 else ('edge' if edge == 0 else 'gold')
                        # Two small green leaves in the gold shoulder top.
                        if face == 'top' and (x, y) in {(2, 3), (1, 4), (2, 4), (2, 5)}:
                            color = 'leaf' if x == 1 else 'green'
                    else:
                        color = 'gold' if edge == 0 else ('green' if x % 3 else 'shade')
                        if y == h-1: color = 'light'
                        # Short gold vein on the larger green back panels.
                        if part == 'back' and face in ('front', 'back') and x == 2 and y in (3, 4, 5):
                            color = 'leaf'
                    pixels[v+y][u+x] = COLORS[color]
    return pixels


if __name__ == '__main__':
    expected = build()
    if '--check' in sys.argv:
        assert pngkit.read(str(TARGET)) == expected, 'shoulder texture differs from native UV source'
        print('PASS: 64x32 shoulder atlas; five cuboids, six opaque faces each; non-overlapping in-bounds UV; gold/green palette')
    else:
        TARGET.parent.mkdir(parents=True, exist_ok=True)
        pngkit.write(str(TARGET), expected)
        print(f'Wrote {TARGET}')
