"""Pixel maps for the items that Phase 4's mobs drop. Imported by make_textures.py."""


# Glowing Spores: a little cluster of luminous spore pods (left behind by End Pollinators).
GLOWING_SPORES = [
    "................",
    "................",
    "......oo........",
    ".....oGwo...oo..",
    ".....ogGo..owGo.",
    "..oo..oo...ogGo.",
    ".owGo.......oo..",
    ".ogGo..ooo......",
    "..oo..oGwGo.....",
    "......ogGGo..oo.",
    "......oggGo.oGwo",
    ".......ooo..oggo",
    "...oo........oo.",
    "..oGwo..........",
    "..oggo..........",
    "...oo...........",
]
GLOWING_SPORES_PAL = dict(o="1d4a4f", g="2fae9f", G="6ff2da", w="e6fffa")

# Data Shard: a thin crystal wafer with glowing circuitry (from a Void Wanderer's scan).
DATA_SHARD = [
    "................",
    "..........oo....",
    ".........ohwo...",
    "........ohlhlo..",
    ".......ohcllco..",
    "......ohllcclo..",
    ".....ohlcllmo...",
    "....ohllcllmo...",
    "...ohlcclcmo....",
    "..ohllllcmo.....",
    "..olcllmmo......",
    "..olclmmo.......",
    "...ommmo........",
    "....ooo.........",
    "................",
    "................",
]
DATA_SHARD_PAL = dict(o="12202e", m="2d5f7a", l="4b93b3", h="9fe0ff", w="ffffff", c="7dfff0")

# Cracked Lens: the Wanderer's broken eye.
CRACKED_LENS = [
    "................",
    "................",
    ".....oooooo.....",
    "....offfffffo...",
    "...offlllllffo..",
    "..oflllhhlllfo..",
    "..offlhwclllfo..",
    "..oflllhcllfo...",
    "..oflllcllllfo..",
    "..offllclllffo..",
    "...offlcllffo...",
    "....offcfffo....",
    ".....ooooooo....",
    "................",
    "................",
    "................",
]
CRACKED_LENS_PAL = dict(o="231a33", f="5b5470", l="7b3fd0", h="c79bff", w="ffffff", c="1a1026")

# Singularity Shard: a black shard with a bright rim, the remains of a Black Hole Guardian.
SINGULARITY_SHARD = [
    "................",
    "........rr......",
    ".......rkkr.....",
    "......rkkkr.....",
    ".....rkkpkkr....",
    ".....rkkkkkr....",
    "....rkkkpkkkr...",
    "....rkkkkkkkr...",
    "...rkkpkkkkkkr..",
    "...rkkkkkkkkr...",
    "....rkkkkkpkr...",
    "....rkkkkkkr....",
    ".....rkkkkr.....",
    "......rkkr......",
    ".......rr.......",
    "................",
]
SINGULARITY_SHARD_PAL = dict(r="f0a8ff", k="07050c", p="3a1d5c")

# Umbral Membrane: a dark, glossy scrap of hide (from Umbral Stalkers).
UMBRAL_MEMBRANE = [
    "................",
    "................",
    "...oo.....oo....",
    "..oddo...oddo...",
    "..odmdoooddmo...",
    "..odmmdddmmdo...",
    "...odmmhmmdo....",
    "...odmhhhmdo....",
    "..odmmhmmmmdo...",
    "..odmmmmmmmdo...",
    "...odmmmmmdo....",
    "....oddmmdo.....",
    ".....oddddo.....",
    "......oooo......",
    "................",
    "................",
]
UMBRAL_MEMBRANE_PAL = dict(o="0b0714", d="1f1733", m="332654", h="7d6ab8")

# Skimmer Film: a thin, glowing, heat-proof membrane (sheared from Corona Skimmers).
SKIMMER_FILM = [
    "................",
    "................",
    "..oooooooooo....",
    "..oyyyyyyyyyo...",
    "..oyaaaaaayyo...",
    "...oyaawaaayo...",
    "...oyaaaaaayyo..",
    "...oyyaaaaaayo..",
    "..oyyaaawaaayo..",
    "..oyaaaaaaayo...",
    "..oyyaaaaayyo...",
    "...oyyyyyyyyo...",
    "...ooooooooo....",
    "................",
    "................",
    "................",
]
SKIMMER_FILM_PAL = dict(o="7a3a12", y="e8913a", a="ffd27a", w="fff6d8")

# Cinder: a glowing ember (from Cinder Mites); a small fuel.
CINDER = [
    "................",
    "................",
    "................",
    "................",
    ".......oo.......",
    ".....oorfoo.....",
    "....orffyfro....",
    "...orfyyyffro...",
    "...ofyywyyfro...",
    "...orfyyyfrro...",
    "....orfffrro....",
    "....oorrrroo....",
    "......oooo......",
    "................",
    "................",
    "................",
]
CINDER_PAL = dict(o="2a1410", r="8a2a14", f="e0561c", y="ffb03a", w="fff0b0")

ITEM_MAPS = {
    "glowing_spores": (GLOWING_SPORES, GLOWING_SPORES_PAL),
    "data_shard": (DATA_SHARD, DATA_SHARD_PAL),
    "cracked_lens": (CRACKED_LENS, CRACKED_LENS_PAL),
    "singularity_shard": (SINGULARITY_SHARD, SINGULARITY_SHARD_PAL),
    "umbral_membrane": (UMBRAL_MEMBRANE, UMBRAL_MEMBRANE_PAL),
    "skimmer_film": (SKIMMER_FILM, SKIMMER_FILM_PAL),
    "cinder": (CINDER, CINDER_PAL),
}

# Star Chart: a rolled-out chart of dark vellum with a few bright stars and a route drawn on it.
STAR_CHART = [
    "................",
    "..oooooooooooo..",
    ".obppppppppppbo.",
    ".obpdpppppwppbo.",
    ".obppplppppppbo.",
    ".obpppplpppdpbo.",
    ".obpwppplppppbo.",
    ".obpppppllpppbo.",
    ".obpppdppppwpbo.",
    ".obppppppplppbo.",
    ".obpplppppppbo..",
    ".obpwplllpdppbo.",
    ".obppppppppppbo.",
    ".obbbbbbbbbbbbo.",
    "..oooooooooooo..",
    "................",
]
STAR_CHART_PAL = dict(o="2a1d0f", b="8a6a3a", p="1f2340", d="6a75b0", w="fff6c8", l="c9a86a")
ITEM_MAPS["star_chart"] = (STAR_CHART, STAR_CHART_PAL)
