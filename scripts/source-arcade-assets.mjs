/** Reproducible finite metadata + bounded ArcadeItalia sourcing. Node 18+, no Python.
 * Usage: node scripts/source-arcade-assets.mjs /tmp/mame2003-plus.xml /tmp/arcade-device-inventory.txt
 * Add --download to source cabinets. Raw new images stay in downloads/, never automatic build inputs.
 * Generated text goes to /tmp/arcade-generated for review, not directly into source code.
 */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const root = path.resolve(import.meta.dirname ?? path.dirname(new URL(import.meta.url).pathname), '..');
const [xmlPath, inventoryPath] = process.argv.slice(2);
if (!xmlPath || !inventoryPath) throw new Error('Expected XML and adb inventory paths');
const out = '/tmp/arcade-generated';
fs.mkdirSync(out, { recursive: true });
const decode = s => s.replace(/&#x([0-9a-f]+);|&#(\d+);|&(amp|lt|gt|quot|apos);/gi,
    (_, hex, dec, name) => hex ? String.fromCodePoint(parseInt(hex, 16)) : dec ? String.fromCodePoint(+dec) :
        ({ amp: '&', lt: '<', gt: '>', quot: '"', apos: "'" }[name]));
const xml = fs.readFileSync(xmlPath, 'utf8');
const sets = new Map();
for (const m of xml.matchAll(/<game\s+([^>]+)>([\s\S]*?)<\/game>/g)) {
    const attrs = Object.fromEntries([...m[1].matchAll(/(\w+)="([^"]*)"/g)].map(x => [x[1], decode(x[2])]));
    const text = tag => decode(m[2].match(new RegExp(`<${tag}>([\\s\\S]*?)<\\/${tag}>`))?.[1] ?? '').trim();
    sets.set(attrs.name, { rom: attrs.name, title: text('description'), year: text('year'),
        manufacturer: text('manufacturer'), parent: attrs.cloneof ?? '' });
}
if (sets.size < 4000) throw new Error('Unexpected XML coverage');
// MAME2003 aburner.c explicitly declares thndrbdj cloneof thndrbld (Japan).
// MAME2003-plus segaxbd.c calls the corresponding deluxe/standing set thndrbld1.
sets.set('thndrbdj', { ...sets.get('thndrbld1'), rom: 'thndrbdj', parent: 'thndrbld' });
const inventoryFiles = fs.readFileSync(inventoryPath, 'utf8').trim().split(/\r?\n/);
const inventory = [...new Set(inventoryFiles.map(x => path.basename(x).replace(/\.zip$/i, '').replace(/ \([0-9]+\)$/, '').toLowerCase()))].sort();
const catalogPath = path.join(root, 'app/src/main/java/com/ariy/retrolauncher/launch/ArcadeGameCatalog.kt');
const old = fs.readFileSync(catalogPath, 'utf8');
// Initial seed survives in a checked-in JSON alongside generated metadata on later runs.
const seedPath = path.join(root, 'app/src/main/assets/arcade-metadata/catalog-seed.json');
const seed = fs.existsSync(seedPath) ? JSON.parse(fs.readFileSync(seedPath, 'utf8')) :
    [...old.matchAll(/Game\("([^"]+)", "([^"]+)"(?:, listOf\(([^)]*)\))?\)/g)].map(m =>
        ({ rom: m[1], title: m[2], clones: [...(m[3] ?? '').matchAll(/"([^"]+)"/g)].map(x => x[1]) }));
if (!seed.length) throw new Error('Missing existing identity seed');
const curated = new Map(seed.map(x => [x.rom, x]));
const oldAliases = new Map(seed.flatMap(x => x.clones.map(a => [a, x.rom])));
const names = new Set([...inventory, ...seed.flatMap(x => [x.rom, ...x.clones])]);
for (const name of [...names]) {
    let s = sets.get(name);
    const seen = new Set();
    while (s?.parent && !seen.has(s.parent)) { seen.add(s.parent); names.add(s.parent); s = sets.get(s.parent); }
}
const missing = [...names].filter(n => !sets.has(n) && !oldAliases.has(n));
if (missing.length) throw new Error(`Unresolved metadata: ${missing}`);
const baseTitle = title => title.replace(/\s*\([^)]*\)/g, '').trim();
// Original short gameplay summaries, only for confidently identified games; no copied history text.
const gameplay = {
    '1942': 'Pilot a fighter through vertically scrolling enemy formations.',
    '1943': 'Fly over the Pacific, fighting aircraft and attacking warships.',
    arkanoid: 'Deflect a ball with a paddle to clear brick formations and collect power-ups.',
    asteroid: 'Rotate a spaceship and blast drifting rocks while managing momentum.',
    btime: 'Walk across ingredients to assemble burgers while avoiding pursuing enemies.',
    centiped: 'Shoot a descending centipede through a mushroom-filled field.',
    contra: 'Run, jump and shoot through enemy bases with an expanding weapon arsenal.',
    ddonpach: 'Dodge dense bullet patterns while chaining enemies in a vertical shooter.',
    ddragon: 'Fight through city streets using punches, kicks and dropped weapons.',
    defender: 'Protect ground-level humans from alien abductions in a scrolling space battle.',
    digdug: 'Tunnel underground and defeat enemies with an air pump or falling rocks.',
    dkong: 'Climb girders and ladders while dodging barrels to rescue the captive.',
    dkong3: 'Use insect spray to drive Donkey Kong upward while protecting flowers.',
    dkongjr: 'Climb vines and avoid enemies while trying to free Donkey Kong.',
    ffight: 'Brawl through Metro City with close-range attacks and improvised weapons.',
    frogger: 'Guide a frog across busy roads and floating logs to reach safe homes.',
    galaga: 'Shoot diving alien formations and recover a captured fighter for extra firepower.',
    galaxian: 'Defend against alien formations whose attackers swoop toward your ship.',
    ghouls: 'Guide an armored knight through monster-filled stages with throwable weapons.',
    gng: 'Run and jump through haunted landscapes as a knight battling supernatural enemies.',
    goldnaxe: 'Battle fantasy foes with melee weapons, magic and rideable creatures.',
    invaders: 'Move beneath rows of descending aliens and shoot while sheltering behind barriers.',
    joust: 'Ride a flying mount and defeat rival riders by colliding from above.',
    mario: 'Clear sewer platforms by flipping pests from below and kicking them away.',
    mk: 'Duel opponents in one-on-one martial-arts bouts with special moves.',
    mk2: 'Fight one-on-one bouts using character-specific special moves and finishing attacks.',
    mk3: 'Chain attacks and special moves in one-on-one fighting bouts.',
    mslug: 'Run and gun through hand-drawn battlefields, rescuing prisoners and piloting vehicles.',
    mspacman: 'Eat maze pellets, avoid ghosts and use power pellets to turn the tables.',
    puckman: 'Clear maze pellets while avoiding ghosts and using temporary power-ups.',
    pacman: 'Clear maze pellets while avoiding ghosts and using temporary power-ups.',
    mvsc: 'Build a two-character team and tag between comic-book and Capcom fighters.',
    outrun: 'Race a sports car through branching roads before the timer expires.',
    paperboy: 'Deliver newspapers along a hazardous suburban route while keeping subscribers.',
    qbert: 'Hop across a pyramid to change tile colors while avoiding pursuing creatures.',
    rtype: 'Pilot a spacecraft through side-scrolling stages with a detachable Force weapon.',
    rtype2: 'Battle through side-scrolling space stages with charge shots and the Force weapon.',
    sf2: 'Choose a martial artist and face rivals in one-on-one bouts with special attacks.',
    shinobi: 'Rescue hostages and fight enemy ninjas with thrown weapons and close-range attacks.',
    starwars: 'Pilot an X-wing through first-person battles culminating in a Death Star trench run.',
    tapper: 'Serve drinks along multiple bars and catch returning mugs before they fall.',
    tekken: 'Compete in one-on-one 3D fighting bouts with character-specific strikes and throws.',
    tekken2: 'Fight one-on-one 3D bouts using a roster of distinct martial artists.',
    tekken3: 'Use strikes, throws and sidesteps in one-on-one 3D fighting bouts.',
    tmnt: 'Cooperate as the Ninja Turtles to battle enemies across side-scrolling city stages.',
    tmnt2: 'Battle through historical settings in a cooperative side-scrolling brawler.',
    tron: 'Complete distinct action challenges involving light cycles, tanks and other electronic hazards.',
    xmen: 'Choose a mutant hero and battle through scrolling stages using attacks and special powers.',
    thndrbld: 'Pilot a helicopter through alternating overhead and behind-the-craft shooting stages.',
    robotron: 'Move and fire independently to protect humans from waves of hostile robots.',
    tempest: 'Move around a geometric playfield rim and shoot enemies climbing toward you.',
    missile: 'Intercept incoming missiles with timed explosions to defend cities.',
    bzone: 'Drive a tank through a first-person battlefield and destroy enemy vehicles.',
    llander: 'Manage thrust and fuel to land a spacecraft safely on uneven lunar terrain.',
    bombjack: 'Leap between platforms to collect bombs while avoiding enemies.',
    pang: 'Break bouncing balloons into smaller pieces with upward-firing weapons.',
    snowbros: 'Trap enemies in snowballs and roll them across platforms.',
    nbajam: 'Play fast-paced two-on-two basketball with exaggerated dunks.',
    nbajamte: 'Play two-on-two basketball with team substitutions and spectacular dunks.',
    gauntlet: 'Explore maze-like dungeons, collecting treasure and battling crowds of enemies.',
    gaunt2: 'Explore cooperative dungeon mazes while managing health, keys and enemy generators.',
    strider: 'Slash through futuristic stages with acrobatic jumps and a sweeping blade.',
    punchout: 'Read opposing boxers’ tells and counter with carefully timed punches.',
    zaxxon: 'Fly through an isometric space fortress while judging height and dodging defenses.',
    qix: 'Claim parts of a playfield by drawing lines while avoiding moving hazards.',
    rampage: 'Climb and demolish city buildings as giant monsters while resisting military attacks.',
    berzerk: 'Navigate robot-filled rooms and fire in multiple directions.',
    mappy: 'Bounce between floors to recover stolen goods while evading cats.',
    mrdo: 'Dig paths, throw a ball and drop apples to defeat underground enemies.'
};
const parentOf = name => sets.get(name)?.parent ?? '';
const canonical = name => {
    if (curated.has(name)) return name; // Keep longstanding primary identities and cabinet slugs.
    if (oldAliases.has(name)) return oldAliases.get(name);
    let current = name; const seen = new Set();
    while (parentOf(current) && !seen.has(current)) {
        const parent = parentOf(current);
        // cloneof can include a sequel/expanded edition sharing code: After Burner, UMK3,
        // Hyper Fighting, Muscle Bomber Duo. Different named games retain distinct identities.
        if (baseTitle(sets.get(current).title) !== baseTitle(sets.get(parent)?.title ?? '')) break;
        seen.add(current); current = parent;
    }
    return current;
};
const groups = new Map();
for (const name of [...names].sort()) {
    const rootRom = canonical(name);
    if (!groups.has(rootRom)) groups.set(rootRom, []);
    if (name !== rootRom) groups.get(rootRom).push(name);
}
const rows = [...names].sort().map(rom => {
    const set = sets.get(rom) ?? sets.get(oldAliases.get(rom));
    const group = canonical(rom);
    const title = curated.get(group)?.title ?? baseTitle(sets.get(group)?.title ?? set.title);
    const maker = /[a-z]/i.test(set.manufacturer) ? set.manufacturer : 'an unrecorded manufacturer';
    const fact = `Arcade set ${/^[0-9]{4}$/.test(set.year) ? `dated ${set.year}` : 'with an uncertain release year'}, credited to ${maker}.`;
    return { rom, title, setTitle: set.title, year: set.year, manufacturer: set.manufacturer,
        parent: parentOf(rom), canonical: group, description: `${fact}${gameplay[group] ? ` ${gameplay[group]}` : ''}`,
        inventory: inventory.includes(rom), metadataSource: rom === 'thndrbdj' ? 'MAME2003 legacy + MAME2003-plus thndrbld1' : sets.has(rom) ? 'MAME2003-plus XML' : 'preserved catalog alias' };
});
const k = s => JSON.stringify(s).replace(/\$/g, '\\$');
const gameLines = [...groups].sort(([a], [b]) => a.localeCompare(b)).map(([rom, clones]) => {
    const row = rows.find(x => x.rom === rom);
    return `        Game(${k(rom)}, ${k(row.title)}, listOf(${clones.map(k).join(', ')}), ${k(row.description)})`;
});
fs.writeFileSync(path.join(out, 'games.txt'), gameLines.join(',\n') + '\n');
fs.writeFileSync(path.join(out, 'kotlin-table.txt'), [...groups].sort(([a], [b]) => a.localeCompare(b)).map(([rom, clones]) => {
    const row = rows.find(x => x.rom === rom);
    return [rom, row.title, row.year, row.manufacturer, clones.join(','), gameplay[rom] ?? ''].join('|');
}).join('\n') + '\n');
fs.writeFileSync(path.join(out, 'parents.txt'), rows.filter(x => x.parent).map(x => `        ${k(x.rom)} to ${k(x.parent)}`).join(',\n') + '\n');
fs.writeFileSync(path.join(out, 'metadata.json'), JSON.stringify({ source: 'https://raw.githubusercontent.com/libretro/mame2003-plus-libretro/master/metadata/mame2003-plus.xml',
    xmlSha256: crypto.createHash('sha256').update(xml).digest('hex'), inventoryFiles: inventoryFiles.length,
    distinctInventory: inventory.length, sets: rows }, null, 2) + '\n');
fs.writeFileSync(path.join(out, 'catalog-seed.json'), JSON.stringify(seed, null, 2) + '\n');
fs.writeFileSync(path.join(out, 'inventory.txt'), inventory.join('\n') + '\n');
if (process.argv.includes('--verify')) {
    const checkedTable = old.match(/private val metadata = """\n([\s\S]*?)\n"""/)?.[1];
    if (!checkedTable) throw new Error('No finite checked-in Kotlin table');
    const generated = fs.readFileSync(path.join(out, 'kotlin-table.txt'), 'utf8').trim().split('\n').sort();
    const checked = checkedTable.trim().split('\n').sort();
    if (JSON.stringify(generated) !== JSON.stringify(checked)) throw new Error('Kotlin table differs from regenerated metadata');
    const pairs = old.match(/private val confirmedParents = """\n([\s\S]*?)\n"""/)?.[1].trim().split(',').sort();
    const expected = rows.filter(x => x.parent).map(x => `${x.rom}:${x.parent}`).sort();
    if (JSON.stringify(pairs) !== JSON.stringify(expected)) throw new Error('Confirmed clone parents differ');
    console.log(`Verified checked-in Kotlin table: ${checked.length} games, ${expected.length} confirmed parent relationships.`);
}
if (process.argv.includes('--publish-data')) {
    const resource = path.join(root, 'app/src/main/assets/arcade-metadata');
    fs.mkdirSync(resource, { recursive: true });
    for (const name of ['metadata.json', 'catalog-seed.json', 'inventory.txt'])
        fs.copyFileSync(path.join(out, name), path.join(resource, name));
}
console.log(`Metadata: ${inventoryFiles.length} files, ${inventory.length} distinct ROMs, ${rows.length} identities, ${groups.size} catalog games.`);
if (process.argv.includes('--download')) {
    const refs = path.join(root, 'artwork/cabinet-references');
    const downloads = path.join(refs, 'downloads');
    fs.mkdirSync(downloads, { recursive: true });
    const priorPath = path.join(refs, 'cabinet-manifest.json');
    const prior = fs.existsSync(priorPath) ? JSON.parse(fs.readFileSync(priorPath, 'utf8')).mappings : [];
    const unavailable = new Set(prior.flatMap(x => x.tried).filter(x => x.status === 'unavailable').map(x => x.url));
    const sha256 = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
    const results = new Map();
    const download = async rom => {
        if (results.has(rom)) return results.get(rom);
        const task = (async () => {
            const url = `https://adb.arcadeitalia.net/media/mame.current/cabinets/${rom}.png`;
            const existing = path.join(refs, `${rom}.png`), destination = path.join(downloads, `${rom}.png`);
            if (fs.existsSync(existing)) return { rom, url, status: 'existing-reviewed', file: `${rom}.png`,
                sha256: sha256(existing), sourceFile: fs.existsSync(destination) ? `downloads/${rom}.png` : `${rom}.png`,
                sourceSha256: sha256(fs.existsSync(destination) ? destination : existing) };
            if (fs.existsSync(destination)) return { rom, url, status: 'downloaded-unreviewed', file: `downloads/${rom}.png`, sha256: sha256(destination) };
            if (unavailable.has(url) && !process.argv.includes('--refresh-missing')) return { rom, url, status: 'unavailable', cached: true };
            for (let attempt = 0; attempt < 2; attempt++) {
                try {
                    const response = await fetch(url, { signal: AbortSignal.timeout(20000) });
                    if (response.status === 404) return { rom, url, status: 'unavailable' };
                    if (!response.ok) throw new Error(`HTTP ${response.status}`);
                    const bytes = Buffer.from(await response.arrayBuffer());
                    if (!bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10]))) throw new Error('Not PNG');
                    if (bytes.length > 12000000) throw new Error('Oversized source');
                    fs.writeFileSync(destination, bytes);
                    return { rom, url, status: 'downloaded-unreviewed', file: `downloads/${rom}.png`, sha256: crypto.createHash('sha256').update(bytes).digest('hex') };
                } catch (e) { if (attempt === 1) return { rom, url, status: 'request-failed', error: e.message }; }
            }
        })();
        results.set(rom, task); return task;
    };
    // Explicit verified historical-to-current rename candidates; NEVER strip arbitrary ROM suffixes.
    const aliases = { thndrbdj: 'thndrbld1', asteroid: 'asteroid', centiped: 'centipede' };
    const mappings = new Array(inventory.length);
    let next = 0;
    await Promise.all(Array.from({ length: 6 }, async () => {
        while (next < inventory.length) {
            const index = next++, rom = inventory[index], tried = [];
            const candidates = [...new Set([rom, parentOf(rom), aliases[rom]].filter(Boolean))];
            let selected;
            for (const candidate of candidates) {
                const result = await download(candidate); tried.push(result);
                if (result.file) { selected = result; break; }
            }
            const ready = candidates.find(candidate => fs.existsSync(path.join(refs, `${candidate}.png`)));
            mappings[index] = { rom, parent: parentOf(rom), tried, selected: selected?.rom ?? null,
                relation: !selected ? 'missing' : selected.rom === rom ? 'exact-set-reference' : selected.rom === parentOf(rom) ? 'confirmed-parent-alternative' : 'verified-rename',
                buildReady: Boolean(ready), buildReference: ready ?? null,
                buildRelation: !ready ? 'game-snapshot-fallback-recommended' : ready === rom ? 'exact-set-reference' : ready === parentOf(rom) ? 'confirmed-parent-alternative' : 'verified-rename' };
        }
    }));
    fs.writeFileSync(path.join(out, 'cabinet-manifest.json'), JSON.stringify({ retrieved: '2026-10-09', inventory: inventory.length,
        note: 'Reference photos may show conversions/restorations. Parent alternatives are not exact set/control-layout claims. New downloads require silhouette review.', mappings }, null, 2) + '\n');
    if (process.argv.includes('--publish-data')) fs.copyFileSync(path.join(out, 'cabinet-manifest.json'), path.join(refs, 'cabinet-manifest.json'));
    console.log(`Cabinets: ${mappings.filter(x => x.selected).length} sourced, ${mappings.filter(x => x.buildReady).length} reviewed, ${mappings.filter(x => !x.selected).length} missing.`);
    if (process.argv.includes('--publish-data')) {
        const sourceCode = fs.readFileSync(path.join(root, 'scripts/PrepareReviewedArcadeSources.java'), 'utf8');
        const reviewed = [...sourceCode.match(/REVIEWED = \{([\s\S]*?)\}/)[1].matchAll(/"([^"]+)"/g)].map(x => x[1]);
        const review = path.join(refs, 'review');
        fs.mkdirSync(review, { recursive: true });
        fs.writeFileSync(path.join(review, 'cutout-manifest.json'), JSON.stringify({ policy: 'Explicit continuous-upright row-interior silhouette restoration; preserve original RGB, no black-key on prepared alpha images.',
            rejected: { aliensyn: 'Near-black side wall loses exterior contour', looping: 'Nonzero opaque scan backdrop', rallyx: 'Nonzero opaque scan backdrop',
                kchamp: 'Connected scan-frame rectangle', missile: 'Horizontal scan-frame baseline', pacland: 'Horizontal scan-frame baseline' },
            images: reviewed.map(rom => ({ rom, source: `downloads/${rom}.png`, sourceSha256: sha256(path.join(downloads, `${rom}.png`)),
                prepared: `${rom}.png`, preparedSha256: sha256(path.join(refs, `${rom}.png`)), threshold: 2, solidRowInterior: true })) }, null, 2) + '\n');
        fs.writeFileSync(path.join(review, 'coverage.tsv'), 'rom\tbuild_reference\trelation\tdownload_reference\n' +
            mappings.map(x => `${x.rom}\t${x.buildReference ?? ''}\t${x.buildRelation}\t${x.selected ?? ''}`).join('\n') + '\n');
        // A convenient source contact-sheet input, outside the build's top-level PNG glob.
        const rawReview = '/tmp/arcade-reviewed-raw';
        fs.mkdirSync(rawReview, { recursive: true });
        for (const rom of reviewed) fs.copyFileSync(path.join(downloads, `${rom}.png`), path.join(rawReview, `${rom}.png`));
    }
}