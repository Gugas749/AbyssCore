package com.gugas749.abysscore.Features.title;

import com.google.gson.JsonObject;

/**
 * Represents a saved title configuration.
 */
public class ACTitle {

    public String id;           // unique internal ID (UUID string)
    public String name;         // display name for the list
    public String titleText;    // main title text (supports §color codes)
    public String subtitleText; // subtitle text (can be empty)
    public int fadeIn   = 10;   // ticks
    public int stay     = 70;   // ticks
    public int fadeOut  = 20;   // ticks

    public ACTitle() {}

    public ACTitle(String id, String name, String titleText, String subtitleText,
                   int fadeIn, int stay, int fadeOut) {
        this.id           = id;
        this.name         = name;
        this.titleText    = titleText;
        this.subtitleText = subtitleText;
        this.fadeIn       = fadeIn;
        this.stay         = stay;
        this.fadeOut      = fadeOut;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id",           id);
        obj.addProperty("name",         name);
        obj.addProperty("titleText",    titleText);
        obj.addProperty("subtitleText", subtitleText);
        obj.addProperty("fadeIn",       fadeIn);
        obj.addProperty("stay",         stay);
        obj.addProperty("fadeOut",      fadeOut);
        return obj;
    }

    public static ACTitle fromJson(JsonObject obj) {
        ACTitle t = new ACTitle();
        t.id           = obj.get("id").getAsString();
        t.name         = obj.get("name").getAsString();
        t.titleText    = obj.get("titleText").getAsString();
        t.subtitleText = obj.has("subtitleText") ? obj.get("subtitleText").getAsString() : "";
        t.fadeIn       = obj.has("fadeIn")  ? obj.get("fadeIn").getAsInt()  : 10;
        t.stay         = obj.has("stay")    ? obj.get("stay").getAsInt()    : 70;
        t.fadeOut      = obj.has("fadeOut") ? obj.get("fadeOut").getAsInt() : 20;
        return t;
    }
}
