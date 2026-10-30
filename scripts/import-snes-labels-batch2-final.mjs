import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const target = path.join(root, 'app/src/main/assets/snes_labels');
const indexPath = path.join(target, 'index.json');
const sourcePack = 'game-labels-batch2-final';
const aiRepresentation = 'user-supplied-ai-generated-recreation';
const originalStems = new Set([
    'aladdin-japan',
    'aladdin-usa',
    'donkeykongcountry-usa',
    'gooftroop-usa',
    'kirbysdreamland3-usa',
    'mortalkombat-usa',
    'supermarioallstars-usa',
    'supermariokart-japan',
    'supermariorpglegendofthesevenstars-japan',
    'supermariorpglegendofthesevenstars-usa',
    'supermarioworld-usa',
    'supermarioworld2yoshisisland-usa',
    'supermetroid-usa',
    'thelegendofzeldaalinktothepast-usa',
    'thelionking-usa'
]);
const newLabels = new Map([
    ['megaman10', 'Mega Man 10'],
    ['megaman12', 'Mega Man 12'],
    ['megaman13', 'Mega Man 13']
]);
const require = createRequire(path.resolve(process.env.ARTWORK_TOOLS || '/tmp/retro-art-handoff-tools', 'package.json'));
const sharp = require('sharp');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const check = (condition, message) => { if (!condition) throw Error(message); };
const readIndex = async () => JSON.parse(await fs.readFile(indexPath, 'utf8'));

const sourceDir = process.argv.slice(2).find(arg => !arg.startsWith('--'));
check(sourceDir, 'Pass the game_labels_batch2_final/labels folder.');
const source = path.resolve(sourceDir);
const notes = await fs.readFile(path.join(path.dirname(source), 'README.txt'));
const files = (await fs.readdir(source)).filter(file => file.endsWith('.jpg')).sort();
check(files.length === 61, `Expected 61 JPEG labels, found ${files.length}`);
check(files.every(file => /^[a-z0-9_-]+\.jpg$/.test(file)), 'Unexpected filename in supplied label folder.');
check([...newLabels].every(([stem, title]) => notes.toString().includes(`${stem}.jpg`) &&
    title.toLowerCase().replace(/[^a-z0-9]+/g, '') === stem), 'README does not document the three added labels.');
const index = await readIndex();
const rowsByStem = new Map(index.labels.map(row => [
    path.basename(row.path, path.extname(row.path)),
    row
]));

