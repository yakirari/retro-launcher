#!/usr/bin/env node
/** Review cache only: pinned public sources; no writes to runtime artwork. */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
import {recipes} from '../artwork/arcade-presentations-review/realism-batch.mjs';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const cache=path.join(root,'artwork/arcade-presentations-review/downloads');
const args=new Set(process.argv.slice(2));
if([...args].some(arg=>arg!=='--download'))throw new Error('Only --download is supported; default is offline verification.');
fs.mkdirSync(cache,{recursive:true});
for(const recipe of recipes.filter(recipe=>!recipe.photo)){
  const target=path.join(cache,`${recipe.overlay}-verticalarcade.png`);
  if(!fs.existsSync(target)&&args.has('--download')){
    const archive=path.join(cache,`${recipe.overlay}-verticalarcade.zip`);
    execFileSync('curl',['-L','--fail','--max-time','90','--max-filesize','60000000',`https://verticalarcade.xyz/${recipe.download}`,'-o',archive],{stdio:'inherit'});
    const png=execFileSync('unzip',['-p',archive,recipe.entry],{maxBuffer:40000000});
    if(crypto.createHash('sha256').update(png).digest('hex')!==recipe.sha256)throw new Error(`Downloaded source changed: ${recipe.overlay}`);
    fs.writeFileSync(target,png);
  }
  const png=fs.readFileSync(target);
  if(crypto.createHash('sha256').update(png).digest('hex')!==recipe.sha256)throw new Error(`Source changed: ${recipe.overlay}`);
  if(png.subarray(0,8).toString('hex')!=='89504e470d0a1a0a'||png.readUInt32BE(16)!==2160||png.readUInt32BE(20)!==3840)throw new Error(`Invalid overlay: ${recipe.overlay}`);
  console.log(`PASS ${recipe.title}: pinned 2160×3840 source`);
}
const plan=JSON.parse(fs.readFileSync(path.join(root,'artwork/arcade-presentations-review/plan.json')));
for(const recipe of recipes){
  const job=plan.games.find(game=>game.asset===`${recipe.rom}.png`);
  if(!job)throw new Error(`Missing job: ${recipe.rom}`);
  const inputs=[[path.join(root,'artwork/full-cabinets',job.asset),job.originalSha256]];
  if(!recipe.photo)for(const kind of ['panel','gameplay'])inputs.push([path.join(cache,job[kind].file),job[kind].sha256]);
  for(const [file,expected] of inputs)if(crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')!==expected)throw new Error(`Input changed: ${file}`);
  console.log(`PASS ${recipe.title}: original and required exact/same-title inputs verified`);
}
console.log('Review-only sources verified. No runtime assets modified. Open realism-batch.html through a local HTTP server to render.');