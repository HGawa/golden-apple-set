const fs = require('node:fs');
const zlib = require('node:zlib');
const assert = require('node:assert/strict');
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


module.exports = {decode, encode};
