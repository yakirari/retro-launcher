/** Offline-reproducible operator conversions. Node 18+, JDK 17; no Gradle. */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import os from 'node:os';

const root = path.resolve(process.argv.find(x => x.startsWith('--root='))?.slice(7)
  ?? path.join(path.dirname(fileURLToPath(import.meta.url)), '..'));
const offline = process.argv.includes('--offline');
const verifyOnly = process.argv.includes('--verify');
const review = path.join(root, 'artwork/cabinet-references/full-cabinet-review');
const dir = path.join(review, 'conversions');
const out = path.join(root, 'artwork/full-cabinets');
const mappingFile = path.join(root, 'app/src/main/assets/arcade-metadata/cabinet-art.json');
const read = async p => JSON.parse(await fs.readFile(p, 'utf8'));
const write = async (p, value) => fs.writeFile(p, JSON.stringify(value, null, 2) + '\n');
const sha = async p => crypto.createHash('sha256').update(await fs.readFile(p)).digest('hex');
const assert = (ok, message) => { if (!ok) throw new Error(message); };
const inventory = (await read(path.join(root, 'artwork/cabinet-references/cabinet-manifest.json'))).mappings;
const gaps = (await read(path.join(review, 'gaps.json'))).gaps;
const metadata = (await read(path.join(root, 'app/src/main/assets/arcade-metadata/metadata.json'))).sets;
const meta = new Map(metadata.map(x => [x.rom, x]));
const existing = (await read(mappingFile)).mappings;
const sourcePhotos = (await read(path.join(review, 'provenance.json'))).sources;
const kind = 'period-style-conversion';
await fs.mkdir(path.join(dir, 'marquees'), { recursive: true });

// Explicit, inspected source geometry lives in the Java renderer. These are not
// claims of a game's original cabinet, original wiring, or factory control layout.
const shellIds = ['frogger', 'columns', 'sonic'];
const shells = await Promise.all(shellIds.map(async rom => {
  const s = sourcePhotos.find(x => x.sourceRom === rom);
  assert(s?.asset, `Missing approved shell ${rom}`);
  assert(await sha(path.join(out, s.asset)) === s.outputSha256, `Changed shell ${rom}`);
  assert(await sha(path.join(root, 'artwork/cabinet-references', s.sourceFile)) === s.sourceSha256,
    `Changed original shell ${rom}`);
  return { rom, asset: s.asset, sourceFile: s.sourceFile, url: s.url,
    sourceSha256: s.sourceSha256, cutoutSha256: s.outputSha256 };
}));
const fighters = new Set('cybots mshvsfj nwarr ringdest sf1us sgemf vsav2'.split(' '));
const motorcycles = new Set(['endurobl', 'shangonb']);
const normalizedTitle = s => s.toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');
const plans = gaps.map(gap => {
  const m = meta.get(gap.rom);
  assert(m?.title && m.year, `No verified title/year ${gap.rom}`);
  const year = /^\d{4}$/.test(m.year) ? Number(m.year) : null;
  const shell = (year !== null && year <= 1986) || gap.rom === 'fantzn2' ? 'frogger'
    : year !== null && year >= 1995 ? 'sonic' : 'columns';
  const candidates = [m.rom];
  // Only direct metadata parents/canonical identities with matching game title;
  // e.g. Muscle Bomber Duo must NOT silently receive Slam Masters' marquee.
  for (const id of [m.parent, m.canonical]) {
    const other = meta.get(id);
    if (other && normalizedTitle(other.title) === normalizedTitle(m.title) && !candidates.includes(id)) candidates.push(id);
  }
  return { rom: m.rom, title: m.title, year: m.year, canonical: m.canonical,
    shell, controls: motorcycles.has(m.rom) ? 'handlebar' : m.rom === 'gnbarich' ? 'spinner'
      : fighters.has(m.rom) ? 'six-button' : shell === 'frogger' ? 'single-joystick' : 'dual-three-button',
    candidates, asset: `${m.rom}__conversion.png`, marquee: null,
    eraReason: year === null ? 'Uncertain metadata year; early wooden upright family, not an exact date claim.'
      : shell === 'frogger' ? 'Early/mid-eighties dedicated-upright geometry reused as an imagined operator conversion.'
      : shell === 'sonic' ? 'Mid/late-nineties or later game in a period candy conversion shell.'
      : 'Late-eighties/early-nineties plain upright conversion/kit geometry; no exact JAMMA wiring claim.' };
});
assert(plans.length === 131 && new Set(plans.map(x => x.rom)).size === 131, 'Expected 131 distinct input gaps');

