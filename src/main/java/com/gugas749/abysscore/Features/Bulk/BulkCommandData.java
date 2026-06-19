package com.gugas749.abysscore.Features.Bulk;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stored bulk command definition.
 *
 * name — For: /abysscore run <name>
 * permLevel — required permission level (0 = everyone, 2 = OP)
 * commands - list of commands to be executed in order
 */
public class BulkCommandData {

    public String name;
    public int permLevel;
    public List<String> commands;

    public BulkCommandData() {
        this.commands = new ArrayList<>();
    }

    public BulkCommandData(String name, int permLevel, List<String> commands) {
        this.name = name;
        this.permLevel = permLevel;
        this.commands = new ArrayList<>(commands);
    }

    @Override
    public String toString() {
        return "BulkCommand{name='" + name + "', perm=" + permLevel + ", cmds=" + commands + "}";
    }
}
