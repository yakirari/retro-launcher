/** Import a complete user-supplied front pack without resizing or altering pixels. Node 18+. */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const output = path.join(root, 'artwork/arcade-presentations');
const manifestPath = path.join(root, 'artwork/arcade-presentations-review/front-import.json');
const read = async p => JSON.parse(await fs.readFile(p, 'utf8'));
const write = async (p, value) => fs.writeFile(p, JSON.stringify(value, null, 2) + '\n');
const assert = (ok, message) => { if (!ok) throw Error(message); };
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const verify = process.argv.includes('--verify');
const sourceArg = process.argv.slice(2).find(arg => arg !== '--verify');
assert(verify || sourceArg, 'Usage: node scripts/import-arcade-fronts.mjs <pack/front> | --verify');
const index = await read(path.join(output, 'index.json'));
const expected = index.mappings.map(row => row.asset).sort();
assert(new Set(expected).size === expected.length, 'Duplicate index assets');
assert(expected.every(name => /^[a-z0-9_]+\.png$/.test(name)), 'Unsafe asset name');
const source = sourceArg ? path.resolve(sourceArg) : path.join(output, 'front');
const names = (await fs.readdir(source)).sort();
assert(JSON.stringify(names) === JSON.stringify(expected), 'Front pack must exactly match every indexed cabinet filename');
const records = [];
// Validate the entire pack before replacing anything.
for (const asset of expected) {
  const bytes = await fs.readFile(path.join(source, asset));
  assert(bytes.length >= 33 && bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10])) &&
    bytes.toString('ascii', 12, 16) === 'IHDR', `Invalid PNG: ${asset}`);
  const width = bytes.readUInt32BE(16), height = bytes.readUInt32BE(20);
  assert(width > 0 && height > 0 && width * height <= 16000000, `Invalid dimensions: ${asset}`);
  records.push({ asset, sha256: sha(bytes), width, height });
}
const saved = verify ? await read(manifestPath) : null;
if (verify) {
  assert(saved.version === 1 && JSON.stringify(saved.fronts) === JSON.stringify(records), 'Imported fronts changed');
} else {
  for (const record of records) await fs.copyFile(path.join(source, record.asset), path.join(output, 'front', record.asset));
  await write(manifestPath, {
    version: 1,
    source: `${path.basename(path.dirname(source))}/${path.basename(source)}`,
    processing: 'Byte-for-byte import; no resizing, cropping, background removal or screen replacement.',
    rights: 'User-supplied artwork. Creator, generation method and redistribution permissions not supplied; not certified factory photographs or MIT-licensed media.',
    fronts: records,
  });
}
const byAsset = new Map(records.map(record => [record.asset, record]));
for (const row of index.mappings) {
  const record = byAsset.get(row.asset);
  if (verify) {
    assert(row.front === `front/${row.asset}` && row.frontSha256 === record.sha256 &&
      row.frontWidth === record.width && row.frontHeight === record.height &&
      row.frontSource === saved.source && row.representation === 'user-supplied-front-unverified-provenance', `Index mismatch: ${row.asset}`);
  } else {
    row.frontSha256 = record.sha256;
    row.frontWidth = record.width;
    row.frontHeight = record.height;
    row.frontSource = `${path.basename(path.dirname(source))}/${path.basename(source)}`;
    row.representation = 'user-supplied-front-unverified-provenance';
  }
  // The independent lit collection must remain intact.
  if (row.lit) assert(sha(await fs.readFile(path.join(output, row.lit))) === row.litSha256, `Lit artwork changed: ${row.asset}`);
}
if (!verify) await write(path.join(output, 'index.json'), index);
console.log(`PASS: ${records.length} imported fronts ${verify ? 'verified' : 'copied byte-for-byte'}; lit artwork unchanged.`);