if (process.argv.includes('--verify')) {
    const labels = index.labels.filter(label => label.sourcePack === sourcePack);
    check(labels.length === 61, `Expected 61 final-batch labels, found ${labels.length}`);
    check(labels.filter(label => label.representation === aiRepresentation).length === 43,
        'Expected 43 AI recreations.');
    check(labels.filter(label => originalStems.has(path.basename(label.path, '.jpg'))).length === 15,
        'Expected 15 retained originals.');
    check(labels.filter(label => newLabels.has(path.basename(label.path, '.jpg'))).length === 3,
        'Expected three newly supplied labels.');
    check(index.sources[sourcePack]?.readmeSha256 === hash(notes), 'Source README hash mismatch.');
    for (const file of files) {
        const label = rowsByStem.get(path.basename(file, '.jpg'));
        check(label?.sourcePack === sourcePack && label.path === `labels/${file}`,
            `Missing catalog entry: ${file}`);
        const [sourceBytes, assetBytes] = await Promise.all([
            fs.readFile(path.join(source, file)),
            fs.readFile(path.join(target, label.path))
        ]);
        check(hash(sourceBytes) === hash(assetBytes) && hash(assetBytes) === label.sha256,
            `Imported bytes differ: ${file}`);
        const metadata = await sharp(assetBytes).metadata();
        check(metadata.format === 'jpeg' && metadata.width === label.width && metadata.height === label.height,
            `Invalid image metadata: ${file}`);
        if (newLabels.has(path.basename(file, '.jpg'))) {
            check(label.representation === 'user-supplied-label' && !label.templateRegion,
                `New label must not claim an unprovided region: ${file}`);
        }
        try {
            await fs.access(path.join(target, 'labels', `${path.basename(file, '.jpg')}.png`));
            throw Error(`Obsolete PNG still exists: ${file}`);
        } catch (error) {
            if (error.code !== 'ENOENT') throw error;
        }
    }
    console.log(`PASS: ${labels.length} final-batch labels, image bytes, metadata and source notes verified.`);
} else {
    const replacements = new Map();
    for (const file of files) {
        const stem = path.basename(file, '.jpg');
        const row = rowsByStem.get(stem);
        if (newLabels.has(stem)) {
            check(!row, `Unexpected existing catalog entry for new label: ${file}`);
            replacements.set(stem, null);
        } else {
            check(row && (row.sourcePack === 'game-labels-batch2' || row.sourcePack === sourcePack),
                `No existing second-batch label matches: ${file}`);
            replacements.set(stem, row);
        }
    }
    check(replacements.size === 61, 'Supplied files do not match the final label set.');

    await fs.mkdir(path.join(target, 'labels'), { recursive: true });
    for (const file of files) {
        const stem = path.basename(file, '.jpg');
        let row = replacements.get(stem);
        const bytes = await fs.readFile(path.join(source, file));
        const metadata = await sharp(bytes).metadata();
        check(metadata.format === 'jpeg' && metadata.width && metadata.height, `Invalid JPEG: ${file}`);
        await fs.copyFile(path.join(source, file), path.join(target, 'labels', file));
        if (!row) {
            row = {
                romTitle: newLabels.get(stem),
                path: `labels/${file}`,
                representation: 'user-supplied-label',
                authenticatedOriginalScan: false,
                sourceFile: `game_labels_batch2_final/labels/${file}`,
                sourceDescription: 'User-supplied label artwork; no source URL or region was supplied.',
                sourceTitle: null,
                sourceUrl: null,
                sourceRole: 'User-supplied artwork matched by title only; no release-region claim.',
                note: 'Title follows the device ROM filename; region was not provided.'
            };
            replacements.set(stem, row);
            index.labels.push(row);
        }
        Object.assign(row, {
            path: `labels/${file}`,
            sourcePack,
            sha256: hash(bytes),
            width: metadata.width,
            height: metadata.height,
            bytes: bytes.length
        });
        if (newLabels.has(stem)) {
            row.representation = 'user-supplied-label';
            row.authenticatedOriginalScan = false;
        } else if (!originalStems.has(stem)) {
            row.representation = aiRepresentation;
            row.sourceRole = 'AI-generated artwork; existing scan sources are reference context, not verification of output pixels.';
            row.generationNote = 'AI recreation from the supplied image batch; lettering, illustration and small logos may differ.';
        }
    }

    for (const label of index.labels) {
        if (label.sourcePack !== sourcePack && (label.sourcePack === 'supplied-jpeg-pack' ||
            ['user-supplied-ai-generated-recreation', 'user-supplied-composite-image-extract']
                .includes(label.representation) && label.path.endsWith('.jpg'))) {
            label.sourcePack = 'supplied-jpeg-pack';
        }
    }
    index.sources = {
        ...index.sources,
        [sourcePack]: {
            representation: '58 updated labels (43 AI recreations and 15 retained originals) plus three supplied labels for games previously missing artwork.',
            processing: 'High-quality JPEG files copied byte-for-byte. AI may alter artwork and small lettering; the three added labels have no supplied region or source URL.',
            readmeSha256: hash(notes)
        }
    };
    await fs.writeFile(indexPath, JSON.stringify(index, null, 2) + '\n');
    for (const file of files) {
        await fs.rm(path.join(target, 'labels', `${path.basename(file, '.jpg')}.png`), { force: true });
    }
    console.log(`Imported ${files.length} final SNES labels into the unified catalog.`);
}
