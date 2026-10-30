import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(path.resolve(process.env.ARTWORK_TOOLS || '/tmp/retro-art-handoff-tools', 'package.json'));
const sharp = require('sharp');
const cheerio = require('cheerio');
const sourcePage = 'https://www.vgmuseum.com/scans/snes.html';
const staging = process.env.SNES_SCAN_CACHE || '/tmp/retro-vgmuseum-snes';
const output = path.join(root, 'app/src/main/assets/snes_labels');
const reviewPath = path.join(root, 'artwork/snes/cartridge-scans.json');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const check = (condition, message) => { if (!condition) throw Error(message); };
const write = async (file, value) => {
  await fs.mkdir(path.dirname(file), { recursive: true });
  await fs.writeFile(file, JSON.stringify(value, null, 2) + '\n');
};
async function download(url) {
  check(new URL(url).origin === 'https://www.vgmuseum.com', 'Unexpected source host');
  const response = await fetch(url, { signal: AbortSignal.timeout(30000) });
  check(response.ok && new URL(response.url).origin === new URL(url).origin, `Download failed: ${url}`);
  const bytes = Buffer.from(await response.arrayBuffer());
  check(bytes.length <= 16 * 1024 * 1024, 'Oversized source scan');
  return bytes;
}
if (process.argv.includes('--inventory')) {
  await fs.mkdir(staging, { recursive: true });
  const $ = cheerio.load(await download(sourcePage));
  const rows = [];
  let title = '';
  let candidateIndex = 0;
  for (const anchor of $('a[href]').toArray()) {
    const text = $(anchor).text().trim();
    const href = $(anchor).attr('href');
    if (!/\.jpe?g$/i.test(href)) continue;
    if (!text.startsWith('(')) title = text;
    if (!/\bCart\b/i.test(text) || /\bBack\b/i.test(text) || /Game Genie/i.test(title)) continue;
    const index = candidateIndex++;
    const url = new URL(href, sourcePage).href;
    const file = path.join(staging, `${index}.jpg`);
    let bytes;
    try { bytes = await fs.readFile(file); } catch {
      try { bytes = await download(url); } catch (error) { console.warn(`Skipped ${index}: ${error.message}`); continue; }
      await fs.writeFile(file, bytes);
    }
    const metadata = await sharp(bytes).metadata();
    rows.push({ index, title, type: text, url, sourceSha256: sha(bytes), width: metadata.width, height: metadata.height });
  }
  await write(path.join(staging, 'inventory.json'), rows);
  for (let offset = 0; offset < rows.length; offset += 24) {
    const tiles = await Promise.all(rows.slice(offset, offset + 24).map(async (row, position) => {
      const picture = await sharp(path.join(staging, `${row.index}.jpg`)).rotate()
        .resize(240, 150, { fit: 'contain', background: '#eeeeee' }).png().toBuffer();
      const caption = Buffer.from(`<svg width="240" height="24"><rect width="240" height="24" fill="white"/><text x="4" y="17" font-size="14">${row.index}</text></svg>`);
      const tile = await sharp({ create: { width: 240, height: 174, channels: 3, background: '#eeeeee' } })
        .composite([{ input: picture }, { input: caption, top: 150, left: 0 }]).png().toBuffer();
      return { input: tile, left: position % 4 * 240, top: Math.floor(position / 4) * 174 };
    }));
    await sharp({ create: { width: 960, height: 1044, channels: 3, background: '#eeeeee' } }).composite(tiles)
      .png().toFile(path.join(staging, `review-${offset}.png`));
  }
  console.log(`Staged ${rows.length} cartridge-front scans, no boxes/manuals, in ${staging}.`);
} else if (process.argv.includes('--freeze-review')) {
  const review = JSON.parse(await fs.readFile(reviewPath, 'utf8'));
  const inventory = JSON.parse(await fs.readFile(path.join(staging, 'inventory.json'), 'utf8'));
  for (const row of review.labels) {
    const source = inventory.find(source => source.index === row.index);
    check(source && /\bCart\b/i.test(source.type), 'Not a cartridge scan');
    Object.assign(row, { id: row.romTitle.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/-$/, ''),
      sourceTitle: source.title, sourceKind: source.type, url: source.url, sourceSha256: source.sourceSha256 });
  }
  await write(reviewPath, review);
  console.log(`Pinned ${review.labels.length} reviewed cartridge scans.`);
} else if (process.argv.includes('--verify')) {
  const index = JSON.parse(await fs.readFile(path.join(output, 'index.json'), 'utf8'));
  const review = JSON.parse(await fs.readFile(reviewPath, 'utf8'));
  const labels = index.labels.filter(row => row.sourcePack === 'vgmuseum-scan-crops');
  check(labels.length === review.labels.length, 'Review/index count mismatch');
  for (const row of labels) {
    check(/^labels\/[a-z0-9_-]+\.jpg$/.test(row.path), 'Unsafe asset path');
    const bytes = await fs.readFile(path.join(output, row.path));
    const metadata = await sharp(bytes).metadata();
    check(sha(bytes) === row.sha256 && bytes.length === row.bytes, `Changed asset: ${row.path}`);
    check(metadata.width === row.width && metadata.height === row.height && Math.max(row.width, row.height) <= 768, 'Invalid normalized dimensions');
    check(!metadata.exif && !metadata.icc && metadata.format === 'jpeg', 'Unexpected metadata or format');
    const source = review.labels.find(source => source.id === row.id);
    check(source?.sourceSha256 === row.sourceSha256 && source.url === row.sourceUrl && source.romTitle === row.romTitle &&
      JSON.stringify(source.crop) === JSON.stringify(row.crop), 'Provenance/crop mismatch');
    check(row.width <= row.crop.width && row.height <= row.crop.height &&
      Math.abs(row.width / row.height - row.crop.width / row.crop.height) < 0.02, 'Upscaled or distorted label');
  }
  console.log(`PASS: ${index.labels.length} bundled cartridge-label crops verified offline.`);
} else {
  const review = JSON.parse(await fs.readFile(reviewPath, 'utf8'));
  await fs.mkdir(path.join(output, 'labels'), { recursive: true });
  const labels = [];
  for (const row of review.labels) {
    check(/^[a-z0-9_-]+$/.test(row.id), 'Unsafe label ID');
    let bytes;
    try { bytes = await fs.readFile(path.join(staging, `${row.index}.jpg`)); } catch { bytes = await download(row.url); }
    check(sha(bytes) === row.sourceSha256, `Changed source: ${row.url}`);
    const original = await sharp(bytes).metadata();
    const normalized = await sharp(bytes).extract(row.crop).toColourspace('srgb')
      .resize({ width: 768, height: 768, fit: 'inside', withoutEnlargement: true })
      .jpeg({ quality: 85, chromaSubsampling: '4:4:4', mozjpeg: true }).toBuffer();
    const metadata = await sharp(normalized).metadata();
    const target = `labels/${row.id}.jpg`;
    await fs.writeFile(path.join(output, target), normalized);
    labels.push({ id: row.id, romTitle: row.romTitle, path: target,
      sourcePack: 'vgmuseum-scan-crops', representation: 'reviewed-physical-cartridge-scan-crop',
      sha256: sha(normalized), width: metadata.width, height: metadata.height,
      bytes: normalized.length, sourceUrl: row.url, sourceSha256: row.sourceSha256,
      sourceWidth: original.width, sourceHeight: original.height, crop: row.crop });
  }
  const index = JSON.parse(await fs.readFile(path.join(output, 'index.json'), 'utf8'));
  const replacedPaths = new Set(labels.map(row => row.path));
  const replacedTitles = new Set(labels.map(row => row.romTitle));
  const existingLabels = index.labels.filter(row =>
    !replacedPaths.has(row.path) && !replacedTitles.has(row.romTitle));
  const fallbackLabels = existingLabels.filter(row =>
    ['game-labels-batch2', 'game-labels-batch2-final', 'supplied-png-pack'].includes(row.sourcePack));
  const primaryLabels = existingLabels.filter(row => !fallbackLabels.includes(row));
  index.labels = [...primaryLabels, ...labels, ...fallbackLabels];
  index.sources = {
    ...index.sources,
    'vgmuseum-scan-crops': {
      sourcePage,
      representation: 'Reviewed label-only crops of physical cartridge scans, not box art; title/region matching is not exact-ROM authentication.',
      processing: 'Reviewed original-pixel crop; sRGB JPEG quality 85, 4:4:4; max768px; no upscaling; metadata removed.'
    }
  };
  await write(path.join(output, 'index.json'), index);
  console.log(`Prepared ${labels.length} bundled cartridge labels.`);
}