// Native pixel-asset source for the vanilla-gold + leaf armor revision.
// node scripts/build_leaf_armor.cjs [--check] (Node built-ins only)
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const art = path.join(root, 'art/leaf_armor');
const textures = path.join(root, 'src/main/resources/assets/gas/textures');
const check = process.argv.includes('--check');

function decode(file) {
  const b = fs.readFileSync(file), parts = [];
  let w, h, type, palette, alpha;
  for (let p = 8; p < b.length;) {
    const n = b.readUInt32BE(p), tag = b.toString('ascii', p + 4, p + 8), d = b.subarray(p + 8, p + 8 + n);
    if (tag === 'IHDR') {
      w = d.readUInt32BE(0); h = d.readUInt32BE(4); type = d[9];
      assert.equal(d[8], 8); assert.equal(d[12], 0);
    }
    if (tag === 'PLTE') palette = d;
    if (tag === 'tRNS') alpha = d;
    if (tag === 'IDAT') parts.push(d);
    p += n + 12;
  }
  const channels = {2: 3, 3: 1, 6: 4}[type];
  assert.ok(channels, `Unsupported PNG type ${type}`);
  const raw = zlib.inflateSync(Buffer.concat(parts)), data = Buffer.alloc(w * h * 4);
  let previous = Buffer.alloc(w * channels), off = 0;
  for (let y = 0; y < h; y++) {
    const filter = raw[off++], row = Buffer.from(raw.subarray(off, off + w * channels));
    off += row.length;
    for (let x = 0; x < row.length; x++) {
      const a = x < channels ? 0 : row[x - channels], b = previous[x], c = x < channels ? 0 : previous[x - channels];
      const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
      const predictor = [0, a, b, Math.floor((a + b) / 2), pa <= pb && pa <= pc ? a : pb <= pc ? b : c][filter];
      assert.notEqual(predictor, undefined);
      row[x] = (row[x] + predictor) & 255;
    }
    for (let x = 0; x < w; x++) {
      const i = (y * w + x) * 4, q = x * channels;
      const rgb = type === 3 ? palette.subarray(row[q] * 3, row[q] * 3 + 3) : row.subarray(q, q + 3);
      rgb.copy(data, i);
      data[i + 3] = type === 6 ? row[q + 3] : type === 3 ? (alpha?.[row[q]] ?? 255) : 255;
    }
    previous = row;
  }
  return {w, h, data};
}
function crc32(b) {
  let c = 0xffffffff;
  for (const v of b) { c ^= v; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ ((c & 1) ? 0xedb88320 : 0); }
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(name, b) {
  const out = Buffer.alloc(b.length + 12);
  out.writeUInt32BE(b.length); out.write(name, 4); b.copy(out, 8);
  out.writeUInt32BE(crc32(out.subarray(4, b.length + 8)), b.length + 8);
  return out;
}
function encode({w, h, data}) {
  const header = Buffer.alloc(13); header.writeUInt32BE(w); header.writeUInt32BE(h, 4); header[8] = 8; header[9] = 6;
  const rows = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) data.copy(rows, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4);
  return Buffer.concat([Buffer.from('89504e470d0a1a0a', 'hex'), chunk('IHDR', header), chunk('IDAT', zlib.deflateSync(rows, {level: 9})), chunk('IEND', Buffer.alloc(0))]);
}

