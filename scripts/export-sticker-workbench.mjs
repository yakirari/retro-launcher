import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const out = path.join(root, 'artwork/sticker-workbench');
const require = createRequire(path.resolve(process.env.ARTWORK_TOOLS || '/tmp/retro-art-handoff-tools', 'package.json'));
const sharp = require('sharp');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const escape = text => String(text).replace(/[&<>"']/g,
    c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
const templates = {
    nes: {file: 'app/src/main/res/drawable-nodpi/nostalgic_overlay_nes.png', width: 298, height: 479},
    snesUsa: {file: 'app/src/main/res/drawable-nodpi/nostalgic_overlay_snes.png', width: 469, height: 223},
    snesJapan: {file: 'app/src/main/res/drawable-nodpi/nostalgic_overlay_snes_japan.png', width: 648, height: 201}
};

if (process.argv.includes('--verify')) {
    const manifest = JSON.parse(await fs.readFile(path.join(out, 'index.json'), 'utf8'));
    for (const template of Object.values(manifest.templates)) {
        assert.equal(hash(await fs.readFile(path.join(root, template.file))), template.sha256);
    }
    assert.equal(new Set(manifest.games.map(game => game.id)).size, manifest.games.length);
    for (const game of manifest.games) {
        assert(['needs-sticker', 'verified-sticker', 'restored-sticker', 'manual-sticker'].includes(game.status));
        if (!game.art) continue;
        assert.equal(game.status, 'needs-sticker');
        assert(/^game-art\/[a-z0-9-]+\.png$/.test(game.art.file));
        const bytes = await fs.readFile(path.join(out, game.art.file));
        assert.equal(hash(bytes), game.art.sha256, game.title);
        const metadata = await sharp(bytes).metadata();
        assert.equal(metadata.width, game.art.width);
        assert.equal(metadata.height, game.art.height);
    }
    for (const scan of manifest.availableSnesScans) {
        assert.equal(hash(await fs.readFile(path.join(root, scan.file))), scan.sha256);
    }
    console.log(`PASS: ${manifest.games.length} game records, artwork hashes/dimensions and fixed templates verified.`);
    process.exit(0);
}

const adb = process.env.ADB_BIN || path.join(process.env.HOME, 'Android/Sdk/platform-tools/adb');
assert(process.env.ADB_SERIAL, 'Set ADB_SERIAL to the explicitly selected device.');
const device = (...args) => execFileSync(adb, ['-s', process.env.ADB_SERIAL, 'exec-out', ...args],
    {maxBuffer: 32 * 1024 * 1024});
const inventory = JSON.parse(device('run-as', 'com.ariy.retrolauncher',
    'cat', 'cache/nes-snes-sticker-inventory.json').toString());
assert.equal(inventory.version, 1);
const games = [];
const images = [];
for (const row of inventory.games) {
    assert(['Nintendo Entertainment System', 'Super Nintendo'].includes(row.platform));
    const {imageUri, ...game} = row;
    if (game.status === 'needs-sticker' && imageUri) {
        const uri = new URL(imageUri);
        assert.equal(uri.protocol, 'file:', `Unsupported device artwork URI for ${game.title}`);
        const source = decodeURIComponent(uri.pathname);
        assert(/^\/data\/(?:user\/0|data)\/com\.ariy\.retrolauncher\/files\/[^']+$/.test(source),
            `Artwork is outside the app files directory: ${game.title}`);
        const bytes = device('run-as', 'com.ariy.retrolauncher', 'cat', source);
        const metadata = await sharp(bytes).metadata();
        assert.equal(metadata.format, 'png', `Expected PNG artwork: ${game.title}`);
        const slug = game.title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'game';
        const file = `game-art/${slug}-${hash(Buffer.from(game.id)).slice(0, 12)}.png`;
        game.art = {file, width: metadata.width, height: metadata.height, sha256: hash(bytes),
            representation: 'Current device game art; not an authenticated cartridge sticker.',
            source: 'Existing launcher imageUri; original provider and media rights not established by this export.'};
        images.push({file, bytes});
    } else if (game.status === 'needs-sticker') {
        game.artworkIssue = 'No device artwork is assigned.';
    }
    const regions = [...game.romFilename?.matchAll(/\((USA|Japan|Europe|World|Australia)\)/gi) || []]
        .map(match => match[1].toLowerCase());
    game.regionEvidence = regions.length ? regions : ['unknown'];
    game.replacementSticker = null;
    games.push(game);
}
for (const template of Object.values(templates)) {
    template.sha256 = hash(await fs.readFile(path.join(root, template.file)));
}
const snesCatalog = JSON.parse(await fs.readFile(path.join(root, 'app/src/main/assets/snes_labels/index.json'), 'utf8'));
const availableSnesScans = snesCatalog.labels.map(scan => ({
    title: scan.romTitle, file: `app/src/main/assets/snes_labels/${scan.path}`,
    sha256: scan.sha256, width: scan.width, height: scan.height, source: scan.sourceUrl,
    representation: scan.representation || 'User-supplied cartridge label; review title, region and edition before use.'
}));

// Refuse to overwrite any hand-edited manifest or future user-created stickers.
await fs.mkdir(out);
await fs.mkdir(path.join(out, 'game-art'));
for (const {file, bytes} of images) await fs.writeFile(path.join(out, file), bytes);
const counts = Object.fromEntries(['verified-sticker', 'restored-sticker', 'manual-sticker', 'needs-sticker']
    .map(status => [status, games.filter(game => game.status === status).length]));
await fs.writeFile(path.join(out, 'index.json'), JSON.stringify({
    version: 1, createdAt: new Date().toISOString(), counts, templates, availableSnesScans,
    purpose: 'Local sticker replacement workbench, not an app asset catalog or redistribution-cleared artwork pack.',
    policy: 'Keep fixed windows and preserve original art. Unknown ROM region stays unknown. Review region, title, edition and fit before assigning a replacement. AI/reconstructed stickers must be identified as such, never as authentic scans.',
    games
}, null, 2) + '\n');
await fs.writeFile(path.join(out, 'gallery.html'), `<!doctype html><meta charset="utf-8">
<title>NES / SNES sticker workbench</title>
<style>body{background:#17191e;color:#eee;font:16px sans-serif;margin:24px}main{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:20px}figure{margin:0;padding:16px;background:#282c34}img{width:100%;height:220px;object-fit:contain}small{display:block;color:#bbc}h2{font-size:17px}</style>
<h1>NES / SNES replacement stickers</h1><p>Current game art for ${images.length} games needing stickers.
These are references, not verified labels. Do not resize cartridge windows. Unknown region needs review.
See index.json for verified sticker coverage and fixed target dimensions.</p>
<h2>Fixed cartridge templates</h2><main>${Object.entries(templates).map(([name, template]) =>
    `<figure><h2>${escape(name)}</h2><img src="../../${template.file}"><small>Window: ${template.width}×${template.height}</small></figure>`).join('')}</main>
<h2>Device game art needing stickers</h2><main>${games.filter(game => game.art)
    .map(game => `<figure><h2>${escape(game.title)}</h2><img loading="lazy" src="${game.art.file}">
<small>${escape(game.platform)} · ${escape(game.regionEvidence.join(', '))}</small>
<small>${game.art.width}×${game.art.height} · needs sticker</small></figure>`).join('')}</main>
<h2>Available reviewed SNES museum scans (not automatically matched to these device games)</h2>
<main>${availableSnesScans.map(scan => `<figure><h2>${escape(scan.title)}</h2><img loading="lazy" src="../../${scan.file}">
<small>${scan.width}×${scan.height} · reviewed label crop</small></figure>`).join('')}</main>\n`);
console.log(`Exported ${images.length} game-art files; coverage ${JSON.stringify(counts)}. Device library unchanged.`);
