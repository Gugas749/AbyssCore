package com.gugas749.abysscore.Regions;

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
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        Set<String> tags
) {

    // Compact constructor — ensures tags is always an independent mutable set
    public ACRegion {
        tags = new LinkedHashSet<>(tags);
    }

    public static ACRegion fromCorners(String name, String dimension, BlockPos first, BlockPos second) {
        return new ACRegion(
                name,
                dimension,
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ()),
                new LinkedHashSet<>() // no tags by default
        );
    }

    public static ACRegion load(CompoundTag tag) {
        Set<String> tags = new LinkedHashSet<>();
        // Load the tags list — each entry is a StringTag
        ListTag tagList = tag.getList("tags", Tag.TAG_STRING);
        for (int i = 0; i < tagList.size(); i++) {
            tags.add(tagList.getString(i));
        }

        return new ACRegion(
                tag.getString("name"),
                tag.getString("dimension"),
                tag.getInt("minX"),
                tag.getInt("minY"),
                tag.getInt("minZ"),
                tag.getInt("maxX"),
                tag.getInt("maxY"),
                tag.getInt("maxZ"),
                tags
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("dimension", dimension);
        tag.putInt("minX", minX);
        tag.putInt("minY", minY);
        tag.putInt("minZ", minZ);
        tag.putInt("maxX", maxX);
        tag.putInt("maxY", maxY);
        tag.putInt("maxZ", maxZ);

        // Save tags as a ListTag of StringTags
        ListTag tagList = new ListTag();
        for (String t : tags) {
            tagList.add(StringTag.valueOf(t));
        }
        tag.put("tags", tagList);

        return tag;
    }

    public boolean contains(String dimension, BlockPos pos) {
        return this.dimension.equals(dimension)
                && pos.getX() >= minX
                && pos.getX() <= maxX
                && pos.getY() >= minY
                && pos.getY() <= maxY
                && pos.getZ() >= minZ
                && pos.getZ() <= maxZ;
    }

    /** Returns true if this region has the given protection tag. */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    /** Returns an unmodifiable view of this region's tags. */
    public Set<String> tags() {
        return Collections.unmodifiableSet(tags);
    }

    /** Returns a new ACRegion with the given tag added. */
    public ACRegion withTag(String tag) {
        Set<String> newTags = new LinkedHashSet<>(tags);
        newTags.add(tag);
        return new ACRegion(name, dimension, minX, minY, minZ, maxX, maxY, maxZ, newTags);
    }

    /** Returns a new ACRegion with the given tag removed. */
    public ACRegion withoutTag(String tag) {
        Set<String> newTags = new LinkedHashSet<>(tags);
        newTags.remove(tag);
        return new ACRegion(name, dimension, minX, minY, minZ, maxX, maxY, maxZ, newTags);
    }
}
