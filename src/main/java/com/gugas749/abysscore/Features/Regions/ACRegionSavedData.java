package com.gugas749.abysscore.Features.Regions;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ACRegionSavedData extends SavedData {

    private static final String DATA_NAME = Abysscore.MODID + "_regions";
    private static final Factory<ACRegionSavedData> FACTORY = new Factory<>(
            ACRegionSavedData::new,
            ACRegionSavedData::load
    );

    private final Map<String, ACRegion> regions = new LinkedHashMap<>();

    public static ACRegionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static ACRegionSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ACRegionSavedData data = new ACRegionSavedData();
        ListTag regionTags = tag.getList("regions", Tag.TAG_COMPOUND);

        for (int i = 0; i < regionTags.size(); i++) {
            ACRegion region = ACRegion.load(regionTags.getCompound(i));
            data.regions.put(region.name(), region);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag regionTags = new ListTag();

        for (ACRegion region : regions.values()) {
            regionTags.add(region.save());
        }

        tag.put("regions", regionTags);
        return tag;
    }

    // ── Region CRUD ───────────────────────────────────────────────────────────

    public boolean addRegion(String name, String dimension, BlockPos first, BlockPos second) {
        if (regions.containsKey(name)) return false;
        regions.put(name, ACRegion.fromCorners(name, dimension, first, second));
        setDirty();
        return true;
    }

    public boolean removeRegion(String name) {
        if (regions.remove(name) == null) return false;
        setDirty();
        return true;
    }

    public Optional<ACRegion> getRegion(String name) {
        return Optional.ofNullable(regions.get(name));
    }

    // ── Tag management ────────────────────────────────────────────────────────

    public boolean addTagToRegion(String regionName, String tag) {
        ACRegion region = regions.get(regionName);
        if (region == null) return false;
        if (region.hasTag(tag)) return false;

        // Records are immutable — replace with a new instance that has the tag
        regions.put(regionName, region.withTag(tag));
        setDirty();
        return true;
    }

    public boolean removeTagFromRegion(String regionName, String tag) {
        ACRegion region = regions.get(regionName);
        if (region == null) return false;
        if (!region.hasTag(tag)) return false;

        regions.put(regionName, region.withoutTag(tag));
        setDirty();
        return true;
    }

    // ── Protection queries ────────────────────────────────────────────────────

    public boolean isRestrictedAt(String dimension, BlockPos pos, String tag) {
        for (ACRegion region : regions.values()) {
            if (region.contains(dimension, pos) && region.hasTag(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Legacy method — kept for backward compatibility.
     * Returns true if the position is inside ANY region (regardless of tags).
     */
    public boolean isProtected(String dimension, BlockPos pos) {
        for (ACRegion region : regions.values()) {
            if (region.contains(dimension, pos)) return true;
        }
        return false;
    }

    public Collection<ACRegion> regions() {
        return regions.values();
    }
}
