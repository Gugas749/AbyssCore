package com.gugas749.abysscore.Dimen;

import com.google.gson.JsonObject;

public class ACDimensionData {

    public enum State {
        PENDING_CREATE,
        LOADED,
        UNLOADED,
        PENDING_REMOVE
    }

    public enum Style {
        NORMAL,
        SUPERFLAT,
        VOID;

        public static Style fromString(String s) {
            return switch (s.toLowerCase()) {
                case "superflat" -> SUPERFLAT;
                case "void"      -> VOID;
                default          -> NORMAL;
            };
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    public String name;
    public String displayName;
    public Style style;
    public long seed;
    public State state;

    // ── Options (editable after creation) ────────────────────────────────────

    public boolean generateStructures = true;
    public String  difficulty         = "normal";  // peaceful, easy, normal, hard
    public boolean spawnMobs          = true;
    public boolean spawnAnimals       = true;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ACDimensionData() {}

    public ACDimensionData(String name, String displayName, Style style, long seed) {
        this.name        = name;
        this.displayName = displayName;
        this.style       = style;
        this.seed        = seed;
        this.state       = State.PENDING_CREATE;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("displayName", displayName);
        obj.addProperty("style", style.name());
        obj.addProperty("seed", seed);
        obj.addProperty("state", state.name());
        obj.addProperty("generateStructures", generateStructures);
        obj.addProperty("difficulty", difficulty);
        obj.addProperty("spawnMobs", spawnMobs);
        obj.addProperty("spawnAnimals", spawnAnimals);
        return obj;
    }

    public static ACDimensionData fromJson(JsonObject obj) {
        ACDimensionData d = new ACDimensionData();
        d.name = obj.get("name").getAsString();
        d.displayName = obj.get("displayName").getAsString();
        d.style = Style.fromString(obj.get("style").getAsString());
        d.seed = obj.get("seed").getAsLong();
        d.state = State.valueOf(obj.get("state").getAsString());
        d.generateStructures = obj.has("generateStructures") && obj.get("generateStructures").getAsBoolean();
        d.difficulty = obj.has("difficulty") ? obj.get("difficulty").getAsString() : "normal";
        d.spawnMobs = !obj.has("spawnMobs") || obj.get("spawnMobs").getAsBoolean();
        d.spawnAnimals = !obj.has("spawnAnimals") || obj.get("spawnAnimals").getAsBoolean();
        return d;
    }
}
