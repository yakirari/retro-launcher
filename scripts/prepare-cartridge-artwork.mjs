/** System-wide source inventory and reviewed-label preparation. No device/library/ROM access.
 * node scripts/prepare-cartridge-artwork.mjs --archive /path/scans.zip --output /tmp/n64-review
 * Add --prepare for reviewed PNGs; --distribution additionally requires explicit rights approval.
 * JAVA_HOME must select JDK 17+. This is an offline tool: no automatic copyright-media downloads.
 */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const args = process.argv.slice(2);
const option = name => {
    const i = args.indexOf(name);
    return i < 0 ? undefined : args[i + 1];
};
if (args.includes('--help')) {
    console.log('Usage: node scripts/prepare-cartridge-artwork.mjs --archive scans.zip --output /tmp/n64-review [--prepare | --batch-crop] [--distribution]');
    process.exit(0);
}
for (let i = 0; i < args.length; i++) {
    if (['--archive', '--output', '--manifest'].includes(args[i])) { if (!args[++i] || args[i].startsWith('--')) throw Error('Missing option value'); }
    else if (!['--prepare', '--batch-crop', '--distribution'].includes(args[i])) throw Error(`Unknown option: ${args[i]}`);
}
const archive = path.resolve(option('--archive') ?? '');
const output = path.resolve(option('--output') ?? '');
const appAssets = path.join(root, 'app/src/main/assets');
if (!option('--archive') || !option('--output')) throw Error('Explicit --archive and --output are required');
const localBatch = path.join(root, 'artwork/n64/local-prepared');
if ((output === root || output.startsWith(root + path.sep)) && !(args.includes('--batch-crop') && output === localBatch))
    throw Error('Use external output, or artwork/n64/local-prepared for local batch crops; never Android build inputs');
if (args.includes('--batch-crop') && (args.includes('--prepare') || args.includes('--distribution')))
    throw Error('Batch crops are local prepared candidates, not reviewed/runtime or distribution approval');