const green = {v: [47, 107, 39], V: [78, 155, 60], u: [124, 200, 91]};
// Small edge accents: keep the centers of armor panels completely gold.
const small = ['.uV', 'Vv.'];
const edge = ['.u', 'uV', 'v.'];
const tiny = ['uV', 'v.'];
const mirror = rows => rows.map(row => [...row].reverse().join(''));
// Coordinates are native 64x32 armor UV texels or native 16x16 icon texels.
// Shared 64x32 arm/leg UVs are mirrored by vanilla's left-limb geometry.
const assets = [
  {src: 'gold_layer_1.png', dst: 'models/armor/golden_apple_layer_1.png', marks: [
    [8, 9, tiny], [14, 9, mirror(tiny)], // left/right helmet rim, clear forehead
    [0, 10, small], [21, 10, mirror(small)], // lower side rim, no crown/back panels
    [40, 20, edge], [44, 20, edge], [54, 20, mirror(edge)], // shoulder borders only
    [0, 26, small], [4, 26, tiny], [13, 26, mirror(small)], // boot cuff edge only
  ]},
  {src: 'gold_layer_2.png', dst: 'models/armor/golden_apple_layer_2.png', marks: [
    [0, 20, edge], // outer upper-leg seam; front/back thigh and waist stay gold
  ]},
  {src: 'golden_helmet.png', dst: 'item/golden_apple_helmet.png', marks: [[3, 8, tiny], [11, 8, mirror(tiny)]]},
  {src: 'golden_chestplate.png', dst: 'item/golden_apple_chestplate.png', marks: [[1, 2, edge], [13, 2, mirror(edge)]]},
  {src: 'golden_leggings.png', dst: 'item/golden_apple_leggings.png', marks: [[3, 5, edge], [11, 5, mirror(edge)]]},
  {src: 'golden_boots.png', dst: 'item/golden_apple_boots.png', marks: [[3, 4, tiny], [11, 4, mirror(tiny)]]},
];
for (const asset of assets) {
  const base = decode(path.join(art, 'vanilla', asset.src));
  const out = {...base, data: Buffer.from(base.data)}, changed = new Set();
  for (const [ox, oy, rows] of asset.marks) {
    rows.forEach((row, y) => [...row].forEach((letter, x) => {
      if (letter === '.') return;
      const xx = ox + x, yy = oy + y, p = (yy * out.w + xx) * 4;
      assert.ok(xx >= 0 && xx < out.w && yy >= 0 && yy < out.h);
      assert.equal(base.data[p + 3], 255, `${asset.src}: decoration crossed transparent UV at ${xx},${yy}`);
      out.data.set(green[letter], p); changed.add(p);
    }));
  }
  let opaque = 0;
  for (let p = 0; p < out.data.length; p += 4) {
    assert.equal(out.data[p + 3], base.data[p + 3], 'Original alpha/silhouette must be preserved');
    if (base.data[p + 3]) opaque++;
    if (!changed.has(p)) assert.deepEqual(out.data.subarray(p, p + 4), base.data.subarray(p, p + 4));
  }
  assert.ok(changed.size / opaque < 0.10, 'Edge leaves must remain restrained');
  const dest = path.join(textures, asset.dst), png = encode(out);
  if (check) {
    const actual = decode(dest);
    assert.equal(actual.w, out.w); assert.equal(actual.h, out.h);
    assert.deepEqual(actual.data, out.data, `Rebuild required: ${dest}`);
  } else { fs.mkdirSync(path.dirname(dest), {recursive: true}); fs.writeFileSync(dest, png); }
  console.log(`${check ? 'CHECK' : 'WRITE'} ${asset.dst}: ${changed.size}/${opaque} leaf texels; alpha and all other gold pixels unchanged`);
}
// ArmorItem + GoldenAppleArmorMaterial already bind to vanilla humanoid armor
// geometry and gas:textures/models/armor/golden_apple_layer_{1,2}.png.
for (const name of ['helmet', 'chestplate', 'leggings', 'boots']) {
  const file = path.join(root, `src/main/resources/assets/gas/models/item/golden_apple_${name}.json`);
  const model = {parent: 'minecraft:item/generated', textures: {layer0: `gas:item/golden_apple_${name}`}};
  if (check) assert.deepEqual(JSON.parse(fs.readFileSync(file, 'utf8')), model);
  else fs.writeFileSync(file, JSON.stringify(model, null, 2) + '\n');
}
if (!check) {
  const paths = {
    layer1: path.join(textures, 'models/armor/golden_apple_layer_1.png'),
    layer2: path.join(textures, 'models/armor/golden_apple_layer_2.png'),
    vanilla1: path.join(art, 'vanilla/gold_layer_1.png'),
    vanilla2: path.join(art, 'vanilla/gold_layer_2.png'),
    ...Object.fromEntries(['helmet', 'chestplate', 'leggings', 'boots'].map(n => [n, path.join(textures, `item/golden_apple_${n}.png`)])),
  };
  const data = Object.fromEntries(Object.entries(paths).map(([key, file]) => [key, 'data:image/png;base64,' + fs.readFileSync(file).toString('base64')]));
  const template = fs.readFileSync(path.join(art, 'preview.template.html'), 'utf8');
  fs.writeFileSync(path.join(art, 'preview.html'), template.replace('__ARMOR_TEXTURES__', JSON.stringify(data)));
  console.log('WRITE art/leaf_armor/preview.html (standalone, offline, actual asset preview)');
}
