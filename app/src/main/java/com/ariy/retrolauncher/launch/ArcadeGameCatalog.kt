package com.ariy.retrolauncher.launch

import java.util.Locale

/** Finite offline arcade metadata; provenance and per-set facts are in arcade-metadata assets. */
internal object ArcadeGameCatalog {
    data class Game(
        val rom: String, val title: String, val clones: List<String> = emptyList(),
        val description: String = "An arcade game.", val year: String? = null, val catchphrase: String? = null
    )
    private val originalGames = listOf(
        Game("1942", "1942"), Game("1943", "1943: The Battle of Midway"),
        Game("arkanoid", "Arkanoid"), Game("asteroid", "Asteroids", listOf("asteroid1", "asteroid2")),
        Game("btime", "BurgerTime", listOf("btime2", "btime3")), Game("centiped", "Centipede", listOf("centiped1", "centiped2")),
        Game("contra", "Contra"), Game("ddonpach", "DoDonPachi"), Game("ddragon", "Double Dragon", listOf("ddragonu")),
        Game("defender", "Defender", listOf("defendg", "defenderg", "defenderb", "defenderw")),
        Game("digdug", "Dig Dug", listOf("digdug1")), Game("dkong", "Donkey Kong", listOf("dkongjo", "dkongjo1")),
        Game("dkong3", "Donkey Kong 3"),
        Game("ffight", "Final Fight", listOf("ffightu", "ffightj")), Game("frogger", "Frogger"),
        Game("galaga", "Galaga", listOf("galagao", "galagamw")), Game("galaxian", "Galaxian"),
        Game("ghouls", "Ghouls ’n Ghosts", listOf("ghoulsu")), Game("gng", "Ghosts ’n Goblins", listOf("gnga", "gngt")),
        Game("goldnaxe", "Golden Axe", listOf("goldnaxe1", "goldnaxe2", "goldnaxe3")),
        Game("invaders", "Space Invaders"), Game("joust", "Joust"), Game("mario", "Mario Bros."),
        Game("mk2", "Mortal Kombat II"), Game("mslug", "Metal Slug"), Game("mspacman", "Ms. Pac-Man"),
        Game("mvsc", "Marvel vs. Capcom: Clash of Super Heroes", listOf("mvscu", "mvscj")),
        Game("outrun", "Out Run", listOf("outrunb")), Game("pacman", "Pac-Man", listOf("pacmanf")), Game("paperboy", "Paperboy"),
        Game("qbert", "Q*bert"), Game("rtype", "R-Type"), Game("rtype2", "R-Type II"),
        Game("sf2", "Street Fighter II: The World Warrior", listOf("sf2u", "sf2j")),
        Game("shinobi", "Shinobi"), Game("starwars", "Star Wars"), Game("tapper", "Tapper"),
        Game("tekken3", "Tekken 3"), Game("tmnt", "Teenage Mutant Ninja Turtles", listOf("tmntu", "tmntj")),
        Game("tmnt2", "Teenage Mutant Ninja Turtles: Turtles in Time", listOf("tmnt22p")),
        Game("tron", "TRON"), Game("xmen", "X-Men", listOf("xmen2p"))
    )
    // Generated finite rom|title|year|manufacturer|aliases table. Never prefix-match ROMs.
    private val metadata = """
1941|1941 - Counter Attack|1990|Capcom||
1942|1942|1984|Capcom||Pilot a fighter through vertically scrolling enemy formations.
1943|1943: The Battle of Midway|1987|Capcom||Fly over the Pacific, fighting aircraft and attacking warships.
1943kai|1943 Kai - Midway Kaisen|1987|Capcom||
1944|1944: The Loop Master|2000|Eighting / Raizing (Capcom license)||
19xx|19XX: The War Against Destiny|1996|Capcom||
3wonders|Three Wonders|1991|Capcom|3wonderu|
720|720 Degrees|1986|Atari Games||
aburner|After Burner|1987|Sega||
aburner2|After Burner II|1987|Sega||
actfancr|Act-Fancer Cybernetick Hyper Weapon|1989|Data East Corporation||
aerofgt|Aero Fighters|1992|Video System Co.||
airwolf|Airwolf|1987|Kyugo||
ajax|Ajax|1987|Konami||
aliens|Aliens|1990|Konami|aliensu|
aliensyn|Alien Syndrome|1987|Sega||
aligator|Alligator Hunt|1994|Gaelco|aligatun|
altbeast|Altered Beast|1988|Sega||
amidar|Amidar|1981|Konami||
aquajack|Aqua Jack|1990|Taito Corporation Japan||
arkanoid|Arkanoid|1986|Taito Corporation Japan|arknoidu|Deflect a ball with a paddle to clear brick formations and collect power-ups.
arknoid2|Arkanoid - Revenge of DOH|1987|Taito Corporation Japan||
armwar|Armored Warriors|1994|Capcom|armwaru|
asterix|Asterix|1992|Konami||
asteroid|Asteroids|1979|Atari|asteroid1,asteroid2|Rotate a spaceship and blast drifting rocks while managing momentum.
astorm|Alien Storm|1990|Sega|astormbl|
atetris|Tetris|1988|Atari Games||
avsp|Alien vs. Predator|1994|Capcom|avspu|
avspirit|Avenging Spirit|1991|Jaleco||
baddudes|Bad Dudes vs. Dragonninja|1988|Data East USA||
bankp|Bank Panic|1984|[Sanritsu] Sega||
batcir|Battle Circuit|1997|Capcom||
batman|Batman|1991|Atari Games||
batrider|Armed Police Batrider|1998|Raizing / Eighting||
bbros|Buster Bros.|1989|Capcom||
berzerk|Berzerk|1980|Stern|berzerk1|Navigate robot-filled rooms and fire in multiple directions.
biomtoy|Biomechanical Toy|1995|Gaelco||
bionicc|Bionic Commando|1987|Capcom||
blktiger|Black Tiger|1987|Capcom||
blstroid|Blasteroids|1987|Atari Games||
blswhstl|Bells and Whistles|1991|Konami||
bnj|Bump 'n' Jump|1982|Data East USA (Bally Midway license)||
bogeyman|Bogey Manor|1985|Technos Japan||
bombjack|Bomb Jack|1984|Tehkan||Leap between platforms to collect bombs while avoiding enemies.
boogwing|Boogie Wings|1992|Data East Corporation||
bosco|Bosconian|1981|Namco||
brubber|Burnin' Rubber|1982|Data East||
btime|BurgerTime|1982|Data East Corporation|btime2,btime3|Walk across ingredients to assemble burgers while avoiding pursuing enemies.
btoads|Battle Toads|1994|Rare||
bubbles|Bubbles|1982|Williams||
bublbob2|Bubble Bobble 2|1994|Taito Corporation Japan||
bublbobl|Bubble Bobble|1986|Taito Corporation|bubbobr1|
bubsympu|Bubble Symphony|1994|Taito America Corporation||
buckrog|Buck Rogers - Planet of Zoom|1982|Sega||
bucky|Bucky O'Hare|1992|Konami|buckyua|
bwidow|Black Widow|1982|Atari||
bzone|Battle Zone|1980|Atari||Drive a tank through a first-person battlefield and destroy enemy vehicles.
cabal|Cabal|1988|Tad Corporation||
candance|Cannon Dancer|1996|Mitchell||
captaven|Captain America and The Avengers|1991|Data East Corporation|captavnu|
captcomm|Captain Commando|1991|Capcom|captcomu|
cawing|Carrier Air Wing|1990|Capcom|cawingu|
cbuster|Crude Buster|1990|Data East Corporation||
ccastles|Crystal Castles|1983|Atari||
cclimber|Crazy Climber|1980|Nichibutsu||
centiped|Centipede|1980|Atari|centiped1,centiped2,centtime|Shoot a descending centipede through a mushroom-filled field.
chasehq|Chase H.Q.|1988|Taito Corporation Japan||
chinagat|China Gate|1988|[Technos] (Taito Romstar license)||
chplft|Choplifter|1985|Sega|chplftb|
circusc|Circus Charlie|1984|Konami||
citycon|City Connection|1985|Jaleco||
columns|Columns|1990|Sega||
commando|Commando|1985|Capcom|commandu|
congo|Congo Bongo|1983|Sega||
contra|Contra|1987|Konami||Run, jump and shoot through enemy bases with an expanding weapon arsenal.
crimfght|Crime Fighters|1989|Konami|crimfgt2|
ctribe|The Combatribes|1990|Technos||
cybots|Cyberbots: Fullmetal Madness|1995|Capcom||
cyvern|Cyvern|1998|Kaneko||
darkseal|Dark Seal|1990|Data East Corporation||
dassault|Desert Assault|1991|Data East Corporation||
dbreed|Dragon Breed|1989|Irem||
ddcrew|D. D. Crew|1991|Sega||
ddonpach|DoDonPachi|1997|Atlus/Cave||Dodge dense bullet patterns while chaining enemies in a vertical shooter.
ddragon|Double Dragon|1987|Technos|ddragonu|Fight through city streets using punches, kicks and dropped weapons.
ddragon2|Double Dragon II - The Revenge|1988|Technos|ddragn2u|
ddsom|Dungeons & Dragons: Shadow over Mystara|1996|Capcom|ddsomu|
ddtod|Dungeons & Dragons: Tower of Doom|1993|Capcom|ddtodu|
deadconx|Dead Connection|1992|Taito Corporation Japan||
defender|Defender|1980|Williams|defenderb,defenderg,defenderw,defendg|Protect ground-level humans from alien abductions in a scrolling space battle.
digdug|Dig Dug|1982|Namco|digdug1|Tunnel underground and defeat enemies with an air pump or falling rocks.
digdug2|Dig Dug II|1985|Namco||
dimahoo|Dimahoo|2000|Eighting / Raizing (Capcom license)||
dino|Cadillacs and Dinosaurs|1993|Capcom|dinou|
dkong|Donkey Kong|1981|Nintendo of America|dkongjo,dkongjo1,dkongo|Climb girders and ladders while dodging barrels to rescue the captive.
dkong3|Donkey Kong 3|1983|Nintendo of America||Use insect spray to drive Donkey Kong upward while protecting flowers.
dkongjr|Donkey Kong Junior|1982|Nintendo of America||Climb vines and avoid enemies while trying to free Donkey Kong.
docastle|Mr. Do's Castle|1983|Universal||
dogyuun|Dogyuun|1992|Toaplan||
dondokod|Don Doko Don|1989|Taito Corporation Japan||
donpachi|DonPachi|1995|Atlus/Cave||
drtoppel|Dr. Toppel's Adventure|1987|Taito Corporation Japan||
dsaber|Dragon Saber|1990|Namco||
dstlk|Darkstalkers: The Night Warriors|1994|Capcom|dstlku|
dungeonm|Dungeon Magic|1993|Taito Corporation Japan||
dw|Dynasty Wars|1989|Capcom||
ecofghtr|Eco Fighters|1993|Capcom||
edrandy|The Cliffhanger - Edward Randy|1990|Data East Corporation||
elevator|Elevator Action|1983|Taito Corporation||
elvactr|Elevator Action Returns|1994|Taito Corporation Japan||
enduror|Enduro Racer|1986|Sega|endurobl|
exedexes|Exed Exes|1985|Capcom||
fantzn2|Fantasy Zone 2|198?|????||
fantzone|Fantasy Zone|1986|Sega||
ffight|Final Fight|1989|Capcom|ffightj,ffightu|Brawl through Metro City with close-range attacks and improvised weapons.
flicky|Flicky|1984|Sega||
foodf|Food Fight|1982|Atari||
forgottn|Forgotten Worlds|1988|Capcom||
frogger|Frogger|1981|Konami||Guide a frog across busy roads and floating logs to reach safe homes.
funkyjet|Funky Jet|1992|[Data East] (Mitchell license)||
ga2|Golden Axe - The Revenge of Death Adder|1992|Sega||
gaiden|Ninja Gaiden|1988|Tecmo||
galaga|Galaga|1981|Namco|galagamw,galagao|Shoot diving alien formations and recover a captured fighter for extra firepower.
galaga88|Galaga '88|1987|Namco||
galaxian|Galaxian|1979|Namco||Defend against alien formations whose attackers swoop toward your ship.
galmedes|Galmedes|1992|Visco||
gangwars|Gang Wars|1989|Alpha Denshi Co.||
gaplus|Gaplus|1984|Namco||
gaunt2|Gauntlet II|1986|Atari Games|gaunt22p|Explore cooperative dungeon mazes while managing health, keys and enemy generators.
gauntlet|Gauntlet|1985|Atari Games|gaunt2p|Explore maze-like dungeons, collecting treasure and battling crowds of enemies.
gberet|Green Beret|1985|Konami||
ghostb|The Real Ghostbusters|1987|Data East USA||
ghouls|Ghouls ’n Ghosts|1988|Capcom|ghoulsu|Guide an armored knight through monster-filled stages with throwable weapons.
gigawing|Giga Wing|1999|Takumi (Capcom license)||
gijoe|GI Joe|1992|Konami|gijoeu|
gnbarich|Gunbarich|2001|Psikyo||
gng|Ghosts ’n Goblins|1985|Capcom|gnga,gngt|Run and jump through haunted landscapes as a knight battling supernatural enemies.
godzilla|Godzilla|1993|Banpresto||
goldnaxe|Golden Axe|1989|Sega|goldnaxe1,goldnaxe2,goldnaxe3|Battle fantasy foes with melee weapons, magic and rideable creatures.
gorf|Gorf|1981|Midway||
gradius|Gradius|1985|Konami||
gradius2|Gradius II - GOFER no Yabou|1988|Konami||
gradius3|Gradius III|1989|Konami|grdius3e|
grdians|Guardians - Denjin Makai II|1995|Banpresto||
growl|Growl|1990|Taito Corporation Japan|growlu|
gtmr2|Mille Miglia 2 - Great 1000 Miles Rally|1995|Kaneko||
gunbird2|Gunbird 2|1998|Psikyo||
gunforc2|Gunforce 2|1994|Irem||
gunlock|Gunlock|1993|Taito Corporation Japan||
gunnail|GunNail|1993|NMK / Tecmo||
gunsmoke|Gun.Smoke|1985|Capcom|gunsmoku|
guwange|Guwange|1999|Atlus/Cave||
gyruss|Gyruss|1983|Konami||
hangon|Hang-On|1985|Sega||
hattrick|Hat Trick|1984|Bally/Sente||
heatbrl|Heated Barrel|1992|Tad|heatbrlu|
hharry|Hammerin' Harry|1990|Irem|hharryu|
hitice|Hit the Ice|1990|Williams||
hook|Hook|1992|Irem|hooku|
horizon|Horizon|1985|Irem||
hvysmsh|Heavy Smash|1993|Data East Corporation||
ikari|Ikari Warriors|1986|SNK||
ikari3|Ikari III - The Rescue|1989|SNK|ikari3nr|
ikarijpb|Ikari|1986|bootleg||
imgfight|Image Fight|1988|Irem||
inthunt|In The Hunt|1993|Irem||
invaders|Space Invaders|1978|Midway||Move beneath rows of descending aliens and shoot while sheltering behind barriers.
irobot|I, Robot|1983|Atari||
jailbrek|Jail Break|1986|Konami||
journey|Journey|1983|Bally Midway||
joust|Joust|1982|Williams||Ride a flying mount and defeat rival riders by colliding from above.
jungleh|Jungle Hunt|1982|Taito America Corporation||
junglek|Jungle King|1982|Taito Corporation||
junofrst|Juno First|1983|Konami||
kangaroo|Kangaroo|1982|Sun Electronics||
karnov|Karnov|1987|Data East USA||
kchamp|Karate Champ|1984|Data East USA||
kick|Kick|1981|Midway||
kicker|Kicker|1985|Konami||
kingofb|King of Boxer|1985|Woodplace||
klax|Klax|1989|Atari Games||
knights|Knights of the Round|1991|Capcom|knightsu|
kod|The King of Dragons|1991|Capcom|kodu|
kungfum|Kung-Fu Master|1984|Irem||
landmakr|Land Maker|1998|Taito Corporation|landmkrp|
ldrun|Lode Runner|1984|Irem (licensed from Broderbund)||
ldrun2|Lode Runner II - The Bungeling Strikes Back|1984|Irem (licensed from Broderbund)||
ldrun4|Lode Runner IV - Teikoku Karano Dasshutsu|1986|Irem (licensed from Broderbund)||
lethalth|Lethal Thunder|1991|Irem||
lifefrce|Lifeforce|1986|Konami||
lightbr|Light Bringer|1993|Taito Corporation||
liquidk|Liquid Kids|1990|Taito Corporation Japan||
lkage|The Legend of Kage|1984|Taito Corporation||
llander|Lunar Lander|1979|Atari||Manage thrust and fuel to land a spacecraft safely on uneven lunar terrain.
loderndf|Lode Runner - The Dig Fight|2000|Psikyo||
loht|Legend of Hero Tonma|1989|Irem||
looping|Looping|1982|Venture Line||
lwings|Legendary Wings|1986|Capcom||
macross|Super Spacefortress Macross - Chou-Jikuu Yousai Macross|1992|Banpresto||
macross2|Super Spacefortress Macross II - Chou-Jikuu Yousai Macross II|1993|Banpresto||
macrossp|Macross Plus|1996|Banpresto||
mainevt|The Main Event|1988|Konami|mainev2p|
mappy|Mappy|1983|Namco||Bounce between floors to recover stolen goods while evading cats.
marble|Marble Madness|1984|Atari Games||
mario|Mario Bros.|1983|Nintendo of America||Clear sewer platforms by flipping pests from below and kicking them away.
mazinger|Mazinger Z|1994|Banpresto/Dynamic Pl. Toei Animation||
mbombrd|Muscle Bomber Duo - Ultimate Team Battle|1993|Capcom||
megaman2|Mega Man 2: The Power Fighters|1996|Capcom||
mercs|Mercs|1990|Capcom||
metamrph|Metamorphic Force|1993|Konami||
metmqstr|Metamoqester|1995|Banpresto/Pandorabox||
mgcrystl|Magical Crystals|1991|Kaneko||
mhavoc|Major Havoc|1983|Atari||
milliped|Millipede|1982|Atari||
missile|Missile Command|1980|Atari||Intercept incoming missiles with timed explosions to defend cities.
mk|Mortal Kombat|1992|Midway||Duel opponents in one-on-one martial-arts bouts with special moves.
mk2|Mortal Kombat II|1993|Midway||Fight one-on-one bouts using character-specific special moves and finishing attacks.
mk3|Mortal Kombat 3|1994|Midway||Chain attacks and special moves in one-on-one fighting bouts.
mmatrix|Mars Matrix: Hyper Solid Shooting|2000|Takumi (Capcom license)||
monsterb|Monster Bash|1982|Sega||
moo|Wild West C.O.W.-Boys of Moo Mesa|1992|Konami|mooua|
moonwalk|Michael Jackson's Moonwalker|1990|Sega|moonwlkb|
mpatrol|Moon Patrol|1982|Irem||
mrdo|Mr. Do!|1982|Universal||Dig paths, throw a ball and drop apples to defeat underground enemies.
mshvsf|Marvel Super Heroes Vs. Street Fighter|1997|Capcom|mshvsfj|
mslug|Metal Slug|1996|Nazca||Run and gun through hand-drawn battlefields, rescuing prisoners and piloting vehicles.
mspacman|Ms. Pac-Man|1981|Midway||Eat maze pellets, avoid ghosts and use power pellets to turn the tables.
msword|Magic Sword - Heroic Fantasy|1990|Capcom|mswordu|
mtwins|Mega Twins|1990|Capcom||
mvsc|Marvel vs. Capcom: Clash of Super Heroes|1998|Capcom|mvscj,mvscu|Build a two-character team and tag between comic-book and Capcom fighters.
mystwarr|Mystic Warriors|1993|Konami|mystwaru|
narc|Narc|1988|Williams||
nbajam|NBA Jam|1993|Midway||Play fast-paced two-on-two basketball with exaggerated dunks.
nbajamte|NBA Jam TE|1994|Midway||Play two-on-two basketball with team substitutions and spectacular dunks.
nbbatman|Ninja Baseball Batman|1993|Irem America||
nemesis|Nemesis|1985|Konami||
nemo|Nemo|1990|Capcom||
nibbler|Nibbler|1982|Rock-ola||
ninjak|The Ninja Kids|1990|Taito Corporation Japan||
ninjakd2|Ninja-Kid II|1987|UPL||
nitrobal|Nitro Ball|1992|Data East Corporation||
noboranb|Noboranka|1986|bootleg||
nrallyx|New Rally X|1981|Namco||
nslasher|Night Slashers|1994|Data East Corporation||
nspirit|Ninja Spirit|1988|Irem||
nwarr|Night Warriors: Darkstalkers' Revenge|1995|Capcom||
offroad|Ironman Stewart's Super Off-Road|1989|Leland Corp.||
osman|Osman|1996|Mitchell||
outfxies|Outfoxies|1994|Namco||
outrun|Out Run|1986|Sega|outrunb|Race a sports car through branching roads before the timer expires.
outzone|Out Zone|1990|Toaplan||
pacland|Pac-Land|1984|Namco||
pacman|Pac-Man|1980|[Namco] (Midway license)|pacmanf|Clear maze pellets while avoiding ghosts and using temporary power-ups.
pang|Pang|1989|Mitchell||Break bouncing balloons into smaller pieces with upward-firing weapons.
pang3|Pang! 3|1995|Mitchell||
paperboy|Paperboy|1984|Atari Games||Deliver newspapers along a hazardous suburban route while keeping subscribers.
parodius|Parodius DA!|1990|Konami||
pengo|Pengo|1982|Sega||
peterpak|Peter Pack-Rat|1984|Atari Games||
phoenix|Phoenix|1980|Amstar||
pipedrm|Pipe Dream|1990|Video System Co.||
pitfight|Pit Fighter|1990|Atari Games||
polepos|Pole Position|1982|Namco||
polepos2|Pole Position II|1983|Namco||
pooyan|Pooyan|1982|Konami||
popeye|Popeye|1982|Nintendo|popeyef|
pow|P.O.W. - Prisoners of War|1988|SNK||
progear|Progear|2001|Cave (Capcom license)||
puckman|PuckMan|1980|Namco||Clear maze pellets while avoiding ghosts and using temporary power-ups.
punchout|Punch-Out!!|1984|Nintendo||Read opposing boxers’ tells and counter with carefully timed punches.
punisher|The Punisher|1993|Capcom|punishru|
punkshot|Punk Shot|1990|Konami|punksht2|
puyopuy2|Puyo Puyo 2|1994|Compile (Sega license)||
puyopuyo|Puyo Puyo|1992|Sega / Compile|puyopuya|
puzzloop|Puzz Loop|1998|Mitchell|puzloopu|
qbert|Q*bert|1982|Gottlieb||Hop across a pyramid to change tile colors while avoiding pursuing creatures.
qix|Qix|1981|Taito America Corporation||Claim parts of a playfield by drawing lines while avoiding moving hazards.
rallyx|Rally X|1980|Namco||
rampage|Rampage|1986|Bally Midway||Climb and demolish city buildings as giant monsters while resisting military attacks.
rampart|Rampart|1990|Atari Games|ramprt2p|
rastan|Rastan|1987|Taito Corporation Japan|rastanu|
renegade|Renegade|1986|Technos (Taito America license)||
ringdest|Ring of Destruction: Slammasters II|1994|Capcom||
ringking|Ring King|1985|Data East USA||
robocop|Robocop|1988|Data East Corporation||
robocop2|Robocop 2|1991|Data East Corporation|robocp2u|
robotron|Robotron|1982|Williams||Move and fire independently to protect humans from waves of hostile robots.
rocnrope|Roc'n Rope|1983|Konami||
rohga|Rohga Armour Force|1991|Data East Corporation|rohgau|
rthun2|Rolling Thunder 2|1990|Namco||
rthunder|Rolling Thunder|1986|Namco||
rtype|R-Type|1987|Irem|rtypeu|Pilot a spacecraft through side-scrolling stages with a detachable Force weapon.
rtype2|R-Type II|1989|Irem||Battle through side-scrolling space stages with charge shots and the Force weapon.
rtypeleo|R-Type Leo|1992|Irem||
rushatck|Rush'n Attack|1985|Konami||
rygar|Rygar|1986|Tecmo||
s1945|Strikers 1945|1995|Psikyo||
s1945ii|Strikers 1945 II|1997|Psikyo||
s1945iii|Strikers 1945 III - Strikers 1999|1999|Psikyo||
salamand|Salamander|1986|Konami||
samuraia|Samurai Aces|1993|Psikyo||
scontra|Super Contra|1988|Konami||
seawolf|Sea Wolf|1976|Midway||
seganinj|Sega Ninja|1985|Sega|seganinu|
sf1|Street Fighter|1987|Capcom|sf1us|
sf2|Street Fighter II: The World Warrior|1991|Capcom|sf2j,sf2u|Choose a martial artist and face rivals in one-on-one bouts with special attacks.
sf2ce|Street Fighter II' - Champion Edition|1992|Capcom||
sf2t|Street Fighter II' - Hyper Fighting|1992|Capcom||
sfa2|Street Fighter Alpha 2|1996|Capcom||
sfa3|Street Fighter Alpha 3|1998|Capcom||
sgemf|Super Gem Fighter Mini Mix|1997|Capcom||
shadfrce|Shadow Force|1993|Technos Japan||
shadoww|Shadow Warriors|1988|Tecmo||
shangon|Super Hang-On|1992|Sega|shangonb|
shaolins|Shao-Lin's Road|1985|Konami||
sharrier|Space Harrier|1985|Sega||
shdancer|Shadow Dancer|1989|Sega||
shinobi|Shinobi|1987|Sega||Rescue hostages and fight enemy ninjas with thrown weapons and close-range attacks.
shollow|Satan's Hollow|1981|Bally Midway||
sidearms|Side Arms - Hyper Dyne|1986|Capcom|sidearmr|
simpsons|The Simpsons|1991|Konami|simpsn2p|
sinistar|Sinistar|1982|Williams||
slammast|Saturday Night Slam Masters|1993|Capcom|slammasu|
slyspy|Sly Spy|1989|Data East USA||
smashtv|Smash T.V.|1990|Williams||
snowbros|Snow Bros. - Nick & Tom|1990|Toaplan|snowbrob|Trap enemies in snowballs and roll them across platforms.
solomon|Solomon's Key|1986|Tecmo||
sonic|Segasonic the Hedgehog|1992|Sega||
sonson|Son Son|1984|Capcom||
spf2t|Super Puzzle Fighter II Turbo|1996|Capcom||
spidey|Spider-Man: The Videogame|1991|Sega||
splatter|Splatter House|1988|Namco||
spnchout|Super Punch-Out!!|1984|Nintendo||
spyhunt|Spy Hunter|1983|Bally Midway||
ssf2t|Super Street Fighter II Turbo|1994|Capcom||
ssprint|Super Sprint|1986|Atari Games||
ssriders|Sunset Riders|1991|Konami|ssrdrubc|
starcas|Star Castle|1980|Cinematronics||
starwars|Star Wars|1983|Atari||Pilot an X-wing through first-person battles culminating in a Death Star trench run.
stdragon|Saint Dragon|1989|Jaleco||
stmblade|Storm Blade|1996|Visco||
strider|Strider|1989|Capcom||Slash through futuristic stages with acrobatic jumps and a sweeping blade.
superman|Superman|1988|Taito Corporation||
superpac|Super Pac-Man|1982|Namco||
tapper|Tapper|1983|Bally Midway||Serve drinks along multiple bars and catch returning mugs before they fall.
tekken|Tekken|1994|Namco||Compete in one-on-one 3D fighting bouts with character-specific strikes and throws.
tekken2|Tekken 2 Ver.B|1995|Namco||Fight one-on-one 3D bouts using a roster of distinct martial artists.
tekken3|Tekken 3|1996|Namco||Use strikes, throws and sidesteps in one-on-one 3D fighting bouts.
tempest|Tempest|1980|Atari||Move around a geometric playfield rim and shoot enemies climbing toward you.
tetris|Tetris|1988|Sega||
thndrbld|Thunder Blade|1987|Sega|thndrbdj|Pilot a helicopter through alternating overhead and behind-the-craft shooting stages.
thndzone|Thunder Zone|1991|Data East Corporation||
tigeroad|Tiger Road|1987|Capcom (Romstar license)||
timeplt|Time Pilot|1982|Konami||
tmnt|Teenage Mutant Ninja Turtles|1989|Konami|tmnt2po,tmntj,tmntu|Cooperate as the Ninja Turtles to battle enemies across side-scrolling city stages.
tmnt2|Teenage Mutant Ninja Turtles: Turtles in Time|1991|Konami|tmnt22p|Battle through historical settings in a cooperative side-scrolling brawler.
tnzs|The NewZealand Story|1988|Taito Corporation|tnzsb|
toki|Toki|1989|Tad|tokiu|
toobin|Toobin'|1988|Atari Games||
trackfld|Track and Field|1983|Konami||
trojan|Trojan|1986|Capcom||
tron|TRON|1982|Bally Midway||Complete distinct action challenges involving light cycles, tanks and other electronic hazards.
truxton2|Truxton II - Tatsujin II - Tatsujin Oh|1992|Toaplan||
tumblep|Tumble Pop|1991|Data East Corporation||
typhoon|Typhoon|1987|Konami||
uccops|Undercover Cops|1992|Irem||
umk3|Ultimate Mortal Kombat 3|1994|Midway||
unsquad|U.N. Squadron|1989|Capcom||
upndown|Up'n Down|1983|Sega||
valkyrie|Valkyrie No Densetsu|1989|Namco||
varth|Varth - Operation Thunderstorm|1992|Capcom|varthu|
vball|U.S. Championship V'ball|1988|Technos||
vendetta|Vendetta|1991|Konami|vendet2p|
vf|Virtua Fighter|1993|Sega||
vigilant|Vigilante|1988|Irem|vigilntu|
viostorm|Violent Storm|1993|Konami||
volfied|Volfied|1989|Taito Corporation Japan||
vsav|Vampire Savior: The Lord of Vampire|1997|Capcom|vsavu|
vsav2|Vampire Savior 2: The Lord of Vampire|1997|Capcom||
vulcan|Vulcan Venture|1988|Konami||
wb3|Wonder Boy III - Monster Lair|1988|Sega / Westone||
wbml|Wonder Boy in Monster Land|1987|Sega / Westone|wbmlb|
wboy|Wonder Boy|1986|Sega (Escape license)|wboyu|
wildfang|Wild Fang - Tecmo Knight|1989|Tecmo||
willow|Willow|1989|Capcom||
wof|Warriors of Fate|1992|Capcom|wofu|
wow|Wizard of Wor|1980|Midway||
xevious|Xevious|1982|Namco||
xexex|Xexex|1991|Konami||
xmcota|X-Men: Children of the Atom|1994|Capcom|xmcotau|
xmen|X-Men|1992|Konami|xmen2p|Choose a mutant hero and battle through scrolling stages using attacks and special powers.
xmultipl|X Multiply|1989|Irem||
xmvsf|X-Men Vs. Street Fighter|1996|Capcom|xmvsfu,xmvsfur1|
zaxxon|Zaxxon|1982|Sega||Fly through an isometric space fortress while judging height and dodging defenses.
zerowing|Zero Wing|1989|Toaplan||
zookeep|Zoo Keeper|1982|Taito America Corporation||
""".trimIndent()
    val games: List<Game> = metadata.lineSequence().map { line ->
        val fields = line.split('|')
        require(fields.size == 6) { "Malformed arcade metadata row" }
        val year = fields[2].takeIf { it.matches(Regex("[0-9]{4}")) }
        val maker = fields[3].takeIf { it.any(Char::isLetter) } ?: "an unrecorded manufacturer"
        val facts = "Arcade set ${year?.let { "dated $it" } ?: "with an uncertain release year"}, credited to $maker."
        Game(fields[0], fields[1], fields[4].split(',').filter(String::isNotBlank),
            listOf(facts, fields[5]).filter(String::isNotBlank).joinToString(" "), year,
            fields[5].ifBlank { "A coin-op from $maker." })
    }.toList()
    private val byRom = games.flatMap { game -> (listOf(game.rom) + game.clones).map { it to game } }.toMap()
    init {
        require(originalGames.all { original ->
            byRom[original.rom]?.title == original.title &&
                original.clones.all { byRom[it]?.rom == original.rom }
        }) { "Existing arcade title or alias was lost" }
    }
    private val byTitle = games.groupBy { it.title.lowercase(Locale.ROOT) }
        .filterValues { it.size == 1 }.mapValues { it.value.single() }
    private val copySuffix = Regex(" \\([0-9]+\\)$")
    private fun romKey(raw: String): String? {
        val value = raw.trim().lowercase(Locale.ROOT)
        val stem = if (value.endsWith(".zip")) value.dropLast(4) else value
        if (stem in byRom) return stem
        return copySuffix.replace(stem, "").takeIf { it != stem && it in byRom }
    }
    fun find(raw: String?): Game? {
        if (raw == null) return null
        return romKey(raw)?.let(byRom::get) ?: byTitle[raw.trim().lowercase(Locale.ROOT)]
    }

