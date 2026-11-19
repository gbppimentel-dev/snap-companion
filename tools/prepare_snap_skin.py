"""Prepare selected, already decoded owned-game UI art; never reads a device/save."""
import argparse
import json
import struct
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--owned-root', type=Path, required=True)
    args = parser.parse_args()
    selected = json.loads((ROOT / 'tools/data/snap-skin-assets.json').read_text())
    target = ROOT / 'app/src/main/assets/game-art/skin'
    target.mkdir(exist_ok=True)
    for name, source in selected.items():
        pack, number = source.split('/')
        image = Image.open(args.owned_root / 'converted-ui' / pack / f'{pack}-{number}.png').convert('RGBA')
        if name == 'paper-texture':
            image.thumbnail((512, 288), Image.Resampling.LANCZOS)
        image.save(target / f'{name}.webp', lossless=True, method=6)
    # Native controller glyphs: exact A/B/X/Y, shoulders, triggers, D-pad,
    # plus/minus and stick symbols used by the game, with vector outlines.
    source = next((args.owned_root / 'decoded-ui').rglob('nintendo_ext_LE_003.bfttf'))
    raw = source.read_bytes()
    key = struct.pack('<I', struct.unpack_from('<I', raw)[0] ^ 0x18029a7f)
    decoded = bytes(c ^ key[i % 4] for i, c in enumerate(raw[8:]))
    if decoded[:4] != b'\x00\x01\x00\x00':
        raise ValueError('Unexpected owned controller font format')
    (ROOT / 'app/src/main/assets/game-art/fonts/snap-controls.ttf').write_bytes(decoded)
    print(f'Prepared {len(selected)} original UI textures and native controller font')


if __name__ == '__main__':
    main()
