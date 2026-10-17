# Инвентаризация профилей possession

Снимок исходных datapack-профилей и тегов. После переноса все допустимые Mob вне транспорта используют Player Body. «native travel» в исторической таблице означает отсутствие специального профиля, а не второй вызов host.travel. Реально проверенные виды перечислены в POSSESSION_REFACTOR_REPORT.md. Наличие профиля само по себе не разрешает вселение: ограничения души и POSSESSION_BLACKLIST проверяются отдельно. Неизвестные существа с извлечённой душой также могут быть допустимы по текущим правилам проекта.

| ID | Профиль | Допуск по статическим тегам |
|---|---|---|
| `adventurez:blaze_guardian` | полёт FORCED | зависит от души/тегов |
| `adventurez:necromancer` | native travel | перечислен |
| `adventurez:skeleton_vanguard` | native travel | перечислен |
| `adventurez:soul_reaper` | native travel | перечислен |
| `adventurez:summoner` | native travel | перечислен |
| `adventurez:wither_puppet` | native travel | перечислен |
| `alexsmobs:alligator_snapping_turtle` | вода ENABLED | перечислен |
| `alexsmobs:anaconda` | вода ENABLED | перечислен |
| `alexsmobs:anteater` | native travel | перечислен |
| `alexsmobs:bald_eagle` | полёт ENABLED | перечислен |
| `alexsmobs:banana_slug` | native travel | перечислен |
| `alexsmobs:bison` | native travel | перечислен |
| `alexsmobs:blobfish` | вода FORCED | перечислен |
| `alexsmobs:blue_jay` | полёт ENABLED | перечислен |
| `alexsmobs:bone_serpent` | вода FLOATING | перечислен |
| `alexsmobs:bunfungus` | прыжки | перечислен |
| `alexsmobs:cachalot_whale` | вода FORCED | перечислен |
| `alexsmobs:caiman` | вода ENABLED | перечислен |
| `alexsmobs:capuchin_monkey` | лазание | перечислен |
| `alexsmobs:catfish` | вода FORCED | перечислен |
| `alexsmobs:cave_centipede` | лазание | перечислен |
| `alexsmobs:centipede_head` | лазание | перечислен |
| `alexsmobs:cockroach` | лазание | перечислен |
| `alexsmobs:comb_jelly` | вода FORCED | перечислен |
| `alexsmobs:cosmaw` | полёт FORCED | перечислен |
| `alexsmobs:cosmic_cod` | вода FORCED | перечислен |
| `alexsmobs:crimson_mosquito` | полёт FORCED | перечислен |
| `alexsmobs:crocodile` | вода ENABLED | перечислен |
| `alexsmobs:crow` | полёт ENABLED | перечислен |
| `alexsmobs:devils_hole_pupfish` | вода FORCED | перечислен |
| `alexsmobs:dropbear` | лазание | перечислен |
| `alexsmobs:elephant` | native travel | перечислен |
| `alexsmobs:emu` | native travel | перечислен |
| `alexsmobs:endergrade` | полёт FORCED | перечислен |
| `alexsmobs:enderiophage` | полёт FORCED | перечислен |
| `alexsmobs:farseer` | полёт FORCED | blacklist |
| `alexsmobs:flutter` | полёт FORCED | перечислен |
| `alexsmobs:fly` | полёт FORCED | перечислен |
| `alexsmobs:flying_fish` | полёт ENABLED, вода FORCED | перечислен |
| `alexsmobs:frilled_shark` | вода FORCED | перечислен |
| `alexsmobs:froststalker` | native travel | перечислен |
| `alexsmobs:gazelle` | native travel | перечислен |
| `alexsmobs:gelada_monkey` | лазание | перечислен |
| `alexsmobs:ghost_miner` | native travel | перечислен |
| `alexsmobs:giant_squid` | вода FORCED | перечислен |
| `alexsmobs:gorilla` | лазание | перечислен |
| `alexsmobs:grizzly_bear` | вода ENABLED | перечислен |
| `alexsmobs:guster` | полёт FORCED | перечислен |
| `alexsmobs:hammerhead_shark` | вода FORCED | перечислен |
| `alexsmobs:hummingbird` | полёт FORCED | перечислен |
| `alexsmobs:jerboa` | прыжки | перечислен |
| `alexsmobs:kangaroo` | прыжки | перечислен |
| `alexsmobs:komodo_dragon` | native travel | перечислен |
| `alexsmobs:laviathan` | вода FLOATING | перечислен |
| `alexsmobs:leafcutter_ant` | лазание | перечислен |
| `alexsmobs:leafcutter_ant_queen` | лазание | перечислен |
| `alexsmobs:lobster` | вода ENABLED | перечислен |
| `alexsmobs:maned_wolf` | native travel | перечислен |
| `alexsmobs:mantis_shrimp` | вода ENABLED | перечислен |
| `alexsmobs:mimic_octopus` | вода FORCED | перечислен |
| `alexsmobs:mimicube` | вода FLOATING, прыжки | перечислен |
| `alexsmobs:moose` | native travel | перечислен |
| `alexsmobs:mudskipper` | вода ENABLED, прыжки | перечислен |
| `alexsmobs:mungus` | native travel | перечислен |
| `alexsmobs:murmur` | лазание | перечислен |
| `alexsmobs:murmur_head` | полёт FORCED | перечислен |
| `alexsmobs:orca` | вода FORCED | перечислен |
| `alexsmobs:platypus` | вода ENABLED | перечислен |
| `alexsmobs:potoo` | полёт ENABLED | перечислен |
| `alexsmobs:raccoon` | лазание | перечислен |
| `alexsmobs:rain_frog` | прыжки | перечислен |
| `alexsmobs:rattlesnake` | native travel | перечислен |
| `alexsmobs:rhinoceros` | native travel | перечислен |
| `alexsmobs:roadrunner` | native travel | перечислен |
| `alexsmobs:rocky_roller` | прыжки | перечислен |
| `alexsmobs:sea_bear` | вода ENABLED | перечислен |
| `alexsmobs:seagull` | полёт ENABLED | перечислен |
| `alexsmobs:seal` | вода ENABLED | перечислен |
| `alexsmobs:shoebill` | полёт ENABLED | перечислен |
| `alexsmobs:skelewag` | вода FORCED | перечислен |
| `alexsmobs:skreecher` | лазание | перечислен |
| `alexsmobs:skunk` | native travel | перечислен |
| `alexsmobs:snow_leopard` | native travel | перечислен |
| `alexsmobs:soul_vulture` | полёт ENABLED | перечислен |
| `alexsmobs:spectre` | полёт FORCED | перечислен |
| `alexsmobs:straddler` | вода FLOATING | перечислен |
| `alexsmobs:stradpole` | вода FLOATING | перечислен |
| `alexsmobs:sugar_glider` | полёт ENABLED | перечислен |
| `alexsmobs:sunbird` | полёт FORCED | перечислен |
| `alexsmobs:tarantula_hawk` | полёт ENABLED | перечислен |
| `alexsmobs:tasmanian_devil` | native travel | перечислен |
| `alexsmobs:terrapin` | вода ENABLED | перечислен |
| `alexsmobs:tiger` | native travel | перечислен |
| `alexsmobs:toucan` | полёт ENABLED | перечислен |
| `alexsmobs:triops` | вода FORCED | перечислен |
| `alexsmobs:tusklin` | native travel | перечислен |
| `alexsmobs:underminer` | native travel | перечислен |
| `alexsmobs:void_worm` | полёт FORCED | blacklist |
| `alexsmobs:warped_mosco` | полёт ENABLED | перечислен |
| `alexsmobs:warped_toad` | вода ENABLED, прыжки | перечислен |
| `betterend:shadow_walker` | native travel | перечислен |
| `betternether:jungle_skeleton` | native travel | перечислен |
| `bewitchment:owl` | полёт ENABLED | зависит от души/тегов |
| `bewitchment:raven` | полёт ENABLED | зависит от души/тегов |
| `biomemakeover:blightbat` | полёт FORCED | зависит от души/тегов |
| `biomemakeover:decayed` | native travel | перечислен |
| `biomemakeover:ghost` | полёт FORCED | blacklist |
| `biomemakeover:giant_slime` | вода FLOATING, прыжки | зависит от души/тегов |
| `biomemakeover:lightning_bug` | полёт FORCED | зависит от души/тегов |
| `biomemakeover:lightning_bug_alternate` | полёт FORCED | зависит от души/тегов |
| `biomemakeover:moth` | полёт FORCED | зависит от души/тегов |
| `biomemakeover:owl` | полёт ENABLED | зависит от души/тегов |
| `biomemakeover:stone_golem` | неподвижный | перечислен |
| `biomemakeover:tadpole` | вода FORCED | зависит от души/тегов |
| `golemsgalore:anti_creeper_golem` | native travel | перечислен |
| `golemsgalore:diamond_golem` | native travel | перечислен |
| `golemsgalore:diamond_laser_golem` | native travel | перечислен |
| `golemsgalore:golden_golem` | native travel | перечислен |
| `golemsgalore:hay_golem` | native travel | перечислен |
| `golemsgalore:laser_golem` | native travel | перечислен |
| `golemsgalore:netherite_golem` | native travel | перечислен |
| `golemsgalore:obsidian_golem` | native travel | перечислен |
| `golemsgalore:quartz_golem` | native travel | перечислен |
| `minecraft:allay` | полёт FORCED | перечислен |
| `minecraft:axolotl` | вода ENABLED | перечислен |
| `minecraft:bat` | полёт FORCED | перечислен |
| `minecraft:bee` | полёт FORCED | перечислен |
| `minecraft:blaze` | полёт FORCED | перечислен |
| `minecraft:camel` | native travel | перечислен |
| `minecraft:cat` | native travel | перечислен |
| `minecraft:cave_spider` | лазание | перечислен |
| `minecraft:chicken` | native travel | перечислен |
| `minecraft:cod` | вода FORCED | перечислен |
| `minecraft:cow` | native travel | перечислен |
| `minecraft:dolphin` | вода FORCED | перечислен |
| `minecraft:donkey` | native travel | перечислен |
| `minecraft:drowned` | вода ENABLED | перечислен |
| `minecraft:elder_guardian` | вода FORCED | зависит от души/тегов |
| `minecraft:ender_dragon` | полёт FORCED | зависит от души/тегов |
| `minecraft:enderman` | native travel | перечислен |
| `minecraft:endermite` | native travel | перечислен |
| `minecraft:evoker` | native travel | перечислен |
| `minecraft:fox` | native travel | перечислен |
| `minecraft:frog` | native travel | перечислен |
| `minecraft:ghast` | полёт FORCED | перечислен |
| `minecraft:giant` | native travel | перечислен |
| `minecraft:glow_squid` | вода FORCED | перечислен |
| `minecraft:goat` | native travel | перечислен |
| `minecraft:guardian` | вода FORCED | перечислен |
| `minecraft:hoglin` | native travel | перечислен |
| `minecraft:horse` | native travel | перечислен |
| `minecraft:husk` | native travel | перечислен |
| `minecraft:iron_golem` | вода SINKING | перечислен |
| `minecraft:llama` | native travel | перечислен |
| `minecraft:magma_cube` | вода FLOATING, прыжки | перечислен |
| `minecraft:mooshroom` | native travel | перечислен |
| `minecraft:mule` | native travel | перечислен |
| `minecraft:ocelot` | native travel | перечислен |
| `minecraft:panda` | native travel | перечислен |
| `minecraft:parrot` | полёт ENABLED | перечислен |
| `minecraft:phantom` | полёт FORCED | перечислен |
| `minecraft:pig` | native travel | перечислен |
| `minecraft:piglin` | native travel | перечислен |
| `minecraft:piglin_brute` | native travel | перечислен |
| `minecraft:pillager` | native travel | перечислен |
| `minecraft:polar_bear` | native travel | перечислен |
| `minecraft:pufferfish` | вода FORCED | перечислен |
| `minecraft:rabbit` | прыжки | перечислен |
| `minecraft:ravager` | native travel | перечислен |
| `minecraft:salmon` | вода FORCED | перечислен |
| `minecraft:sheep` | native travel | перечислен |
| `minecraft:shulker` | неподвижный | перечислен |
| `minecraft:silverfish` | native travel | перечислен |
| `minecraft:skeleton_horse` | native travel | перечислен |
| `minecraft:slime` | вода FLOATING, прыжки | перечислен |
| `minecraft:sniffer` | native travel | перечислен |
| `minecraft:snow_golem` | вода SINKING | перечислен |
| `minecraft:spider` | лазание | перечислен |
| `minecraft:squid` | вода FORCED | перечислен |
| `minecraft:strider` | вода FLOATING | перечислен |
| `minecraft:tadpole` | вода FORCED | перечислен |
| `minecraft:trader_llama` | native travel | перечислен |
| `minecraft:tropical_fish` | вода FORCED | перечислен |
| `minecraft:turtle` | вода ENABLED | перечислен |
| `minecraft:vex` | полёт FORCED | перечислен |
| `minecraft:villager` | native travel | перечислен |
| `minecraft:vindicator` | native travel | перечислен |
| `minecraft:wandering_trader` | native travel | перечислен |
| `minecraft:warden` | native travel | blacklist |
| `minecraft:witch` | native travel | перечислен |
| `minecraft:wither` | полёт ENABLED | зависит от души/тегов |
| `minecraft:wolf` | native travel | перечислен |
| `minecraft:zoglin` | native travel | перечислен |
| `minecraft:zombie` | native travel | перечислен |
| `minecraft:zombie_horse` | native travel | перечислен |
| `minecraft:zombie_villager` | native travel | перечислен |
| `minecraft:zombified_piglin` | native travel | перечислен |
| `mobz:armored_entity` | native travel | перечислен |
| `mobz:enderzombie_entity` | native travel | перечислен |
| `mobz:fast_entity` | native travel | перечислен |
| `mobz:frost_entity` | полёт FORCED | зависит от души/тегов |
| `mobz:gchicken_entity` | native travel | зависит от души/тегов |
| `mobz:islandvex_entity` | полёт FORCED | зависит от души/тегов |
| `mobz:skeli1_entity` | native travel | перечислен |
| `mobz:skeli2_entity` | native travel | перечислен |
| `mobz:skeli3_entity` | native travel | перечислен |
| `mobz:skeli4_entity` | native travel | перечислен |
| `mobz:smallzombie_entity` | native travel | перечислен |
| `mobz:tank_entity` | native travel | перечислен |
| `requiemunofficial:cured_piglin` | native travel | перечислен |
| `requiemunofficial:cured_piglin_brute` | native travel | перечислен |
| `requiemunofficial:cured_villager` | native travel | перечислен |
| `requiemunofficial:player_shell` | native travel | перечислен |
| `snowmercy:aftermarket_snowman` | native travel | перечислен |
| `snowmercy:ice_mortar` | native travel | перечислен |
| `snowmercy:mister_chill_snuggles` | native travel | перечислен |
| `snowmercy:mister_snuggles` | native travel | перечислен |
| `snowmercy:sawman` | native travel | перечислен |
