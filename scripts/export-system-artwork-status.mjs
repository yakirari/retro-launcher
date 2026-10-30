/** Compact, read-only artwork snapshot. No research collection or ROMs are exported. */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const out = path.join(root, 'artwork/system-artwork-status');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const escape = text => String(text).replace(/[&<>"']/g, c => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[c]));
const drawable = 'app/src/main/res/drawable-nodpi/';
// Canonical names and resources from NostalgicOverlays.specByPlatform.
const systems = [
  ['nes', 'Nintendo Entertainment System', 'nes'],
  ['snes', 'Super Nintendo', 'snes'],
  ['md', 'Sega Genesis / Mega Drive', 'md'],
  ['gb', 'Game Boy', 'shell:gb', 'Game-Boy-real-sticker-review.png'],
  ['gbc', 'Game Boy Color', 'shell:gbc', 'Game-Boy-Color-real-sticker-review.png'],
  ['gba', 'Game Boy Advance', 'shell:gba', 'Game-Boy-Advance-real-sticker-review.png'],
  ['n64', 'Nintendo 64', 'n64', 'Nintendo 64-console-review.png'],
  ['nds', 'Nintendo DS', 'nds'],
  ['3ds', 'Nintendo 3DS', '3ds'],
  ['gc', 'Nintendo GameCube', 'gc', 'Nintendo GameCube-console-review.png'],
  ['wii', 'Nintendo Wii', 'wii'],
  ['switch', 'Nintendo Switch', 'switch'],
  ['ps1', 'Sony PlayStation', 'ps1'],
  ['ps2', 'Sony PlayStation 2', 'ps2', 'Sony PlayStation 2-console-review.png'],
  ['ps3', 'Sony PlayStation 3', 'ps3'],
  ['psp', 'Sony PlayStation Portable', 'psp'],
  ['dreamcast', 'Sega Dreamcast', 'cd'],
  ['scummvm', 'ScummVM', 'scummvm', 'ScummVM-console-review.png'],
  ['dos', 'DOS Games', 'dos'],
  ['digital-pc', 'PC (Windows), Steam, GOG, Epic Games, Amazon Games, GameNative, GameHub, GameHub Lite', 'digital_pc'],
  ['fallback', 'Other platforms — generic fallback', 'default']
];
const readJson = async file => JSON.parse(await fs.readFile(file, 'utf8'));

if (process.argv.includes('--verify')) {
  const manifest = await readJson(path.join(out, 'index.json'));
  for (const image of manifest.images) {
    assert.equal(hash(await fs.readFile(path.join(out, image.file))), image.sha256, image.file);
    if (image.sourceType === 'workspace') {
      assert.equal(hash(await fs.readFile(path.join(root, image.source))), image.sha256, image.source);
    }
  }
  assert.equal(manifest.systems.length, systems.length + 1);
  assert.equal(new Set(manifest.images.map(image => image.file)).size, manifest.images.length);
  for (const system of manifest.systems) {
    for (const file of [system.template, system.example, ...(system.alternatives ?? [])].filter(Boolean)) {
      assert(manifest.images.some(image => image.file === file), `Missing ${file}`);
    }
  }
  console.log(`PASS: ${manifest.systems.length} system groups, ${manifest.images.length} original PNG hashes; workspace sources unchanged.`);
  process.exit(0);
}

// Fail before exporting if any required source is absent, including cached device examples.
const adb = process.env.ADB ?? '/home/deploy-pc/Android/Sdk/platform-tools/adb';
const deviceCache = '/sdcard/Android/data/com.ariy.retrolauncher/cache/';
const pending = [];
async function queue(file, source, note, device = false) {
  const bytes = device
    ? execFileSync(adb, ['exec-out', 'cat', source], {maxBuffer: 8 * 1024 * 1024})
    : await fs.readFile(path.join(root, source));
  assert.equal(bytes.subarray(0, 8).toString('hex'), '89504e470d0a1a0a', source);
  pending.push({file, source, sourceType: device ? 'device-review-cache' : 'workspace', note,
    width: bytes.readUInt32BE(16), height: bytes.readUInt32BE(20), sha256: hash(bytes), bytes});
}
const rows = [];
for (const [id, name, resource, review] of systems) {
  const resourceName = resource.startsWith('shell:')
    ? `nostalgic_shell_${resource.slice(6)}` : `nostalgic_overlay_${resource}`;
  const template = `${id}-template.png`;
  await queue(template, `${drawable}${resourceName}.png`, 'Current runtime template. Empty window is intentional; this is not a completed game tile.');
  let example = null;
  if (review) {
    example = `${id}-example.png`;
    await queue(example, deviceCache + review,
      'Existing 2026-10-20 device bitmap review, not a live UI screenshot. Full source canvas retained; layout cropping and badges are not represented.', true);
  }
  rows.push({id, name, template, example, status: example
    ? 'Current template plus existing composed device review.'
    : 'Current template only; no suitable finished game preview reused. No synthetic placeholder artwork inserted.'});
}

const arcadeRoot = 'app/src/main/assets/arcade_presentations';
const arcadeIndex = await readJson(path.join(root, arcadeRoot, 'index.json'));
for (const [asset, mode, file] of [
  ['ddragon.png', 'front', 'arcade-stacked-double-dragon.png'],
  ['ddragon.png', 'lit', 'arcade-lit-double-dragon.png'],
  ['btoads.png', 'front', 'arcade-stacked-battletoads.png']
]) {
  const entry = arcadeIndex.mappings.find(item => item.asset === asset);
  assert(entry?.[mode]);
  await queue(file, `${arcadeRoot}/${entry[mode]}`,
    mode === 'front' ? 'Current packaged illustrative Stacked front, NOT an authenticated factory cabinet or an approved realism improvement.'
      : 'Current packaged lit variant; not the rejected realism study.');
  assert.equal(pending.at(-1).sha256, entry[`${mode}Sha256`]);
}
rows.push({id:'arcade', name:'Arcade — MAME / FinalBurn Neo', example:'arcade-stacked-double-dragon.png',
  alternatives:['arcade-lit-double-dragon.png', 'arcade-stacked-battletoads.png'],
  status:'Three CURRENT packaged examples, not the rejected experimental realism composites. These show current implementation, not a claim of authenticity or user approval.'});

await fs.mkdir(out); // Never overwrite a previous/user-modified export.
for (const image of pending) await fs.writeFile(path.join(out, image.file), image.bytes);
const images = pending.map(({bytes, ...image}) => ({...image, sizeBytes:bytes.length}));
const totalBytes = images.reduce((sum, image) => sum + image.sizeBytes, 0);
await fs.writeFile(path.join(out, 'index.json'), JSON.stringify({createdAt:new Date().toISOString(),
  description:'Compact artwork status snapshot; exact original PNG copies, no research folders or ROMs.',
  totalBytes, systems:rows, images}, null, 2) + '\n');
await fs.writeFile(path.join(out, 'README.md'), `# Compact current artwork status\n\n${rows.length} system groups, ${images.length} PNGs, ${(totalBytes / 1048576).toFixed(2)} MiB of images. Open gallery.html.\n\nOne current runtime template per distinct console/handheld/digital format; seven existing composed device reviews; three current arcade examples. Digital stores share one template, and MAME/FBN share the arcade presentation. All images are original-dimension, byte-identical copies. No full library, ROMs, research downloads, backgrounds or rejected realism experiments.\n\n## Read the labels\n\n- **Example:** an existing device-generated bitmap review from 2026-10-20, not a live launcher screenshot or a complete Compose rendering. Full canvases are retained. The N64 example uses the sharper cartridge sticker; GB/GBC/GBA examples use real downloaded stickers.\n- **Template only:** current prepared runtime shell/case without inserted game art. No suitable finished preview was available to reuse. This does not mean the launcher cannot compose that platform's game artwork. PS1/Dreamcast magenta test fixtures are deliberately excluded.\n- **Arcade:** currently packaged illustrative fronts/lit images, not the rejected realism studies, newly improved authentic cabinets, or user-approved final results.\n- Raw templates may contain stock printing already present in the app. Device review composites may omit runtime tint/mask/cropping details; inspect current templates and source code for exact behavior.\n\n## Provenance and rights\n\nindex.json records each source, dimensions and SHA-256. This is a local review snapshot, not a redistribution-cleared media pack. Existing third-party rights and attribution still apply; no game imagery is relicensed as MIT. See the project's THIRD_PARTY_NOTICES.md, app/src/main/assets/ARTWORK-CREDITS.txt and artwork guides. No launcher assets, preferences or library entries changed.\n\nRecheck with the exporter --verify option. Regeneration requires the existing device review cache; export refuses to overwrite an existing folder.\n`);
const figure = (file, caption) => `<figure><a href="${file}"><img loading="lazy" src="${file}" alt="${escape(caption)}"></a><figcaption>${escape(caption)}</figcaption></figure>`;
await fs.writeFile(path.join(out, 'gallery.html'), `<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Current system artwork</title><style>body{margin:0;background:#141923;color:#edf1f7;font:16px system-ui;padding:24px}header{max-width:1000px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:18px}section{background:#222a38;padding:18px;border-radius:12px}h2{font-size:19px}p,figcaption{color:#c5cedc;line-height:1.5}figure{margin:10px 0}img{width:100%;height:260px;object-fit:contain;background:repeating-conic-gradient(#313a49 0% 25%,#283140 0% 50%) 0/20px 20px;border-radius:6px}a{color:#9cceff}details img{height:180px}</style><header><h1>Current system artwork — compact snapshot</h1><p>${rows.length} system groups · ${images.length} original PNGs · ${(totalBytes/1048576).toFixed(2)} MiB. Seven existing device bitmap examples, current templates for every distinct supported nostalgic format, and three packaged arcade samples.</p><p><strong>Template ≠ completed tile.</strong> No live UI screenshots, new generated game art, rejected realism experiments or authenticity claims. Shared digital-store and arcade templates are not duplicated.</p><p><a href="README.md">Scope and rights</a> · <a href="index.json">Source hashes</a></p></header><main>${rows.map(row => `<section><h2>${escape(row.name)}</h2><p>${escape(row.status)}</p>${row.example ? figure(row.example, row.id === 'arcade' ? 'Current Stacked illustration — Double Dragon' : 'Existing composed device bitmap review') : figure(row.template, 'TEMPLATE ONLY — no inserted game art')}${row.example && row.template ? `<details><summary>Current runtime template</summary>${figure(row.template, 'Unmodified current template')}</details>` : ''}${(row.alternatives ?? []).map(file => figure(file, file.replaceAll('-', ' ').replace('.png', ''))).join('')}</section>`).join('')}</main></html>`);
console.log(JSON.stringify({folder:out, systemGroups:rows.length, images:images.length, imageMiB:totalBytes/1048576}, null, 2));