    // Only confirmed cloneof relationships; old unverified aliases are intentionally absent.
    private val confirmedParents = """
3wonderu:3wonders,aburner:aburner2,aliensu:aliens,aligatun:aligator,arknoidu:arkanoid,armwaru:armwar,astormbl:astorm,avspu:avsp,bbros:pang,berzerk1:berzerk,bnj:brubber,btime2:btime,bubbobr1:bublbobl,bubsympu:bublbob2,buckyua:bucky,candance:osman,captavnu:captaven,captcomu:captcomm,cawingu:cawing,centtime:centiped,chplftb:chplft,commandu:commando,crimfgt2:crimfght,dassault:thndzone,ddragn2u:ddragon2,ddragonu:ddragon,ddsomu:ddsom,ddtodu:ddtod,defendg:defender,dinou:dino,dkongjo:dkong,dkongjo1:dkong,dkongo:dkong,dstlku:dstlk,endurobl:enduror,ffightj:ffight,ffightu:ffight,gaiden:shadoww,galagamw:galaga,galagao:galaga,gaunt22p:gaunt2,gaunt2p:gauntlet,ghoulsu:ghouls,gijoeu:gijoe,gnga:gng,gngt:gng,gradius:nemesis,gradius2:vulcan,grdius3e:gradius3,growlu:growl,gunsmoku:gunsmoke,heatbrlu:heatbrl,hharryu:hharry,hooku:hook,ikari3nr:ikari3,ikarijpb:ikari,jungleh:junglek,knightsu:knights,kodu:kod,landmkrp:landmakr,lifefrce:salamand,lightbr:dungeonm,mainev2p:mainevt,mbombrd:slammast,moonwlkb:moonwalk,mooua:moo,mshvsfj:mshvsf,mswordu:msword,mvscj:mvsc,mvscu:mvsc,mystwaru:mystwarr,outrunb:outrun,pacman:puckman,pacmanf:puckman,popeyef:popeye,punishru:punisher,punksht2:punkshot,puyopuya:puyopuyo,puzloopu:puzzloop,ramprt2p:rampart,rastanu:rastan,ringking:kingofb,robocp2u:robocop2,rohgau:rohga,rtypeu:rtype,rushatck:gberet,seganinu:seganinj,sf1us:sf1,sf2j:sf2,sf2t:sf2ce,shangonb:shangon,shaolins:kicker,sidearmr:sidearms,simpsn2p:simpsons,slammasu:slammast,snowbrob:snowbros,ssrdrubc:ssriders,thndrbdj:thndrbld,tmnt22p:tmnt2,tmnt2po:tmnt,tmntj:tmnt,tmntu:tmnt,tnzsb:tnzs,tokiu:toki,typhoon:ajax,umk3:mk3,varthu:varth,vendet2p:vendetta,vigilntu:vigilant,vsavu:vsav,wbmlb:wbml,wboyu:wboy,wofu:wof,xmcotau:xmcota,xmen2p:xmen,xmvsfu:xmvsf,xmvsfur1:xmvsf
""".trim().split(',').associate { it.substringBefore(':') to it.substringAfter(':') }
    /** Immediate documented cloneof parent; not necessarily an interchangeable named game. */
    fun canonicalParent(raw: String?): String? = raw?.let(::romKey)?.let(confirmedParents::get)
}