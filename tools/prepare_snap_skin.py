"""Prepare selected, already decoded owned-game UI art; never reads a device/save."""
import argparse
import json
import struct
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def control_symbols(font_path, target):
    """Center original game glyph ink in a square, independent of font bearings."""
    from fontTools.ttLib import TTFont
    from fontTools.pens.boundsPen import BoundsPen
    from fontTools.pens.svgPathPen import SVGPathPen
    font = TTFont(font_path)
    glyphs, cmap = font.getGlyphSet(), font.getBestCmap()
    names = {'close': 0xe14c, 'plus': 0xe0b5, 'minus': 0xe0b6,
             'stick-well': 0xe0c0, 'stick-left': 0xe101, 'stick-right': 0xe102}
    for name, code in names.items():
        glyph = glyphs[cmap[code]]
        bounds = BoundsPen(glyphs)
        glyph.draw(bounds)
        x0, y0, x1, y1 = bounds.bounds
        side = max(x1 - x0, y1 - y0) * 1.12
        x, y = (x0 + x1 - side) / 2, -(y0 + y1 + side) / 2
        pen = SVGPathPen(glyphs)
        glyph.draw(pen)
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{x:g} {y:g} {side:g} {side:g}">'
               f'<path fill="white" transform="scale(1 -1)" d="{pen.getCommands()}"/></svg>\n')
        (target / f'control-{name}.svg').write_text(svg)


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
    font = ROOT / 'app/src/main/assets/game-art/fonts/snap-controls.ttf'
    font.write_bytes(decoded)
    control_symbols(font, target)
    print(f'Prepared {len(selected)} original UI textures, native controller font and six centered control SVGs')


if __name__ == '__main__':
    main()
