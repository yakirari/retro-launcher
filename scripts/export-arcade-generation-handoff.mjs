/** Offline reference export only: never writes launcher artwork. Node 18+, sharp. */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
import {recipes} from '../artwork/arcade-presentations-review/realism-batch.mjs';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const base=path.join(root,'artwork/arcade-presentations-review');
const args=process.argv.slice(2);
if(args.some(a=>!['--verify'].includes(a)))throw Error('Only --verify is supported');
const out=path.join(root,'artwork/arcade-generation-handoff');
const read=async p=>JSON.parse(await fs.readFile(p,'utf8'));
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
const check=(v,m)=>{if(!v)throw Error(m);};
const safe=s=>{check(/^[a-z0-9_]+$/.test(s),`Unsafe cabinet ID ${s}`);return s;};
const plan=await read(path.join(base,'plan.json'));
const metadata=await read(path.join(root,'app/src/main/assets/arcade-metadata/metadata.json'));
const mappings=(await read(path.join(root,'app/src/main/assets/arcade-metadata/cabinet-art.json'))).mappings;
const references=(await read(path.join(root,'artwork/cabinet-references/cabinet-manifest.json'))).mappings;
const conversions=(await read(path.join(root,'artwork/cabinet-references/full-cabinet-review/conversions/plan.json'))).games;
const photos=(await read(path.join(root,'artwork/cabinet-references/full-cabinet-review/provenance.json'))).sources;
const parts=['marquee','control-panel','bezel','speakers','coin-door','body-side-kickplate','gameplay'];
const researched={
  ddragon:{notes:['Museum record describes Taito Y-pattern red/white/grey/black side art, decorative dragon bezel, two-player 8-way sticks and three buttons per player. Cabinet variants exist; do not combine variants blindly.','Existing cutout matches museum 47409; photographed title screen. Prior gameplay insertion leaves visible remnants and is NOT a reference for screen geometry.','The user reference shows a frontal black lower body, detailed dragon bezel and decorated control area. Exact source of that montage is unknown.'],links:['https://www.arcade-museum.com/Videogame/double-dragon'],extra:[['ddragon-museum-47409.jpg','Roger Coltrane, Museum of the Game; same cabinet as cutout; watermark retained','https://www.arcade-museum.com/images-game/47/double-dragon-47409.jpg']]},
  pow:{notes:['Inspected real source: angled complete cabinet with colourful bezel and yellow two-player deck, strong CRT glare and plain black lower body.','User montage has sunset/character kickplate absent from inspected photo. That lower art is NOT verified factory equipment. No accepted frontal replacement.'],links:['https://vintagearcade.net/shop/arcade-games/pow-prisoners-war/'],extra:[['pow-vintage-cabinet.jpg','Vintage Arcade Superstore; detailed angled cabinet; not selected as frontal output','https://vintagearcade.net/wp-content/uploads/2017/10/Pow-video-game.jpg']]},
  ffightu:{notes:['Complete museum cabinet photograph inspected; heavily side-on and worn. Vendor photograph is angled/reflective and crops lower body. Neither is an accepted frontal replacement.','User montage: Metro City bezel header, colourful deck, logo above a lower character-art coin-door surround. Exact configuration remains unverified.'],links:['https://www.arcade-museum.com/Videogame/final-fight'],extra:[['ffight-museum-48490.jpg','Museum of the Game; full side-on photo, watermark retained','https://www.arcade-museum.com/images-game/48/final-fight-48490.jpg']]},
  btoads:{notes:['Strongest new inspected candidate: complete near-frontal turquoise-trim cabinet, three-player deck, real coin door and illustrated kickplate. Seller describes new parts; factory originality is NOT established.','Different lower-art arrangement from user montage. Keep three-player control layout from this cabinet; do not replace it with a generic two-player deck. Title screen is not gameplay.'],links:['https://arcadesmarket.com/products/battletoads-arcade-game-coin-operated-heavy-duty-commercial-grade-new-parts'],extra:[['btoads-market-8850.jpg','Arcades Market; complete near-frontal cabinet with title screen; new-parts listing','https://arcadesmarket.com/cdn/shop/files/IMG_8850.jpg']]},
  goldnaxe:{notes:['Game-specific overlay and control-panel references available. Overlay is not a complete cabinet; lower body and exact hardware require further research.','User montage shows ornate dark bezel, illustrated deck, Golden Axe logo on black lower front. Do not substitute invented fantasy art.'],links:['https://www.verticalarcade.com/games/golden-axe'],extra:[]},
  punisher:{notes:['Game-specific overlay/control-panel references available. Two inspected Szabo product photographs show printed parts, not a complete cabinet.','User montage shows skull-themed bezel/deck and lower front. Exact source and factory status unverified; do not infer a complete original cabinet from a reproduction art kit.'],links:['https://www.szabosarcades.com/products/the-punisher-full-art-kit'],extra:[['punisher-szabo-1.jpg','Szabo reproduction art-kit panel/detail, NOT a full cabinet','https://www.szabosarcades.com/products/the-punisher-full-art-kit']]},
  avspu:{notes:['Overlay and same-title parent panel available; source relation recorded separately. Vendor gallery images inspected were flyers, NOT cabinet photographs.','User montage has LA POLICE trim and illustrated lower front; exact source and authenticity are unverified. Do not turn flyer layouts into invented cabinet parts.'],links:['https://www.verticalarcade.com/games/alien-vs-predator'],extra:[]},
  captcomm:{notes:['Existing cabinet photograph is angled, blue-bodied with yellow deck. Museum frontal image is only 128x320 with dark screen, unsuitable for detailed reconstruction.','User montage shows detailed character bezel, yellow deck and blue lower front; source configuration not verified. Do not flatten the entire angled cabinet.'],links:['https://www.arcade-museum.com/Videogame/captain-commando'],extra:[['captcomm-museum-24812.jpg','Museum of the Game; low-resolution frontal cabinet, watermark retained','https://www.arcade-museum.com/images-game/24/captain-commando-24812.jpg']]}
};

