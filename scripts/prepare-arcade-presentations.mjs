/** Node 18+/JDK17. Exact-game stills, reviewed screen geometry, separate frontal illustrations. */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import os from 'node:os';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const review = path.join(root, 'artwork/arcade-presentations-review');
const cache = path.join(review, 'downloads');
const out = path.join(root, 'artwork/arcade-presentations');
const read = async p => JSON.parse(await fs.readFile(p, 'utf8'));
const write = async (p, v) => fs.writeFile(p, JSON.stringify(v, null, 2) + '\n');
const sha = b => crypto.createHash('sha256').update(b).digest('hex');
const assert = (v, s) => { if (!v) throw Error(s); };
await fs.mkdir(cache, { recursive: true }); await fs.mkdir(out, { recursive: true });
const mappings = (await read(path.join(root, 'app/src/main/assets/arcade-metadata/cabinet-art.json'))).mappings;
const sets = (await read(path.join(root, 'app/src/main/assets/arcade-metadata/metadata.json'))).sets;
const meta = new Map(sets.map(x => [x.rom, x]));
const plans = (await read(path.join(root, 'artwork/cabinet-references/full-cabinet-review/conversions/plan.json'))).games;
const photoQuads = await read(path.join(review, 'screen-quads.json'));
const recordsFile = path.join(review, 'downloads.json');
let records = await read(recordsFile).catch(e => { if (e.code !== 'ENOENT') throw e; return {}; });
const offline = process.argv.includes('--offline') || process.argv.includes('--verify');
async function download(kind, rom) {
  assert(/^[a-z0-9_]+$/.test(rom), `Unsafe ROM ${rom}`);
  const key = `${kind}/${rom}`;
  if (records[key]?.file) {
    assert(sha(await fs.readFile(path.join(cache, records[key].file))) === records[key].sha256, `Changed source ${key}`);
    return records[key];
  }
  if (offline || (records[key] && !process.argv.includes('--retry-missing'))) return null;
  const url = `https://adb.arcadeitalia.net/media/mame.current/${kind}/${rom}.png`;
  let record = { url, status: 'unavailable' };
  try {
    const r = await fetch(url, { signal: AbortSignal.timeout(20000) });
    record.httpStatus = r.status;
    if (r.ok) {
      const bytes = Buffer.from(await r.arrayBuffer());
      assert(bytes.length < 12 * 1024 * 1024 && bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10])), `Invalid PNG ${key}`);
      const width = bytes.readUInt32BE(16), height = bytes.readUInt32BE(20);
      assert(width >= 8 && height >= 8 && width * height <= 16000000, `Invalid image dimensions ${key}`);
      const file = `${kind}-${rom}.png`;
      await fs.writeFile(path.join(cache, file), bytes);
      record = { url, rom, file, sha256: sha(bytes), width, height, status: 'downloaded' };
    }
  } catch (e) { record = { ...record, status: 'failed', error: e.message }; }
  records[key] = record;
  return record.file ? record : null;
}
const jobs = [...new Set(mappings.map(x => x.asset))].sort().map(asset => {
  const rom = asset.replace(/__conversion\.png$|\.png$/g, '');
  const m = meta.get(rom);
  assert(m, `Missing metadata ${rom}`);
  const conversion = plans.find(x => x.asset === asset);
  const quad = conversion ? { frogger: [425,290,508,295,546,362,436,370],
    columns: [418,264,520,250,579,385,445,433], sonic: [333,218,568,212,588,379,348,399] }[conversion.shell] : photoQuads[rom];
  return { asset, rom, title: m.title, quad: quad ?? null, originalSha256: null,
    gameplay: null, marquee: null, panel: null };
});
const titleKey = s => s.toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');
let next = 0;
await Promise.all(Array.from({ length: 6 }, async () => {
  while (next < jobs.length) {
    const j = jobs[next++], m = meta.get(j.rom);
    j.originalSha256 = sha(await fs.readFile(path.join(root, 'artwork/full-cabinets', j.asset)));
    const candidates = [j.rom];
    for (const id of [m.parent, m.canonical]) {
      if (id && meta.get(id) && titleKey(meta.get(id).title) === titleKey(m.title) && !candidates.includes(id)) candidates.push(id);
    }
    for (const [key, kind] of [['gameplay','ingames'], ['marquee','marquees'], ['panel','cpanels']]) {
      for (const id of candidates) {
        const source = await download(kind, id);
        if (source) { j[key] = { ...source, relation: id === j.rom ? 'exact-ROM' : 'metadata-same-title-parent' }; break; }
      }
    }
    if (next % 40 === 0) console.log(`Sources ${next}/${jobs.length}`);
  }
}));
await write(recordsFile, records);
const plan = { version: 1, note: 'Gameplay stills, not animated or live emulation. Fronts are original illustrative operator cabinets, not authentic factory reconstructions. Original cabinet PNGs remain untouched.', games: jobs };
if (!process.argv.includes('--verify')) await write(path.join(review, 'plan.json'), plan);
else assert(JSON.stringify((await read(path.join(review, 'plan.json'))).games) === JSON.stringify(jobs), 'Plan/source changed');
const classes = await fs.mkdtemp(path.join(os.tmpdir(), 'arcade-presentations-'));
const javaHome = process.env.JAVA_HOME;
assert(javaHome, 'Set JAVA_HOME to JDK17');
function run(cmd, args) { const r = spawnSync(cmd, args, { cwd: root, stdio: 'inherit' }); if (r.error) throw r.error; assert(r.status === 0, `${cmd}: exit ${r.status}`); }
try {
  run(path.join(javaHome, 'bin/javac'), ['-Xlint:all', '-d', classes, 'scripts/NormalizeArcadeCabinets.java', 'scripts/PrepareAllArcadeCabinets.java', 'scripts/GenerateArcadeConversions.java', 'scripts/PrepareArcadePresentations.java']);
  run(path.join(javaHome, 'bin/java'), ['-Djava.awt.headless=true', '-Xmx768m', '-cp', classes, 'PrepareArcadePresentations', root, ...(process.argv.includes('--verify') ? ['--verify'] : [])]);
} finally { await fs.rm(classes, { recursive: true, force: true }); }
console.log(JSON.stringify({ cabinets: jobs.length, gameplay: jobs.filter(x => x.gameplay).length,
  reviewedLitScreens: jobs.filter(x => x.gameplay && x.quad).length,
  marquees: jobs.filter(x => x.marquee).length, panels: jobs.filter(x => x.panel).length }, null, 2));