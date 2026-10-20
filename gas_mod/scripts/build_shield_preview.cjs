// Offline preview of the actual ShieldModel geometry and packaged UV texture.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const assets = path.join(root, 'src/main/resources/assets/gas');
const dir = path.join(root, 'art/shield');
fs.mkdirSync(dir, {recursive:true});
const png = name => 'data:image/png;base64,' + fs.readFileSync(path.join(assets,'textures',name)).toString('base64');
// Reuse the project's orthographic texel renderer, without any armor geometry.
const armorTemplate = fs.readFileSync(path.join(root,'art/leaf_armor/preview.template.html'),'utf8');
const renderer = armorTemplate.slice(armorTemplate.indexOf('function render(canvas,yaw)'),armorTemplate.indexOf('const views='));
const icons = ['pickaxe','axe','shovel','hoe'].map((n,i)=>`<div class="tool"><img src="${png(`item/golden_apple_${n}.png`)}"><div>${['金苹果镐','金苹果斧','金苹果锹','金苹果锄'][i]}<small>原版工具手持模型</small></div></div>`).join('');
const html = `<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>金苹果盾 · 模型与材质</title>
<style>*{box-sizing:border-box}body{margin:0;background:#151a16;color:#eee9cf;font:15px/1.6 'Microsoft YaHei',sans-serif}main{max-width:1160px;margin:auto;padding:30px}h1{margin:0;font-size:27px}p{color:#aab09b;margin:8px 0 22px}.layout{display:grid;grid-template-columns:1fr 1fr 260px;gap:16px}.panel,.tool{background:#21291e;border:1px solid #3e4934;border-radius:8px}.title{padding:14px 18px;border-bottom:1px solid #3e4934}canvas{width:100%;height:440px;display:block}.aside{padding:18px}.aside img{width:220px;image-rendering:pixelated;background:repeating-conic-gradient(#283026 0% 25%,#323a30 0% 50%) 0 0/12px 12px}.aside p{font-size:13px}.tools{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-top:16px}.tool{display:flex;align-items:center;gap:16px;padding:16px}.tool img{width:64px;height:64px;image-rendering:pixelated}small{display:block;font-size:11px;color:#aab09b}footer{margin-top:18px;color:#95a18b;font-size:12px}label{font-size:12px;letter-spacing:2px;color:#c5d796}</style>
<main><label>GOLDEN APPLE SET / 1.1.4</label><h1>金苹果盾 · 盾面、盾背与握柄</h1><p>金色包边、苹果徽记与少量叶片；背面为木板，握柄独立使用木纹贴图。</p>
<div class="layout"><section class="panel"><div class="title">盾面 · 原版立体盾模型</div><canvas id="front" width="440" height="480"></canvas></section><section class="panel"><div class="title">盾背 · 木板与立体握柄</div><canvas id="back" width="440" height="480"></canvas></section><section class="panel aside"><div>64 × 64 · 专用 UV 贴图</div><img src="${png('entity/shield/golden_apple_shield.png')}"><p>12 个模型面均有对应纹理：<br>盾面 / 背面 / 四边<br>握柄的六个面</p><p>待机与格挡模型继承原版盾姿态。<br>主手、副手分别使用原版变换。</p></section></div><div class="tools">${icons}</div><footer>由项目实际贴图与原版 ShieldModel 尺寸绘制的资源预览 · 非游戏截图</footer></main>
<script>
let texture,zoom=14;
function scene(){return [
 {x:0,y:8,z:1.5,w:12,h:22,d:1,u:0,v:0,inflate:0,texture},
 {x:0,y:8,z:-2,w:2,h:6,d:6,u:26,v:0,inflate:0,texture}
]}
${renderer}
const im=new Image();im.onload=()=>{const c=document.createElement('canvas');c.width=64;c.height=64;const g=c.getContext('2d');g.drawImage(im,0,0);texture={w:64,h:64,data:g.getImageData(0,0,64,64).data};render(document.querySelector('#front'),-.45);render(document.querySelector('#back'),Math.PI+.55);window.previewReady=true};im.src=${JSON.stringify(png('entity/shield/golden_apple_shield.png'))};
</script></html>`;
fs.writeFileSync(path.join(dir,'preview.html'),html);
console.log('Created art/shield/preview.html');