const cacheFile = path.join(dir, 'marquee-downloads.json');
let downloads;
try { downloads = await read(cacheFile); } catch (e) { if (e.code !== 'ENOENT') throw e; downloads = {}; }
async function download(rom) {
  assert(/^[a-z0-9_]+$/.test(rom), `Unsafe ROM ${rom}`);
  if (downloads[rom]?.status === 'downloaded') {
    const p = path.join(dir, downloads[rom].file);
    assert(await sha(p) === downloads[rom].sha256, `Changed marquee cache ${rom}`);
    return downloads[rom];
  }
  if (downloads[rom] || offline) return null;
  const url = `https://adb.arcadeitalia.net/media/mame.current/marquees/${rom}.png`;
  let record = { rom, url, status: 'unavailable' };
  try {
    const res = await fetch(url, { signal: AbortSignal.timeout(15000), redirect: 'follow' });
    record.httpStatus = res.status;
    if (res.ok) {
      const bytes = Buffer.from(await res.arrayBuffer());
      assert(bytes.length <= 12 * 1024 * 1024, 'Oversized marquee');
      assert(bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10])), 'Not PNG');
      const w = bytes.readUInt32BE(16), h = bytes.readUInt32BE(20);
      assert(w >= 24 && h >= 8 && w <= 8192 && h <= 8192 && w * h <= 16000000, 'Invalid marquee dimensions');
      const file = `marquees/${rom}.png`;
      await fs.writeFile(path.join(dir, file), bytes);
      record = { rom, url, status: 'downloaded', file, width: w, height: h,
        sha256: crypto.createHash('sha256').update(bytes).digest('hex') };
    }
  } catch (e) { record.error = String(e.message); record.status = 'failed'; }
  downloads[rom] = record;
  return record.status === 'downloaded' ? record : null;
}

// Exactly six workers maximum; a worker waits for its exact request before trying
// a verified parent. Negative outcomes are cached; offline never makes requests.
if (!verifyOnly) {
  let next = 0;
  await Promise.all(Array.from({ length: 6 }, async () => {
    while (next < plans.length) {
      const p = plans[next++];
      for (const candidate of p.candidates) {
        const image = await download(candidate);
        if (image) { p.marquee = { ...image, relation: candidate === p.rom ? 'exact-ROM'
          : 'verified-metadata-parent-or-canonical-same-title' }; break; }
      }
    }
  }));
  await write(cacheFile, downloads);

  // Persist immutable protection evidence once; reruns must not promote a prior
  // generated image to real-photo status or overwrite any original photo mapping.
  const baselineFile = path.join(dir, 'preserved-photos.json');
  let baseline;
  try { baseline = await read(baselineFile); }
  catch (e) {
    if (e.code !== 'ENOENT') throw e;
    const files = (await fs.readdir(out)).filter(x => x.endsWith('.png') && !x.endsWith('__conversion.png'));
    baseline = { mappings: existing.filter(x => x.asset && x.kind !== kind),
      assets: await Promise.all(files.map(async asset => ({ asset, sha256: await sha(path.join(out, asset)) }))) };
    await write(baselineFile, baseline);
  }
  const plan = { version: 1, representation: kind, screenPolicy: 'Dark reflective CRT only; no title/cover/gameplay pasted into screen.', shells, games: plans };
  await write(path.join(dir, 'plan.json'), plan);
  const classes = await fs.mkdtemp(path.join(os.tmpdir(), 'arcade-conversions-'));
  function run(cmd, args) {
    const r = spawnSync(cmd, args, { cwd: root, stdio: 'inherit' });
    if (r.error) throw r.error;
    assert(r.status === 0, `${cmd} failed (${r.status})`);
  }
  try {
    const javac = process.env.JAVAC ?? (process.env.JAVA_HOME
      ? path.join(process.env.JAVA_HOME, 'bin', 'javac') : 'javac');
    const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java') : 'java';
    run(javac, ['-Xlint:all', '-d', classes, 'scripts/NormalizeArcadeCabinets.java',
      'scripts/PrepareAllArcadeCabinets.java', 'scripts/GenerateArcadeConversions.java']);
    run(java, ['-Djava.awt.headless=true', '-cp', classes, 'GenerateArcadeConversions', '--self-test']);
    run(java, ['-Djava.awt.headless=true', '-Xmx768m', '-cp', classes, 'GenerateArcadeConversions', root]);
  } finally { await fs.rm(classes, { recursive: true, force: true }); }

  const byRom = new Map(plans.map(p => [p.rom, p]));
  const group = new Map();
  for (const p of plans) if (!group.has(p.canonical)) group.set(p.canonical, p);
  const mappings = new Map(existing.map(row => [row.rom, row]));
  // Every catalogue set attached to an inventory canonical group is explicit.
  const groups = new Set(inventory.map(x => meta.get(x.rom)?.canonical).filter(Boolean));
  for (const m of metadata) if (groups.has(m.canonical) && !mappings.has(m.rom))
    mappings.set(m.rom, { rom: m.rom, asset: null });
  for (const [rom, previous] of mappings) {
    if (previous.asset && previous.kind !== kind) continue;
    const m = meta.get(rom);
    const p = byRom.get(rom) ?? group.get(m?.canonical) ?? group.get(rom);
    assert(p, `No conversion for null lookup key ${rom}`);
    const shell = shells.find(s => s.rom === p.shell);
    const marqueeNote = p.marquee ? `Marquee ${p.marquee.rom} (${p.marquee.relation}); ${p.marquee.url}.`
      : 'Small typeset title in top marquee; no original marquee located.';
    mappings.set(rom, { rom, asset: p.asset, kind, source: shell.url,
      notes: `Imagined operator conversion, NOT a historical exact cabinet or authentic factory design. Photo shell ${p.shell}; donor-game signage/decal areas repainted in the copy. ${p.eraReason} ${marqueeNote} Dark reflective CRT; no title or cover in screen. ${p.controls} controls are illustrative, not verified original controls or wiring.`
        + (rom !== p.rom ? ` Explicit existing catalogue canonical-group alias of inventory ROM ${p.rom}; regional appearance may differ.` : '') });
  }
  await write(mappingFile, { mappings: [...mappings.values()].sort((a,b) => a.rom.localeCompare(b.rom, 'en')) });
}