if(args.includes('--verify')){
  const index=await read(path.join(out,'index.json'));
  check(index.cabinets.length===plan.games.length,'Cabinet count mismatch');
  check(new Set(index.cabinets.map(c=>c.id)).size===plan.games.length,'Duplicate IDs');
  const expectedAssets=new Set(plan.games.map(c=>c.asset));
  let files=0;
  for(const c of index.cabinets){
    safe(c.id);check(expectedAssets.delete(c.asset),`Unexpected asset ${c.asset}`);
    const manifest=await read(path.join(out,c.id,'manifest.json'));
    for(const f of manifest.files){
      check(!path.isAbsolute(f.file)&&!f.file.split('/').includes('..'),'Unsafe path');
      check(hash(await fs.readFile(path.join(out,c.id,f.file)))===f.sha256,`Changed export ${c.id}/${f.file}`);
      if(f.sourcePath)check(hash(await fs.readFile(path.join(root,f.sourcePath)))===f.sourceSha256,`Changed input ${f.sourcePath}`);
      files++;
    }
    for(const p of parts)check(manifest.parts[p],`Missing part status ${c.id}/${p}`);
    check((await fs.readFile(path.join(out,c.id,'GENERATION-BRIEF.md'),'utf8')).includes(c.id),'Missing brief');
  }
  check(!expectedAssets.size,'Missing assets');
  const aliases=await read(path.join(out,'rom-to-folder.json'));
  check(JSON.stringify(aliases)===JSON.stringify(mappings.map(m=>({rom:m.rom,asset:m.asset,folder:m.asset.replace(/\.png$/,'')}))),'ROM alias coverage mismatch');
  console.log(`PASS ${index.cabinets.length} cabinet dossiers, ${aliases.length} ROM mappings, ${files} hashed files; original input hashes unchanged`);
  process.exit(0);
}
const require=createRequire(import.meta.url);
const sharp=require(process.env.SHARP_MODULE??'/tmp/retro-art-handoff-tools/node_modules/sharp');
try{await fs.access(out);throw Error(`Output already exists: ${out}. Preserve or move it before re-exporting.`);}catch(e){if(e.code!=='ENOENT')throw e;}
await fs.mkdir(out,{recursive:true});
const writeJSON=async(p,v)=>fs.writeFile(p,JSON.stringify(v,null,2)+'\n');
const index={schemaVersion:1,purpose:'Offline image-generation references; not runtime artwork or a claim of completed individual research',cabinetCount:plan.games.length,cabinets:[]};
for(const job of plan.games){
  const id=safe(job.asset.replace(/\.png$/,'')),dir=path.join(out,id);
  for(const p of ['references',...parts.map(p=>`parts/${p}`)])await fs.mkdir(path.join(dir,p),{recursive:true});
  const meta=metadata.sets.find(m=>m.rom===job.rom);
  const conversion=conversions.find(c=>c.asset===job.asset);
  const ref=references.find(r=>r.rom===job.rom);
  const specific=researched[job.rom];
  const manifest={schemaVersion:1,id,asset:job.asset,rom:job.rom,title:job.title,metadata:meta,
    researchStatus:specific?'individual-notes-partial':'source-inventory-only-needs-individual-research',
    factoryAuthenticity:'unverified',rights:'Reference use only; public access is not redistribution permission. Preserve watermarks.',
    originalIsGeneratedConversion:!!conversion,conversion:conversion??null,sourceLookup:ref??null,
    screenQuad:job.quad,screenQuadWarning:'Existing plan coordinates only; not accepted CRT geometry. Never flatten an entire cabinet. Gameplay pixel aspect is not necessarily display aspect.',
    notes:specific?.notes??['No cabinet-specific hardware investigation completed. Available source files are evidence to inspect, not verified factory configurations.'],
    researchLinks:specific?.links??[],parts:Object.fromEntries(parts.map(p=>[p,{status:'missing-isolated-reference',files:[],instruction:'Inspect whole-cabinet reference; identify this part manually. Do not invent or copy hardware from another game.'}])),files:[]};
  async function copy(src,dest,info={}){
    const bytes=await fs.readFile(src);if(info.expected)check(hash(bytes)===info.expected,`Input hash mismatch ${src}`);
    await fs.writeFile(path.join(dir,dest),bytes);
    const {expected,...details}=info;
    const record={file:dest,sha256:hash(bytes),sourcePath:path.relative(root,src).split(path.sep).join('/'),sourceSha256:hash(bytes),...details};
    manifest.files.push(record);return record;
  }
  await copy(path.join(root,'artwork/full-cabinets',job.asset),'references/library-cabinet.png',{expected:job.originalSha256,role:conversion?'generated conversion reference ONLY; not evidence for authentic shape or hardware':'existing cabinet reference; may show a restoration/conversion, not certified factory original'});
  const original=photos.find(p=>p.sourceRom===(ref?.buildReference??ref?.selected??job.rom));
  if(original?.sourceFile){
    const source=path.resolve(root,'artwork/cabinet-references',original.sourceFile);
    check(source.startsWith(path.join(root,'artwork/cabinet-references')+path.sep),'Unsafe original source');
    await copy(source,'references/source-cabinet.png',{expected:original.sourceSha256,url:original.url,role:'source reference image; inspect photo/render/restoration status, completeness and perspective before generating'});
  }
  for(const [key,part] of [['gameplay','gameplay'],['marquee','marquee'],['panel','control-panel']]){
    if(!job[key])continue;
    const s=job[key],dest=`parts/${part}/reference.png`;
    check(path.basename(s.file)===s.file,'Unsafe cached path');
    await copy(path.join(base,'downloads',s.file),dest,{expected:s.sha256,url:s.url,relation:s.relation,sourceRom:s.rom,width:s.width,height:s.height,role:part==='gameplay'?'gameplay still; preserve exact image for final screen compositing':'game-specific part reference; factory matching remains unverified'});
    manifest.parts[part]={status:'source-available-not-factory-validated',files:[dest],instruction:part==='gameplay'?'Composite this exact still after cabinet generation; do not synthesize game UI/text. Determine CRT display aspect from game evidence, not raw pixel ratio.':'Inspect reference layout and hardware; do not stretch to fit a shared template.'};
  }
  for(const [file,description,url] of specific?.extra??[]){await copy(path.join(base,'downloads',file),`references/${file}`,{role:description,url});}
  const overlay=recipes.find(r=>r.rom===job.rom&&!r.photo);
  if(overlay){
    const src=path.join(base,'downloads',`${overlay.overlay}-verticalarcade.png`);
    await copy(src,'parts/bezel/vertical-overlay-source.png',{expected:overlay.sha256,url:`https://www.verticalarcade.com/games/${overlay.page}`,credits:overlay.credits,role:'vertical-display overlay, NOT a complete cabinet or source for invented lower-body hardware'});
    // Crop only explicit source-layout coordinates. These strips are contextual
    // references, not guessed semantic segmentation or reconstructed hardware.
    const [x,y,w,h]=overlay.screen,md=await sharp(src).metadata();
    const crops=[['upper-context',0,0,md.width,y],['left-screen-surround',0,y,x,h],['right-screen-surround',x+w,y,md.width-x-w,h],['below-screen-context',0,y+h,md.width,md.height-y-h]];
    for(const [name,left,top,width,height] of crops){
      if(width<=0||height<=0)continue;
      const dest=`parts/bezel/${name}.png`;
      const bytes=await sharp(src).extract({left,top,width,height}).png().toBuffer();
      await fs.writeFile(path.join(dir,dest),bytes);
      manifest.files.push({file:dest,sha256:hash(bytes),sourcePath:path.relative(root,src),sourceSha256:overlay.sha256,crop:{left,top,width,height},role:'lossless contextual crop from overlay; may include multiple parts/transparent areas; not isolated authentic hardware'});
    }
    manifest.parts.bezel={status:'overlay-and-context-crops-available-not-factory-validated',files:manifest.files.filter(f=>f.file.startsWith('parts/bezel/')).map(f=>f.file),instruction:'Use for game-specific bezel artwork. Upper/lower strips contain context, not isolated speakers/deck/coin doors. Never treat this overlay as a whole-cabinet template.'};
  }
  for(const p of parts)await fs.writeFile(path.join(dir,`parts/${p}/README.md`),`# ${job.title}: ${p}\n\nStatus: ${manifest.parts[p].status}\n\n${manifest.parts[p].instruction}\n\n${manifest.parts[p].files.length?'Files: '+manifest.parts[p].files.join(', '):'No isolated part image supplied. Consult references/ and complete manual research; no fabricated placeholder image is provided.'}\n`);
  const brief=`# ${job.title} — image-generation handoff\n\nCabinet ID: ${id}\nROM: ${job.rom}; year: ${meta?.year??'unknown'}; manufacturer: ${meta?.manufacturer??'unknown'}.\nResearch status: **${manifest.researchStatus}**. Factory authenticity: **unverified**.\n\n## Cabinet-specific evidence\n\n${manifest.notes.map(s=>'- '+s).join('\n')}\n${conversion?'\nWARNING: library-cabinet.png is a generated conversion with borrowed shell '+conversion.shell+'. It is NOT authoritative evidence of this game’s original cabinet, controls, speakers, or coin door. Use the actual source photograph if present; research missing details.\n':''}\n## Available parts\n\n${parts.map(p=>'- '+p+': '+manifest.parts[p].status).join('\n')}\n\n## Prompt for the separate tool\n\nCreate ONE realistic complete representation of **${job.title}** based on this cabinet’s supplied real references. First inspect references and manifest.json, and identify the specific cabinet variant. Preserve its individual silhouette, marquee proportions, bezel artwork, speaker positions, control-deck slope/layout, joystick/button configuration, coin door, trim, materials and lower artwork wherever evidenced. Do not borrow a standard body or controls from another game. Do not use library generated-conversion geometry as evidence. If a part is unknown, request/source evidence rather than present an invention as authentic.\n\nReconstruct a natural straight-on view with believable depth for the separate planes; do not flatten a whole three-quarter photograph. Use consistent studio illumination, restrained wear, and clear materials, not a generic neon shell. Complete cabinet from marquee to base; transparent background, no cast background shadow, no cropping. Preserve the cabinet’s own width/height proportions; do not stretch it to fill a common tile. Suggested deliverable: high-resolution RGBA PNG (at least 1600 px tall), plus source-resolution output and a separate CRT glass/mask layer.\n\nKeep marquee/artwork lettering faithful using supplied assets; do not invent logos or gibberish. Leave a clean CRT aperture/mask for exact gameplay compositing after generation: use parts/gameplay/reference.png rather than hallucinated gameplay. Account for the game’s actual screen orientation and display aspect; raw screenshot dimensions may use non-square pixels. Include subtle CRT curvature/reflection only after fitting the full gameplay image without title-screen remnants. Do not reproduce a dark/inactive source screen.\n\n## Acceptance checklist\n\n- Compare each part against cited images; identify variant and unresolved differences.\n- No generic shared shell, borrowed coin hardware, missing feet, cut-off deck or uneven whole-image warp.\n- Gameplay belongs to this ROM/same-title relation stated in manifest; screen is fully fitted.\n- Correct distinct controls, speakers, coin-door and lower art — or explicitly unresolved, never falsely certified.\n- Preserve source originals and watermarks; obtain rights review before distributing external imagery/derivatives.\n\n## Provenance\n\nmanifest.json lists SHA-256, local source, URL where known and relationship for every image. Metadata identity does not certify that a photographed restoration/conversion is factory original. No independently researched cabinet anatomy is claimed where status is source-inventory-only.\n`;
  await fs.writeFile(path.join(dir,'GENERATION-BRIEF.md'),brief);
  await writeJSON(path.join(dir,'manifest.json'),manifest);
  index.cabinets.push({id,rom:job.rom,title:job.title,asset:job.asset,researchStatus:manifest.researchStatus,generatedConversion:!!conversion,availableParts:parts.filter(p=>manifest.parts[p].files.length)});
}
await writeJSON(path.join(out,'index.json'),index);
await writeJSON(path.join(out,'rom-to-folder.json'),mappings.map(m=>({rom:m.rom,asset:m.asset,folder:m.asset.replace(/\.png$/,'')})));
const counts={cabinets:index.cabinets.length,romMappings:mappings.length,individualNotes:index.cabinets.filter(c=>c.researchStatus==='individual-notes-partial').length,generatedConversionWarnings:index.cabinets.filter(c=>c.generatedConversion).length};
counts.partCoverage=Object.fromEntries(parts.map(p=>[p,index.cabinets.filter(c=>c.availableParts.includes(p)).length]));
await writeJSON(path.join(out,'coverage.json'),counts);
await fs.copyFile(path.join(base,'INDIVIDUAL-SOURCE-SEARCH.md'),path.join(out,'RESEARCH-SUMMARY.md'));
await fs.writeFile(path.join(out,'README.md'),`# Arcade image-generation handoff\n\n${counts.cabinets} cabinet folders; ${counts.romMappings} runtime ROM mappings in rom-to-folder.json.\n\nEach folder contains GENERATION-BRIEF.md, manifest.json, references/, and parts/ folders for marquee, control-panel, bezel, speakers, coin-door, body-side-kickplate, gameplay. Start with index.json and coverage.json. File copies are portable (no symlinks), SHA-256 checked, and originals are unchanged.\n\nIMPORTANT: this is a complete source handoff, NOT completed per-cabinet historical research. ${counts.individualNotes} cabinets have partial individual notes; others have source inventory only. ${counts.generatedConversionWarnings} library examples are generated conversions, explicitly flagged as non-authoritative. Missing isolated parts are documented, NOT invented or guessed from generic crop positions. Five researched overlays have lossless contextual bezel strips, not full cabinet anatomy segmentation.\n\nGive the separate tool ONE cabinet folder at a time. Supply the user's original eight-cabinet montage separately: it was visible in chat but is not available here as a local file, so no purported extracted crops are included. It is a visual target, not verified factory provenance. Do not give the tool the rejected generic front/batch as an authenticity reference.\n\nNo generated cabinet images have been created by this exporter. No app artwork changed. Publicly downloadable imagery is not necessarily licensed for redistribution; preserve watermarks and resolve rights before publishing.\n\nVerify: node scripts/export-arcade-generation-handoff.mjs --verify (from repository root). Rebuild requires sharp and a fresh output directory; SHARP_MODULE can name its installed module path.\n`);
console.log(JSON.stringify(counts,null,2));