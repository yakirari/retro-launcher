import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const target = path.join(root, 'app/src/main/assets/snes_labels');
const indexPath = path.join(target, 'index.json');
const provenance = path.join(root, 'artwork/snes/restored');
const require = createRequire(path.resolve(process.env.ARTWORK_TOOLS || '/tmp/retro-art-handoff-tools', 'package.json'));
const sharp = require('sharp');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const readIndex = async () => JSON.parse(await fs.readFile(indexPath, 'utf8'));
const parseCsv = text => {
    const records = [];
    let record = [];
    let field = '';
    let quoted = false;
    let afterQuote = false;
    for (let i = 0; i < text.length; i++) {
        const character = text[i];
        if (quoted) {
            if (character === '"' && text[i + 1] === '"') {
                field += '"';
                i++;
            } else if (character === '"') {
                quoted = false;
                afterQuote = true;
            } else {
                field += character;
            }
        } else if (afterQuote) {
            if (character === ',') {
                record.push(field);
                field = '';
                afterQuote = false;
            } else if (character === '\n' || character === '\r') {
                record.push(field);
                records.push(record);
                record = [];
                field = '';
                afterQuote = false;
                if (character === '\r' && text[i + 1] === '\n') i++;
            } else {
                assert.fail(`Unexpected character after quoted CSV field: ${character}`);
            }
        } else if (character === '"') {
            assert.equal(field, '', 'Quote must begin at the start of a CSV field.');
            quoted = true;
        } else if (character === ',') {
            record.push(field);
            field = '';
        } else if (character === '\n' || character === '\r') {
            record.push(field);
            records.push(record);
            record = [];
            field = '';
            if (character === '\r' && text[i + 1] === '\n') i++;
        } else {
            field += character;
        }
    }
    assert.equal(quoted, false, 'Unclosed quoted CSV field.');
    if (record.length || field || afterQuote) {
        record.push(field);
        records.push(record);
    }
    return records;
};

if (process.argv.includes('--verify')) {
    const manifest = await readIndex();
    const labels = manifest.labels.filter(label => label.sourcePack === 'supplied-png-pack');
    assert.equal(labels.length, 58);
    assert.deepEqual(Object.fromEntries(['USA', 'Japan']
        .map(region => [region, labels.filter(label => label.templateRegion === region).length])), { USA: 44, Japan: 14 });
    assert.equal(new Set(labels.map(label => label.path)).size, labels.length);
    for (const label of labels) {
        assert(/^labels\/[a-z0-9_-]+\.png$/.test(label.path));
        const bytes = await fs.readFile(path.join(target, label.path));
        assert.equal(hash(bytes), label.sha256, label.path);
        const metadata = await sharp(bytes).metadata();
        assert.equal(metadata.width, label.width);
        assert.equal(metadata.height, label.height);
        assert(['user-supplied-restoration', 'user-supplied-label', 'user-supplied-original-scan',
            'user-supplied-cartridge-photo'].includes(label.representation));
        assert.equal(label.authenticatedOriginalScan, false);
    }
    assert.equal(hash(await fs.readFile(path.join(provenance, 'source_manifest.csv'))),
        manifest.sources['supplied-png-pack'].sourceManifestSha256);
    console.log(`PASS: ${labels.length} supplied SNES labels, original bytes/dimensions and provenance verified.`);
    process.exit(0);
}

assert(process.argv[2], 'Pass the supplied label folder, or --verify.');
const source = path.resolve(process.argv[2]);
const csv = await fs.readFile(path.join(source, 'source_manifest.csv'));
const records = parseCsv(csv.toString('utf8').replace(/^\uFEFF/, '').trimEnd());
const headings = records.shift();
const supportedHeadings = [
    ['game', 'original_filename', 'USA', 'Japan', 'source_title', 'source_url'],
    ['game', 'original_filename', 'USA', 'Japan', 'source_title', 'source_url',
        'USA_source_url', 'Japan_source_url']
];
assert(supportedHeadings.some(supported => JSON.stringify(supported) === JSON.stringify(headings)),
    `Unsupported source manifest columns: ${headings.join(',')}`);
