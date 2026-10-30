import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const target = path.join(root, 'app/src/main/assets/snes_labels');
const indexPath = path.join(target, 'index.json');
const require = createRequire(path.resolve(process.env.ARTWORK_TOOLS || '/tmp/retro-art-handoff-tools', 'package.json'));
const sharp = require('sharp');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const aiRepresentation = 'user-supplied-ai-generated-recreation';
const compositeRepresentation = 'user-supplied-composite-image-extract';
const generationNote = 'AI recreation based on original label references; lettering, illustration and small logos may differ.';
const referenceRole = 'reference-only; generated artwork is not a pixel crop of this source';
const compositeNote = 'Extracted from the user-supplied composite image; historical source/crop metadata is reference context, not independent verification of these pixels.';
const cleanedFiles = new Set([
    'addams-family-the-europe.jpg',
    'doom-europe.jpg',
    'mickey-mania-the-timeless-adventures-of-mickey-mouse-europe.jpg',
    'mortal-kombat-europe.jpg',
    'wario-s-woods-europe.jpg',
    'x-men-mutant-apocalypse-usa.jpg'
]);
const check = (condition, message) => { if (!condition) throw Error(message); };
const readIndex = async () => JSON.parse(await fs.readFile(indexPath, 'utf8'));

if (process.argv.includes('--verify')) {
    const index = await readIndex();
    const labels = index.labels.filter(label => label.sourcePack === 'supplied-jpeg-pack' &&
        [aiRepresentation, compositeRepresentation].includes(label.representation));
    check(labels.length === 48, `Expected 48 supplied labels, found ${labels.length}`);
    check(labels.filter(label => label.representation === aiRepresentation).length === 42, 'Expected 42 AI recreations');
    check(labels.filter(label => label.representation === compositeRepresentation).length === 6, 'Expected 6 composite-image extracts');
    for (const label of labels) {
        if (label.representation === aiRepresentation) {
            check(label.sourceRole === referenceRole && label.generationNote === generationNote, `Missing generation note: ${label.path}`);
        } else {
            check(label.sourceRole === compositeNote, `Missing composite-image note: ${label.path}`);
        }
        const bytes = await fs.readFile(path.join(target, label.path));
        const metadata = await sharp(bytes).metadata();
        check(hash(bytes) === label.sha256 && bytes.length === label.bytes, `Changed generated label: ${label.path}`);
        check(metadata.format === 'jpeg' && metadata.width === label.width && metadata.height === label.height,
            `Invalid generated label metadata: ${label.path}`);
    }
    console.log(`PASS: ${labels.length} supplied SNES labels and their index metadata verified.`);
} else {
    const source = process.argv[2];
    check(source && !source.startsWith('--'), 'Pass the supplied labels folder, or --verify.');
    const sourceDir = path.resolve(source);
    const index = await readIndex();
    const entries = new Map(index.labels.map(label => [path.basename(label.path), label]));
    const files = (await fs.readdir(sourceDir)).filter(file => file.endsWith('.jpg')).sort();
    check(files.length === 48, `Expected 48 supplied JPEGs, found ${files.length}`);
    check(cleanedFiles.size === 6 && [...cleanedFiles].every(file => files.includes(file)),
        'The supplied cleaned-scan filenames do not match the expected set.');
    check(files.every(file => entries.has(file)), 'The supplied filenames do not match the SNES catalog.');

    for (const file of files) {
        const row = entries.get(file);
        const bytes = await fs.readFile(path.join(sourceDir, file));
        const metadata = await sharp(bytes).metadata();
        check(metadata.format === 'jpeg' && metadata.width && metadata.height, `Invalid JPEG: ${file}`);
        await fs.copyFile(path.join(sourceDir, file), path.join(target, row.path));
        const isCompositeExtract = cleanedFiles.has(file);
        Object.assign(row, {
            sourcePack: 'supplied-jpeg-pack',
            sha256: hash(bytes),
            width: metadata.width,
            height: metadata.height,
            bytes: bytes.length,
            representation: isCompositeExtract ? compositeRepresentation : aiRepresentation,
            sourceRole: isCompositeExtract ? compositeNote : referenceRole
        });
        if (isCompositeExtract) {
            delete row.generationNote;
        } else {
            row.generationNote = generationNote;
        }
    }

    index.sources = {
        ...index.sources,
        'supplied-jpeg-pack': {
            sourcePage: 'https://www.vgmuseum.com/scans/snes.html',
            representation: '42 user-supplied AI-generated label recreations and 6 user-supplied composite-image extracts; title/region matching is not exact-ROM authentication.',
            processing: 'All 48 JPEGs are bundled byte-for-byte from the supplied all_game_labels_complete/labels folder. The six composite extracts retain historical source references as context; AI recreations may differ in lettering, illustration and small logos.'
        }
    };
    await fs.writeFile(indexPath, JSON.stringify(index, null, 2) + '\n');
    console.log(`Imported ${files.length} supplied SNES labels.`);
}
