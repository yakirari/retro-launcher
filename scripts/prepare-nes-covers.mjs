import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(path.resolve(process.env.ARTWORK_TOOLS || '/tmp/retro-art-handoff-tools', 'package.json'));
const sharp = require('sharp');
const cheerio = require('cheerio');
const sourcePage = 'https://www.vgmuseum.com/scans/nes.html';
const staging = process.env.NES_COVER_CACHE || '/tmp/retro-vgmuseum-nes';
const output = path.join(root, 'app/src/main/assets/nes_covers');
const reviewPath = path.join(root, 'artwork/nes/vgmuseum.json');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const write = async (file, value) => {
  await fs.mkdir(path.dirname(file), { recursive: true });
  await fs.writeFile(file, JSON.stringify(value, null, 2) + '\n');
};
const check = (condition, message) => { if (!condition) throw Error(message); };
async function download(url) {
  const parsed = new URL(url);
  check(parsed.protocol === 'https:' && parsed.hostname === 'www.vgmuseum.com', 'Unexpected source host');
  const response = await fetch(url, { signal: AbortSignal.timeout(30000) });
  check(response.ok && new URL(response.url).hostname === parsed.hostname, `Download failed: ${url}`);
  const bytes = Buffer.from(await response.arrayBuffer());
  check(bytes.length <= 16 * 1024 * 1024, `Oversized source: ${url}`);
  return bytes;
}
if (process.argv.includes('--inventory')) {
  await fs.mkdir(staging, { recursive: true });
  const html = await download(sourcePage);
  const $ = cheerio.load(html);
  const rows = $('a[href]').toArray().filter(anchor => /\.jpe?g$/i.test($(anchor).attr('href')) &&
    !/\b(Back|Cart|Manual)\b/i.test($(anchor).text()) && !/^\s*\(/.test($(anchor).text()))
    .map(anchor => ({ title: $(anchor).text().trim(), url: new URL($(anchor).attr('href'), sourcePage).href }));
  for (let index = 0; index < rows.length; index++) {
    const row = rows[index];
    const file = path.join(staging, `${index}.jpg`);
    let bytes;
    try { bytes = await fs.readFile(file); } catch { bytes = await download(row.url); await fs.writeFile(file, bytes); }
    const metadata = await sharp(bytes).metadata();
    Object.assign(row, { index, sourceSha256: sha(bytes), sourceWidth: metadata.width, sourceHeight: metadata.height });
  }
  await write(path.join(staging, 'inventory.json'), rows);
  for (let offset = 0; offset < rows.length; offset += 40) {
    const tiles = await Promise.all(rows.slice(offset, offset + 40).map(async row => ({
      input: await sharp(path.join(staging, `${row.index}.jpg`)).rotate().resize(140, 200, { fit: 'contain', background: '#dddddd' }).png().toBuffer(),
      left: (row.index - offset) % 8 * 140, top: Math.floor((row.index - offset) / 8) * 200,
    })));
    await sharp({ create: { width: 1120, height: 1000, channels: 3, background: '#dddddd' } }).composite(tiles)
      .png().toFile(path.join(staging, `review-${offset}.png`));
  }
  console.log(`Inventoried ${rows.length} box-front candidates in ${staging}; nothing bundled.`);
} else if (process.argv.includes('--freeze-review')) {
  const review = JSON.parse(await fs.readFile(reviewPath, 'utf8'));
  const inventory = JSON.parse(await fs.readFile(path.join(staging, 'inventory.json'), 'utf8'));
  for (const row of review.covers) {
    const source = inventory[row.index];
    check(source && source.index === row.index, 'Missing reviewed inventory row');
    Object.assign(row, { sourceTitle: source.title, url: source.url, sourceSha256: source.sourceSha256 });
  }
  await write(reviewPath, review);
  console.log(`Pinned ${review.covers.length} reviewed sources.`);
} else if (process.argv.includes('--verify')) {
  const index = JSON.parse(await fs.readFile(path.join(output, 'index.json'), 'utf8'));
  const review = JSON.parse(await fs.readFile(reviewPath, 'utf8'));
  check(index.covers.length === review.covers.length, 'Review/index count mismatch');
  for (const row of index.covers) {
    check(/^front\/[a-z0-9_-]+\.jpg$/.test(row.path), 'Unsafe asset path');
    const bytes = await fs.readFile(path.join(output, row.path));
    const metadata = await sharp(bytes).metadata();
    check(sha(bytes) === row.sha256 && bytes.length === row.bytes, `Changed asset: ${row.path}`);
    check(metadata.width === row.width && metadata.height === row.height && Math.max(row.width, row.height) <= 768, 'Invalid normalized dimensions');
    check(!metadata.exif && !metadata.icc && metadata.format === 'jpeg', 'Unexpected embedded metadata or encoding');
    const source = review.covers.find(source => source.romTitle === row.romTitle);
    check(source?.sourceSha256 === row.sourceSha256 && source.url === row.sourceUrl, 'Source provenance mismatch');
  }
  console.log(`PASS: ${index.covers.length} bundled covers, hashes/dimensions/provenance verified offline.`);
} else {
  const review = JSON.parse(await fs.readFile(reviewPath, 'utf8'));
  await fs.mkdir(path.join(output, 'front'), { recursive: true });
  const covers = [];
  for (const row of review.covers) {
    check(/^[a-z0-9_-]+$/.test(row.id), 'Unsafe cover ID');
    let bytes;
    try { bytes = await fs.readFile(path.join(staging, `${row.index}.jpg`)); } catch { bytes = await download(row.url); }
    check(sha(bytes) === row.sourceSha256, `Source changed: ${row.url}`);
    const original = await sharp(bytes).metadata();
    const normalized = await sharp(bytes).rotate().toColourspace('srgb')
      .resize({ width: 768, height: 768, fit: 'inside', withoutEnlargement: true })
      .jpeg({ quality: 85, mozjpeg: true, chromaSubsampling: '4:4:4' }).toBuffer();
    const metadata = await sharp(normalized).metadata();
    const target = `front/${row.id}.jpg`;
    await fs.writeFile(path.join(output, target), normalized);
    covers.push({ romTitle: row.romTitle, path: target, sha256: sha(normalized), width: metadata.width, height: metadata.height,
      bytes: normalized.length, sourceUrl: row.url, sourceSha256: row.sourceSha256,
      sourceWidth: original.width, sourceHeight: original.height });
  }
  await write(path.join(output, 'index.json'), { version: 1, sourcePage,
    credit: 'Video Game Museum (vgmuseum.com) and its scan contributors; original game artwork belongs to its respective rightsholders.',
    processing: 'Full image; EXIF orientation; sRGB; max 768px long edge; no upscaling/cropping; JPEG quality 85, 4:4:4; metadata stripped.', covers });
  console.log(`Prepared ${covers.length} reviewed covers.`);
}