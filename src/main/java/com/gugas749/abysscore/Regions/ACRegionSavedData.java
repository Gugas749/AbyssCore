package com.gugas749.abysscore.Regions;

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

    public boolean addRegion(String name, String dimension, BlockPos first, BlockPos second) {
        if (regions.containsKey(name)) {
            return false;
        }

        regions.put(name, ACRegion.fromCorners(name, dimension, first, second));
        setDirty();
        return true;
    }

    public boolean isProtected(String dimension, BlockPos pos) {
        for (ACRegion region : regions.values()) {
            if (region.contains(dimension, pos)) {
                return true;
            }
        }

        return false;
    }

    public Collection<ACRegion> regions() {
        return regions.values();
    }
}
