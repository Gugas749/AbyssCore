package com.gugas749.abysscore.Features.Regions;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record ACRegion(
        String name,
        String dimension,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        Set<String> tags,
        String entryFilterTag
) {

    public ACRegion {
        tags = new LinkedHashSet<>(tags);
        if (entryFilterTag == null) entryFilterTag = "";
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public static ACRegion fromCorners(String name, String dimension, BlockPos first, BlockPos second) {
        return new ACRegion(
                name, dimension,
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ()),
                new LinkedHashSet<>(),
                ""
        );
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    public static ACRegion load(CompoundTag tag) {
        Set<String> tags = new LinkedHashSet<>();
        ListTag tagList = tag.getList("tags", Tag.TAG_STRING);
        for (int i = 0; i < tagList.size(); i++) tags.add(tagList.getString(i));

        return new ACRegion(
                tag.getString("name"),
                tag.getString("dimension"),
                tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ"),
                tag.getInt("maxX"), tag.getInt("maxY"), tag.getInt("maxZ"),
                tags,
                tag.contains("entryFilterTag") ? tag.getString("entryFilterTag") : ""
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("dimension", dimension);
        tag.putInt("minX", minX); tag.putInt("minY", minY); tag.putInt("minZ", minZ);
        tag.putInt("maxX", maxX); tag.putInt("maxY", maxY); tag.putInt("maxZ", maxZ);

        ListTag tagList = new ListTag();
        for (String t : tags) tagList.add(StringTag.valueOf(t));
        tag.put("tags", tagList);

        if (entryFilterTag != null && !entryFilterTag.isBlank()) {
            tag.putString("entryFilterTag", entryFilterTag);
        }
        return tag;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean contains(String dimension, BlockPos pos) {
        return this.dimension.equals(dimension)
                && pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public boolean hasTag(String tag) { return tags.contains(tag); }

    public Set<String> tags() { return Collections.unmodifiableSet(tags); }

    public ACRegion withTag(String tag) {
        Set<String> n = new LinkedHashSet<>(tags); n.add(tag);
        return new ACRegion(name, dimension, minX, minY, minZ, maxX, maxY, maxZ, n, entryFilterTag);
    }

    public ACRegion withoutTag(String tag) {
        Set<String> n = new LinkedHashSet<>(tags); n.remove(tag);
        return new ACRegion(name, dimension, minX, minY, minZ, maxX, maxY, maxZ, n, entryFilterTag);
    }

    public ACRegion withEntryFilterTag(String filterTag) {
        return new ACRegion(name, dimension, minX, minY, minZ, maxX, maxY, maxZ, tags, filterTag);
    }
}
