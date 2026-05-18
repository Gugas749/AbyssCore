package com.gugas749.abysscore.Regions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public class ACWorldEditSelection {

    public static Optional<Selection> getSelection(ServerPlayer player) throws SelectionUnavailableException {
        try {
            Class<?> adapterClass = Class.forName("com.sk89q.worldedit.neoforge.NeoForgeAdapter");
            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            Class<?> sessionOwnerClass = Class.forName("com.sk89q.worldedit.session.SessionOwner");
            Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World");

            Method adaptPlayer = adapterClass.getMethod("adaptPlayer", ServerPlayer.class);
            Method adaptWorld = adapterClass.getMethod("adapt", ServerLevel.class);
            Method getInstance = worldEditClass.getMethod("getInstance");

            Object worldEditPlayer = adaptPlayer.invoke(null, player);
            Object worldEditWorld = adaptWorld.invoke(null, player.serverLevel());
            Object worldEdit = getInstance.invoke(null);
            Object sessionManager = worldEditClass.getMethod("getSessionManager").invoke(worldEdit);
            Object localSession = sessionManager.getClass().getMethod("get", sessionOwnerClass).invoke(sessionManager, worldEditPlayer);
            Object region = localSession.getClass().getMethod("getSelection", worldClass).invoke(localSession, worldEditWorld);

            Object min = region.getClass().getMethod("getMinimumPoint").invoke(region);
            Object max = region.getClass().getMethod("getMaximumPoint").invoke(region);

            return Optional.of(new Selection(toBlockPos(min), toBlockPos(max)));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null && "com.sk89q.worldedit.IncompleteRegionException".equals(cause.getClass().getName())) {
                throw new SelectionUnavailableException(SelectionUnavailableReason.INCOMPLETE);
            }
            throw new SelectionUnavailableException(SelectionUnavailableReason.ERROR, cause);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new SelectionUnavailableException(SelectionUnavailableReason.ERROR, e);
        }
    }

    private static BlockPos toBlockPos(Object blockVector) throws ReflectiveOperationException {
        int x = (int) blockVector.getClass().getMethod("getBlockX").invoke(blockVector);
        int y = (int) blockVector.getClass().getMethod("getBlockY").invoke(blockVector);
        int z = (int) blockVector.getClass().getMethod("getBlockZ").invoke(blockVector);
        return new BlockPos(x, y, z);
    }

    public record Selection(BlockPos min, BlockPos max) {
    }

    public static class SelectionUnavailableException extends Exception {
        private final SelectionUnavailableReason reason;

        public SelectionUnavailableException(SelectionUnavailableReason reason) {
            this(reason, null);
        }

        public SelectionUnavailableException(SelectionUnavailableReason reason, Throwable cause) {
            super(cause);
            this.reason = reason;
        }

        public SelectionUnavailableReason reason() {
            return reason;
        }
    }

    public enum SelectionUnavailableReason {
        INCOMPLETE,
        ERROR
    }
}
