"""Create neutral, clearly named demo surfaces; no reference photos are altered."""
from pathlib import Path
import struct, zlib, json, random

root = Path(__file__).resolve().parents[1] / 'app/src/main/assets/scenes'
root.mkdir(parents=True, exist_ok=True)
for name, label, base in [('warm', 'พื้นครีม · ฉากสาธิต', (214, 207, 191)), ('cool', 'พื้นเทา · ฉากสาธิต', (176, 187, 190))]:
    width, height = 1800, 2400
    rng = random.Random(42)
    rows = bytearray()
    for y in range(height):
        rows.append(0)
        for x in range(width):
            grain = rng.randint(-2, 2)
            light = round(8 * (1 - x / width) - 5 * y / height)
            rows.extend(max(0, min(255, c + grain + light)) for c in base)
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', width, height, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    (root / f'{name}.png').write_bytes(png)
    data = dict(id=name, name=label, category='plain', image=f'{name}.png', paperPlacement=dict(centerX=.5, centerY=.5, widthMin=.32, widthMax=.40, rotationMin=-3, rotationMax=3, offsetX=.03, offsetY=.03))
    (root / f'{name}.json').write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
