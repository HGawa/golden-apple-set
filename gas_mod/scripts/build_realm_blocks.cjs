// Deterministic native 16x16 block palette/texel source; node scripts/build_realm_blocks.cjs [--check].
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const {decode, encode} = require('./pixel_png.cjs');
const root = path.resolve(__dirname, '..'), art = path.join(root, 'art/realm_blocks');
const out = path.join(root, 'src/main/resources/assets/gas/textures/block');
const check = process.argv.includes('--check');
const source = name => decode(path.join(art, 'vanilla', name + '.png'));
function ramp(img, low, high) {
  const lum = Array.from({length:256}, (_, i) => {
    const p=i*4; return img.data[p]*0.299+img.data[p+1]*0.587+img.data[p+2]*0.114;
  });
  const min=Math.min(...lum), max=Math.max(...lum);
  const data=Buffer.alloc(1024);
  lum.forEach((v,i) => { const t=(v-min)/(max-min); for(let c=0;c<3;c++) data[i*4+c]=Math.round(low[c]+(high[c]-low[c])*t); data[i*4+3]=255; });
  return {w:16,h:16,data};
}
// Match the original dirt grain, grass edge silhouette, stone speckles and cobble cracks.
const dirt = ramp(source('dirt'), [153,101,28], [242,204,103]);
const top = ramp(source('grass_block_top'), [186,137,32], [255,231,129]);
const side = {w:16,h:16,data:Buffer.from(dirt.data)};
const overlay=source('grass_block_side_overlay');
const turf=ramp(overlay,[178,127,28],[251,221,105]);
for(let p=0;p<1024;p+=4) if(overlay.data[p+3]) turf.data.copy(side.data,p,p,p+4);
const green=[[67,113,38],[92,143,46],[124,171,61]];
function vein(img, points) { points.forEach(([x,y],i)=> img.data.set(green[i%3],(y*16+x)*4)); }
// Discontinuous thin veins leave the golden grass surface dominant; edge exits tile cleanly.
vein(top,[[2,2],[3,3],[3,4],[4,5],[5,5],[4,6],[4,7],[5,8],[6,8],[10,11],[11,11],[12,12],[12,13],[13,14],[10,12]]);
const earthVeins=[[2,8],[3,9],[3,10],[4,11],[5,11],[4,12],[10,5],[11,6],[11,7],[12,8],[12,9],[13,10]];
vein(dirt,earthVeins); vein(side,earthVeins);
vein(side,[[6,0],[6,1],[7,2],[8,2]]);
const stone=ramp(source('stone'),[187,188,184],[246,247,241]);
const cobble=ramp(source('cobblestone'),[141,145,141],[246,247,241]);
const assets={golden_apple_grass_block_top:top,golden_apple_grass_block_side:side,golden_apple_dirt:dirt,golden_apple_stone:stone,golden_apple_cobblestone:cobble};
fs.mkdirSync(out,{recursive:true});
for(const [name,img] of Object.entries(assets)) {
  assert.equal(img.data.length,1024); for(let p=3;p<1024;p+=4)assert.equal(img.data[p],255);
  const file=path.join(out,name+'.png'), bytes=encode(img);
  if(check)assert.deepEqual(fs.readFileSync(file),bytes,name+' is out of sync'); else fs.writeFileSync(file,bytes);
  console.log((check?'CHECK ':'WRITE ')+name+' 16x16 opaque');
}
// Soil below the grass fringe must match the dirt block exactly.
for(let y=5;y<16;y++) for(let x=0;x<16;x++) {
  const p=(y*16+x)*4;
  if(!overlay.data[p+3]) assert.deepEqual(side.data.subarray(p,p+4),dirt.data.subarray(p,p+4));
}
// Nearest-neighbor native atlas: grass top, grass side, dirt, stone, cobble.
const atlas={w:5*192,h:192,data:Buffer.alloc(5*192*192*4)};
Object.values(assets).forEach((img,i)=>{for(let y=0;y<192;y++)for(let x=0;x<192;x++){
  const p=(Math.floor(y/12)*16+Math.floor(x/12))*4, q=(y*atlas.w+i*192+x)*4;
  img.data.copy(atlas.data,q,p,p+4);
}});
if(!check)fs.writeFileSync(path.join(art,'texture-atlas.png'),encode(atlas));
