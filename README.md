# AbyssCore

Core utility mod for modpacks.

AbyssCore adds player restriction tags, saved regions, Figura reload helpers, reusable bulk commands, command key binds, and staff mode profile swapping.

## Command Permissions

Most AbyssCore management commands require permission level 2, the same level normally used by operators.

The exceptions are `/abysscore run <name>` and `/abysscore bind ...`. A saved bulk command can be configured as either available to everyone or OP-only, and players can manage their own key binds.

## Commands

### Player Protection Tags

Use protection tags to restrict what selected players can do.

```mcfunction
/abysscore protect <targets> add <tag>
/abysscore protect <targets> remove <tag>
/abysscore protect <targets> status
```

Available tags:

| Tag | Effect |
| --- | --- |
| `no_build` | Blocks breaking and placing blocks. |
| `no_interact` | Blocks right-click interaction with blocks and entities, such as chests, buttons, doors, villagers, and item frames. |
| `no_fly` | Prevents flight and removes non-creative flight permission. |
| `no_friendlyfire` | Blocks the tagged player from attacking other players. |
| `no_hunger` | Keeps the player's hunger at full food with saturation. |

Examples:

```mcfunction
/abysscore protect Steve add no_build
/abysscore protect @a add no_hunger
/abysscore protect Steve remove no_build
/abysscore protect Steve status
```

Notes:

- `<targets>` accepts Minecraft player selectors such as `Steve`, `@a`, `@p`, and `@r`.
- These restrictions are stored as vanilla scoreboard tags on the player.
- The same tags can also be managed manually with vanilla `/tag`, but `/abysscore protect` validates the tag names and gives clearer feedback.

### Staff Mode

Staff mode swaps a player's normal survival profile with a separate staff profile.

```mcfunction
/abysscore staff
/abysscore staff <player>
/abysscore staff status <player>
/abysscore staff reset <player>
```

Usage:

- `/abysscore staff` toggles staff mode for yourself.
- `/abysscore staff <player>` toggles staff mode for an online player.
- `/abysscore staff status <player>` shows whether a player is currently in staff mode.
- `/abysscore staff reset <player>` deletes that player's saved staff profile. The player must not currently be in staff mode.

Notes:

- Profiles are saved in `config/abysscore_staff_profiles`.
- The swapped profile includes inventory, ender chest, health, hunger, XP, abilities, attributes, active effects, and game mode.
- Position, dimension, spawn point, recipes, advancements, and UUID are not swapped.

### Regions

Use regions to save named areas in the current dimension.

```mcfunction
/abysscore region add <name>
/abysscore region add <name> <from> <to>
/abysscore region remove <name>
/abysscore region list
```

Usage:

- `/abysscore region add <name>` reads the current WorldEdit selection from the executing player.
- `/abysscore region add <name> <from> <to>` creates a region from two block positions and does not require WorldEdit.
- `/abysscore region remove <name>` deletes a saved region.
- `/abysscore region list` shows all saved regions for the current level data.

Examples:

```mcfunction
/abysscore region add spawn
/abysscore region add spawn 0 64 0 100 90 100
/abysscore region list
/abysscore region remove spawn
```

Notes:

- Region names use a single word, such as `spawn`, `market`, or `event_area`.
- WorldEdit support is optional. If WorldEdit is not installed, use the explicit `<from> <to>` form.
- Regions are saved with the dimension id, for example `minecraft:overworld`.

### Figura

Use Figura commands to trigger avatar reloads for online players.

```mcfunction
/abysscore figura reloadall
/abysscore figura reloadall force
```

Usage:

- `/abysscore figura reloadall` sends a packet to every online player that runs Figura's client reload command.
- `/abysscore figura reloadall force` checks that Figura is installed, then tells clients to clear loaded avatar data.

Examples:

```mcfunction
/abysscore figura reloadall
/abysscore figura reloadall force
```

Notes:

- The normal reload path calls the client's `figura reload` command.
- The force path requires Figura classes to be present.
- These commands affect online players only.

### Bulk Commands

Bulk commands let operators create a named command bundle, then run it later with one command.

```mcfunction
/abysscore bulk add
/abysscore bulk remove <name>
/abysscore bulk list
/abysscore run <name>
```

Usage:

- `/abysscore bulk add` opens the client-side creation screen for the operator running the command.
- In the creation screen, choose a command name, permission level, and up to 10 sub-commands.
- Command names must use only lowercase letters, numbers, and underscores.
- Sub-commands should be entered without the leading `/`.
- `/abysscore bulk remove <name>` deletes a saved bulk command.
- `/abysscore bulk list` shows saved bulk commands and their permission setting.
- `/abysscore run <name>` executes each saved sub-command as the command source that ran it.

Permission levels:

| Setting | Who can run it |
| --- | --- |
| Everyone | Any player can run `/abysscore run <name>`. |
| OP Only | Only sources with permission level 2 can run `/abysscore run <name>`. |

Examples:

```mcfunction
/abysscore bulk add
/abysscore bulk list
/abysscore run starter_kit
/abysscore bulk remove starter_kit
```

Example sub-commands for a bulk command:

```mcfunction
effect give @s minecraft:speed 10 1
effect give @s minecraft:night_vision 10 0
give @s minecraft:bread 16
```

Notes:

- Saved bulk commands are stored in `config/abysscore_bulk_commands.json`.
- If one sub-command fails, AbyssCore reports the failed sub-command and continues reporting how many commands ran successfully.
- Because sub-commands execute from the source that ran `/abysscore run <name>`, selectors like `@s` refer to that player or command source.

### Key Binds

Key binds let players assign one of 9 AbyssCore bind slots to either a saved bulk command or a raw command.

```mcfunction
/abysscore bind set <slot> <bulkName or raw command>
/abysscore bind clear <slot>
/abysscore bind list
```

Usage:

- `/abysscore bind set <slot> <bulkName or raw command>` saves a command for one bind slot.
- `<slot>` must be a number from `1` to `9`.
- If the value matches a saved bulk command name, the bind runs that bulk command.
- If the value does not match a saved bulk command name, the bind runs it as a raw command.
- Raw commands should be entered without the leading `/`.
- `/abysscore bind clear <slot>` removes the command saved in that slot.
- `/abysscore bind list` shows all 9 slots and their current values.
- The actual keys are assigned in Minecraft's Controls menu under the AbyssCore category.

Examples:

```mcfunction
/abysscore bind set 1 starter_kit
/abysscore bind set 2 effect give @s minecraft:speed 10 1
/abysscore bind list
/abysscore bind clear 1
```

Notes:

- Bind slots are player-specific.
- Saved binds are stored in `config/abysscore_binds/<uuid>.json`.
- Bound bulk commands still respect their permission setting when triggered from a key.
- Bound raw commands execute as the player who pressed the key, so selectors like `@s` refer to that player.

## License

[BSD 3-Clause License](LICENSE) - Copyright 2026 Gugas749
