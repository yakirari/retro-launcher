/** Export current packaged Stacked images only, losslessly; no research assets. */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const sharp=createRequire(import.meta.url)(process.env.SHARP_MODULE??'/tmp/retro-art-handoff-tools/node_modules/sharp');
const read=async p=>JSON.parse(await fs.readFile(path.join(root,p),'utf8'));
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const packaged='app/src/main/assets/arcade_presentations';
const index=await read(`${packaged}/index.json`);
const aliases=(await read('app/src/main/assets/arcade-metadata/cabinet-art.json')).mappings;
const jobs=(await read('artwork/arcade-presentations-review/plan.json')).games;
const out=path.join(root,'artwork/current-arcade-fronts');
await fs.mkdir(out); // Refuse overwrite: retain any user changes.
const rows=[];let sourceBytes=0,outputBytes=0;
for(const m of index.mappings){
  assert.match(m.front,/^front\/[a-z0-9_]+\.png$/);
  const bytes=await fs.readFile(path.join(root,packaged,m.front));
  assert.equal(sha(bytes),m.frontSha256);
  const image=await sharp(bytes).webp({lossless:true,effort:5}).toBuffer();
  const original=await sharp(bytes).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  const exported=await sharp(image).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  assert.equal(original.info.width,exported.info.width);
  assert.equal(original.info.height,exported.info.height);
  assert.deepEqual(original.data,exported.data);
  const file=m.asset.replace(/\.png$/,'.webp');
  await fs.writeFile(path.join(out,file),image);
  rows.push({file,title:jobs.find(j=>j.asset===m.asset)?.title??m.asset,asset:m.asset,
    roms:aliases.filter(a=>a.asset===m.asset).map(a=>a.rom),
    source:`${packaged}/${m.front}`,sourceSha256:m.frontSha256,sha256:sha(image),
    width:original.info.width,height:original.info.height,representation:m.representation});
  sourceBytes+=bytes.length;outputBytes+=image.length;
}
assert(aliases.every(a=>rows.some(r=>r.asset===a.asset)),'Missing ROM coverage');
await fs.writeFile(path.join(out,'index.json'),JSON.stringify({description:'Current packaged Stacked fronts, pixel-identical lossless WebP copies; not the rejected realism previews',images:rows},null,2)+'\n');
const escape=s=>s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
await fs.writeFile(path.join(out,'gallery.html'),`<!doctype html><meta charset="utf-8"><title>Current arcade fronts</title><style>body{background:#161a21;color:white;font:15px system-ui;padding:24px}main{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:16px}figure{margin:0;background:#242b36;padding:10px}img{width:100%;height:350px;object-fit:contain}figcaption{margin-top:8px}</style><h1>Current Stacked arcade fronts</h1><p>${rows.length} unique current images. These are the existing illustrative fronts, not the experimental realism studies. Lossless WebP: same dimensions and decoded pixels as packaged PNGs.</p><main>${rows.map(r=>`<figure><a href="${r.file}"><img loading="lazy" src="${r.file}" alt="${escape(r.title)}"></a><figcaption>${escape(r.title)}<br><small>${escape(r.file)}</small></figcaption></figure>`).join('')}</main>`);
await fs.writeFile(path.join(out,'README.md'),`# Current arcade fronts only\n\n${rows.length} unique images covering ${aliases.length} ROM mappings, selected from the app's packaged front index for Stacked. No research parts, photographs, lit variants or experimental realism composites. These are the currently packaged illustrative fronts, NOT newly improved or authenticated cabinets.\n\nImages are lossless WebP copies at original dimensions. Every decoded RGBA pixel was compared to the packaged PNG and passed. Transparent pixels and alpha retained. Output filename identifies the original cabinet asset; shared clone mappings are listed in index.json rather than duplicating images. Open gallery.html for a visual overview.\n\nPackaged PNG total: ${(sourceBytes/1048576).toFixed(1)} MiB. Exported images: ${(outputBytes/1048576).toFixed(1)} MiB. Provenance paths and SHA-256 hashes are in index.json. No app assets changed.\n`);
console.log(JSON.stringify({images:rows.length,romMappings:aliases.length,sourceMiB:sourceBytes/1048576,exportMiB:outputBytes/1048576,pixelVerification:'PASS every RGBA pixel'},null,2));