const rows = records.map(fields => {
    assert.equal(fields.length, headings.length);
    return Object.fromEntries(headings.map((heading, index) => [heading, fields[index]]));
});
const labels = [];
const copies = [];
for (const region of ['USA', 'Japan']) {
    for (const file of (await fs.readdir(path.join(source, `labels_${region}`))).sort()) {
        assert(/^[a-z0-9-]+\.png$/.test(file), `Unexpected label filename: ${file}`);
        const matches = rows.filter(row => row.original_filename === file);
        assert.equal(matches.length, 1, `Missing/ambiguous provenance: ${file}`);
        const row = matches[0];
        assert(/^[a-z0-9]+$/.test(row.game));
        const sourceDescription = row[region]?.trim() ?? '';
        const hasRegionalSource = sourceDescription !== '' && !/^not available from Santiago$/i.test(sourceDescription);
        const bytes = await fs.readFile(path.join(source, `labels_${region}`, file));
        const metadata = await sharp(bytes).metadata();
        assert.equal(metadata.format, 'png');
        const output = `${row.game}-${region.toLowerCase()}.png`;
        const representation = /original cartridge scan/i.test(sourceDescription)
            ? 'user-supplied-original-scan'
            : /cartridge photo/i.test(sourceDescription)
                ? 'user-supplied-cartridge-photo'
                : /restoration/i.test(sourceDescription)
                    ? 'user-supplied-restoration'
                    : 'user-supplied-label';
        labels.push({
            romTitle: `${row.game} (${region})`, path: `labels/${output}`, sourcePack: 'supplied-png-pack',
            width: metadata.width, height: metadata.height, sha256: hash(bytes), bytes: bytes.length,
            templateRegion: region,
            representation,
            authenticatedOriginalScan: false,
            sourceFile: `labels_${region}/${file}`,
            sourceDescription: hasRegionalSource ? sourceDescription
                : 'User-supplied regional label; no source attribution for this image in the supplied CSV.',
            sourceTitle: hasRegionalSource ? row.source_title : null,
            sourceUrl: hasRegionalSource
                ? row[`${region}_source_url`]?.trim() || row.source_url?.trim() || null
                : null,
            note: region === 'Japan' && row.game === 'supermariorpglegendofthesevenstars'
                ? 'User-assigned Japan shell; supplied print has Spanish text and a USA product code. Not a verified Japanese release label.'
                : 'Shell assignment follows the supplied folder, not independently authenticated ROM region.'
        });
        copies.push({output, bytes});
    }
}
assert(labels.length > 0, 'No supplied labels found.');
assert.equal(new Set(labels.map(label => label.path)).size, labels.length, 'Duplicate output labels.');
const counts = Object.fromEntries(['USA', 'Japan']
    .map(region => [region, labels.filter(label => label.templateRegion === region).length]));
await fs.mkdir(target, {recursive: true});
await fs.mkdir(path.join(target, 'labels'), {recursive: true});
await fs.mkdir(provenance, {recursive: true});
for (const copy of copies) await fs.writeFile(path.join(target, 'labels', copy.output), copy.bytes);
await fs.writeFile(path.join(provenance, 'source_manifest.csv'), csv);
await fs.copyFile(path.join(source, 'README.md'), path.join(provenance, 'source-notes.md'));
const index = await readIndex();
index.sources = {
    ...index.sources,
    'supplied-png-pack': {
        credit: 'User-supplied label pack. Per-file provenance distinguishes restorations, original scan sources and labels with no source attribution.',
        processing: 'User-supplied label-only PNGs copied byte-for-byte; full-cartridge previews excluded.',
        sourceManifestSha256: hash(csv),
        counts
    }
};
index.labels = [
    ...index.labels.filter(label => !label.path.endsWith('.png')),
    ...labels
];
await fs.writeFile(indexPath, JSON.stringify(index, null, 2) + '\n');
console.log(`Imported ${counts.USA} USA and ${counts.Japan} Japan label-only PNGs. Original images and fixed cartridge windows unchanged.`);