const manifest = JSON.parse(fs.readFileSync(option('--manifest') ?? path.join(appAssets, 'cartridge-artwork/n64-scans.json'), 'utf8'));
if (manifest.schemaVersion !== 1 || manifest.platform !== 'Nintendo 64') throw Error('Unsupported manifest');
const digest = crypto.createHash('sha1');
for await (const chunk of fs.createReadStream(archive)) digest.update(chunk);
if (digest.digest('hex') !== manifest.archiveSha1) throw Error('Original archive hash mismatch');
const approved = manifest.entries.filter(x => x.reviewed === true);
const signatures = new Set();
for (const e of approved) {
    if (!e.filename.endsWith(' Cart.jpg') || /[/\\\t\r\n]/.test(e.filename) || e.filename.includes('..') ||
        !/^[0-9a-f]{64}$/.test(e.sha256) || !Array.isArray(e.crop) || e.crop.length !== 4) throw Error('Unsafe source metadata');
    const [x,y,w,h] = e.crop;
    if (![x,y,w,h,e.sourceWidth,e.sourceHeight].every(Number.isInteger) || x < 0 || y < 0 || w < 300 || h < 350 ||
        x+w > e.sourceWidth || y+h > e.sourceHeight || w/h < .8 || w/h > .95) throw Error('Invalid reviewed crop');
    if (!e.signatures?.length) throw Error('Missing exact ROM identity');
    for (const crc of e.signatures) {
        if (!/^[0-9A-F]{8}$/.test(crc) || signatures.has(crc)) throw Error('Invalid/duplicate exact signature');
        signatures.add(crc);
    }
}
// Refuse before creating/exporting anything; local review is not a redistribution grant.
if (args.includes('--distribution') && (!args.includes('--prepare') || approved.some(e =>
    e.redistributionApproved !== true || !e.redistributionEvidence?.trim()))) {
    throw Error('Distribution blocked: every exported image requires explicit redistribution approval and evidence');
}
fs.mkdirSync(output, { recursive: true });
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java') : 'java';
const run = params => {
    const result = spawnSync(java, [path.join(root, 'scripts/PrepareCartridgeArtwork.java'), ...params], { stdio: 'inherit' });
    if (result.error) throw result.error;
    if (result.status !== 0) throw Error('Cartridge preparation failed');
};
if (args.includes('--batch-crop')) {
    run(['batch-crop', archive, output, path.join(root, 'artwork/n64/crop-overrides.tsv')]);
    run(['verify-crops', archive, output]);
    const known = new Map(approved.map(e => [e.filename, e]));
    const qualityNotes = new Map([
        ['Harvest Moon 64 (U) Cart.jpg', 'Severe source moire; do not automatically replace a cleaner fallback'],
        ['Indiana Jones and the Infernal Machine (U) Cart.jpg', 'Faded/low contrast original'],
        ['Air Boarder 64 (E) Cart.jpg', 'Faded/low contrast original'],
        ['Paperboy (U) Cart.jpg', 'Faded/low contrast original'],
        ['Knife Edge - Nose Gunner (U) Cart.jpg', 'Strong source moire'],
        ['Pilotwings 64 (U) Cart.jpg', 'Physical label wear/damage; preserved, not reconstructed'],
        ["Elmo's Number Journey (U) Cart.jpg", 'Physical price sticker on original; preserved, not reconstructed']
    ]);
    const entries = fs.readFileSync(path.join(output, 'all-crops.tsv'), 'utf8').trim().split('\n').slice(1).map(row => {
        const [id, filename, sw, sh, sourceSha256, x, y, w, h, method] = row.split('\t');
        const regionCode = filename.match(/\((U|UK|E|F|J|AU|BR|DE|IT)\) Cart\.jpg$/)?.[1] ?? 'unknown';
        const region = { U:'USA', UK:'United Kingdom', E:'Europe', F:'France', J:'Japan', AU:'Australia', BR:'Brazil', DE:'Germany', IT:'Italy' }[regionCode] ?? regionCode;
        const title = filename.replace(/\s*\((?:U|UK|E|F|J|AU|BR|DE|IT)\) Cart\.jpg$/, '');
        const bytes = fs.readFileSync(path.join(output, 'labels', id + '.png'));
        const flags = [];
        if (+w < 200 || +h < 230) flags.push('low-source-resolution');
        if (+w / +h < .8 || +w / +h > .98) flags.push('nonstandard-crop-aspect');
        if (qualityNotes.has(filename)) flags.push(qualityNotes.get(filename));
        return { id, title, region, sourceFilename:filename, sourceSha256, sourceWidth:+sw, sourceHeight:+sh,
            crop:[+x,+y,+w,+h], cropMethod:method, filename:'labels/'+id+'.png',
            width:+w, height:+h, sha256:crypto.createHash('sha256').update(bytes).digest('hex'),
            signatures:known.get(filename)?.signatures ?? [],
            runtimeIdentityStatus:known.has(filename)?'verified-header-crc':'not-mapped-do-not-guess',
            qualityFlags:flags, redistributionApproved:false };
    });
    const index = { schemaVersion:1, platform:manifest.platform, sourceCollection:manifest.sourceCollection,
        verification:'all-313-source-hashes-dimensions-and-crop-pixels-verified',
        archiveSha1:manifest.archiveSha1, purpose:'local-prepared-labels-not-redistribution-cleared',
        summary:{ total:entries.length, newlyCropped:entries.filter(e=>!known.has(e.sourceFilename)).length,
            qualityFlagged:entries.filter(e=>e.qualityFlags.length).length,
            exactRuntimeIdentities:entries.filter(e=>e.signatures.length).length }, entries };
    fs.writeFileSync(path.join(output,'index.json'),JSON.stringify(index,null,2)+'\n');
    fs.writeFileSync(path.join(output,'README.txt'),
        'Original-resolution N64 sticker crops. Open index.json for title/region/source/crop/hash metadata.\n'+
        'All source scans cropped; no resizing, AI lettering or regional substitution.\n'+
        'Low resolution, damage, moire and faded printing cannot be repaired by cropping. See qualityFlags.\n'+
        'Only entries with verified signatures are eligible for current exact-ROM runtime lookup.\n'+
        'These local files are not redistribution-cleared and are not bundled by Gradle.\n');
    console.log(JSON.stringify(index.summary,null,2));
    process.exit(0);
}
run(['inventory', archive, output]);
const inventory = fs.readFileSync(path.join(output, 'inventory.tsv'), 'utf8').trim().split('\n').slice(1).map(row => {
    const [filename, w, h, sha256] = row.split('\t');
    const existing = approved.find(e => e.filename === filename);
    return { filename, sourceWidth: +w, sourceHeight: +h, sha256, reviewed: !!existing,
        signatures: existing?.signatures ?? [], crop: existing?.crop ?? null, redistributionApproved: false,
        status: existing ? 'reviewed-external-source' : 'needs-sticker-crop-identity-and-quality-review' };
});
fs.writeFileSync(path.join(output, 'source-inventory.json'), JSON.stringify({ platform: manifest.platform,
    sourceCollection: manifest.sourceCollection, entries: inventory }, null, 2) + '\n');
if (args.includes('--prepare')) {
    const rows = approved.flatMap(e => e.signatures.map(crc => [crc, e.filename, e.sha256, e.sourceWidth, e.sourceHeight, ...e.crop].join('\t')));
    const rowsPath = path.join(output, 'reviewed-crops.tsv');
    fs.writeFileSync(rowsPath, rows.join('\n') + '\n');
    run(['prepare', archive, path.join(output, 'prepared'), rowsPath]);
    const exported = approved.flatMap(e => e.signatures.map(signature => {
        const filename = `${signature}.png`;
        const bytes = fs.readFileSync(path.join(output, 'prepared', filename));
        return { signature, title: e.title, region: e.region, filename, sha256: crypto.createHash('sha256').update(bytes).digest('hex'),
            width: e.crop[2], height: e.crop[3], sourceFilename: e.filename, sourceSha256: e.sha256,
            redistributionApproved: e.redistributionApproved, redistributionEvidence: e.redistributionEvidence ?? null };
    }));
    fs.writeFileSync(path.join(output, 'prepared/index.json'), JSON.stringify({ schemaVersion: 1, platform: manifest.platform,
        purpose: args.includes('--distribution') ? 'rights-approved-distribution' : 'local-review-only-not-redistribution-cleared',
        entries: exported }, null, 2) + '\n');
}
console.log(JSON.stringify({ sourceScans: inventory.length, reviewedScans: approved.length,
    exactSignatures: signatures.size, unreviewedScans: inventory.filter(e => !e.reviewed).length,
    prepared: args.includes('--prepare'), output }, null, 2));