async function verify() {
  const baseline = await read(path.join(dir, 'preserved-photos.json'));
  const provenance = await read(path.join(dir, 'provenance.json'));
  const rows = (await read(mappingFile)).mappings;
  const map = new Map(rows.map(x => [x.rom, x]));
  assert(map.size === rows.length, 'Duplicate lookup key');
  for (const row of rows) {
    assert(JSON.stringify(Object.keys(row)) === JSON.stringify(['rom','asset','kind','source','notes']), `Schema ${row.rom}`);
    assert(row.asset, `Uncovered key ${row.rom}`);
    await fs.access(path.join(out, row.asset));
  }
  for (const row of baseline.mappings) assert(JSON.stringify(map.get(row.rom)) === JSON.stringify(row), `Modified real-photo mapping ${row.rom}`);
  for (const file of baseline.assets) assert(await sha(path.join(out, file.asset)) === file.sha256, `Modified original ${file.asset}`);
  for (const photo of sourcePhotos) if (photo.asset) assert(await sha(path.join(out, photo.asset)) === photo.outputSha256, `Photo provenance mismatch ${photo.asset}`);
  assert(provenance.games.length === gaps.length, 'Conversion count');
  for (const p of provenance.games) {
    assert(map.get(p.rom)?.kind === kind && map.get(p.rom)?.asset === p.asset, `Conversion mapping ${p.rom}`);
    assert(await sha(path.join(out, p.asset)) === p.outputSha256, `Conversion hash ${p.rom}`);
    if (p.marquee) assert(await sha(path.join(dir, p.marquee.file)) === p.marquee.sha256, `Marquee source hash ${p.rom}`);
    assert(p.alphaAudit.transparentBorder && p.alphaAudit.width === 960 && p.alphaAudit.height === 768, `Alpha audit ${p.rom}`);
    const bytes = await fs.readFile(path.join(out, p.asset));
    assert(bytes.readUInt32BE(16) === 960 && bytes.readUInt32BE(20) === 768 && bytes[25] === 6, `RGBA PNG ${p.rom}`);
  }
  for (const p of inventory) assert(map.get(p.rom)?.asset, `Uncovered inventory ${p.rom}`);
  for (const p of plans) for (const m of metadata.filter(x => x.canonical === p.canonical))
    assert(map.get(m.rom)?.asset, `Uncovered catalogue alias ${m.rom}`);
  const invKinds = {};
  for (const x of inventory) { const k = map.get(x.rom).kind; invKinds[k] = (invKinds[k] ?? 0) + 1; }
  console.log(JSON.stringify({ status: 'PASS', inventory: inventory.length, coveredInventory: inventory.length,
    inventoryKinds: invKinds, lookupKeys: rows.length, coveredLookupKeys: rows.length,
    preservedPhotoAssets: baseline.assets.length, conversions: provenance.games.length,
    sourceMarquees: provenance.games.filter(x => x.marquee).length,
    typesetMarquees: provenance.games.filter(x => !x.marquee).length,
    shells: Object.fromEntries(shellIds.map(id => [id, provenance.games.filter(x => x.shell === id).length])) }, null, 2));
}
await verify();