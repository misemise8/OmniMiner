package net.misemise;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redirects only drops created while a tracked block is being broken.
 *
 * <p>Items and experience remain normal entities. Vanilla pickup therefore
 * still handles inventory limits, pickup statistics, Mending, and any
 * remainder that the player cannot carry.</p>
 */
public final class AutoCollector {
    private static final double MERGE_SEARCH_MARGIN = 1.0D;
    private static final Deque<DropCapture> ACTIVE_CAPTURES = new ArrayDeque<>();
    private static final List<DropCapture> ORIGINAL_BREAK_CAPTURES = new ArrayList<>();
    private static boolean registered;

    private AutoCollector() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        ServerEntityEvents.ALLOW_LOAD.register((entity, world, reason, loadedFromDisk) -> {
            if (!loadedFromDisk) {
                redirectSpawnedDrop(entity, world);
            }
            return true;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> finishOriginalBreakCaptures());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
        registered = true;
    }

    /**
     * Opens a short capture window from Fabric's AFTER event until the end of
     * the current server tick. Vanilla creates the original block's drops
     * immediately after that event.
     */
    public static void captureOriginalBreak(
            ServerLevel world,
            BlockPos pos,
            ServerPlayer player,
            boolean collectItems,
            boolean collectExperience) {
        if (!collectItems && !collectExperience) {
            return;
        }

        ORIGINAL_BREAK_CAPTURES.add(snapshot(
                world,
                pos,
                player,
                collectItems,
                collectExperience));
    }

    /**
     * Closes the original-break capture at the return of Minecraft's block
     * destruction method. The end-of-tick listener remains only as an
     * exception-path cleanup fallback.
     */
    public static void finishOriginalBreak(
            ServerLevel world,
            BlockPos pos,
            ServerPlayer player) {
        for (int index = ORIGINAL_BREAK_CAPTURES.size() - 1; index >= 0; index--) {
            DropCapture capture = ORIGINAL_BREAK_CAPTURES.get(index);
            if (capture.world == world
                    && capture.player == player
                    && capture.pos.equals(pos)) {
                ORIGINAL_BREAK_CAPTURES.remove(index);
                capture.finish();
                return;
            }
        }
    }

    /**
     * Brackets one synchronous additional block break.
     */
    public static DropCapture beginAdditionalBreak(
            ServerLevel world,
            BlockPos pos,
            ServerPlayer player,
            boolean collectItems,
            boolean collectExperience) {
        if (!collectItems && !collectExperience) {
            return null;
        }

        DropCapture capture = snapshot(
                world,
                pos,
                player,
                collectItems,
                collectExperience);
        ACTIVE_CAPTURES.addLast(capture);
        return capture;
    }

    public static void finishAdditionalBreak(DropCapture capture) {
        if (capture == null) {
            return;
        }

        if (!ACTIVE_CAPTURES.isEmpty() && ACTIVE_CAPTURES.peekLast() == capture) {
            ACTIVE_CAPTURES.removeLast();
        } else {
            ACTIVE_CAPTURES.remove(capture);
        }
        capture.finish();
    }

    private static void redirectSpawnedDrop(Entity entity, ServerLevel world) {
        DropCapture capture = matchingActiveCapture(entity, world);
        if (capture == null) {
            capture = matchingOriginalCapture(entity, world);
        }
        if (capture != null) {
            capture.redirect(entity);
        }
    }

    private static DropCapture matchingActiveCapture(Entity entity, ServerLevel world) {
        var iterator = ACTIVE_CAPTURES.descendingIterator();
        while (iterator.hasNext()) {
            DropCapture capture = iterator.next();
            if (capture.matches(entity, world)) {
                return capture;
            }
        }
        return null;
    }

    private static DropCapture matchingOriginalCapture(Entity entity, ServerLevel world) {
        for (int index = ORIGINAL_BREAK_CAPTURES.size() - 1; index >= 0; index--) {
            DropCapture capture = ORIGINAL_BREAK_CAPTURES.get(index);
            if (capture.matches(entity, world)) {
                return capture;
            }
        }
        return null;
    }

    private static DropCapture snapshot(
            ServerLevel world,
            BlockPos pos,
            ServerPlayer player,
            boolean collectItems,
            boolean collectExperience) {
        Map<Integer, Integer> itemCounts = new HashMap<>();
        Map<Integer, Integer> experienceValues = new HashMap<>();
        AABB bounds = mergeBounds(pos);

        if (collectItems) {
            for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, bounds)) {
                itemCounts.put(item.getId(), item.getItem().getCount());
            }
        }
        if (collectExperience) {
            for (ExperienceOrb orb : world.getEntitiesOfClass(ExperienceOrb.class, bounds)) {
                experienceValues.put(orb.getId(), orb.getValue());
            }
        }

        return new DropCapture(
                world,
                pos.immutable(),
                player,
                collectItems,
                collectExperience,
                itemCounts,
                experienceValues);
    }

    private static void finishOriginalBreakCaptures() {
        for (DropCapture capture : ORIGINAL_BREAK_CAPTURES) {
            capture.finish();
        }
        ORIGINAL_BREAK_CAPTURES.clear();
    }

    private static void clear() {
        ACTIVE_CAPTURES.clear();
        ORIGINAL_BREAK_CAPTURES.clear();
    }

    private static AABB mergeBounds(BlockPos pos) {
        return new AABB(pos).inflate(MERGE_SEARCH_MARGIN);
    }

    public static final class DropCapture {
        private final ServerLevel world;
        private final BlockPos pos;
        private final ServerPlayer player;
        private final boolean collectItems;
        private final boolean collectExperience;
        private final Map<Integer, Integer> itemCounts;
        private final Map<Integer, Integer> experienceValues;
        private boolean finished;

        private DropCapture(
                ServerLevel world,
                BlockPos pos,
                ServerPlayer player,
                boolean collectItems,
                boolean collectExperience,
                Map<Integer, Integer> itemCounts,
                Map<Integer, Integer> experienceValues) {
            this.world = world;
            this.pos = pos;
            this.player = player;
            this.collectItems = collectItems;
            this.collectExperience = collectExperience;
            this.itemCounts = itemCounts;
            this.experienceValues = experienceValues;
        }

        private boolean matches(Entity entity, ServerLevel entityWorld) {
            if (finished
                    || world != entityWorld
                    || player.isRemoved()
                    || player.level() != world
                    || !entity.blockPosition().equals(pos)) {
                return false;
            }

            return (collectItems && entity instanceof ItemEntity)
                    || (collectExperience && entity instanceof ExperienceOrb);
        }

        private void redirect(Entity entity) {
            if (collectItems && entity instanceof ItemEntity item) {
                item.setTarget(player.getUUID());
                item.setNoPickUpDelay();
                item.setPos(player.getX(), player.getY(), player.getZ());
            } else if (collectExperience && entity instanceof ExperienceOrb orb) {
                orb.setPos(player.getX(), player.getY(), player.getZ());
            }
        }

        /**
         * Handles the vanilla XP-orb merge path, which can increase an
         * existing entity instead of loading a new entity.
         */
        private void finish() {
            if (finished) {
                return;
            }
            finished = true;

            if (player.isRemoved() || player.level() != world) {
                return;
            }

            AABB bounds = mergeBounds(pos);
            if (collectItems) {
                for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, bounds)) {
                    Integer oldCount = itemCounts.get(item.getId());
                    if (oldCount == null || item.getItem().getCount() > oldCount) {
                        redirect(item);
                    }
                }
            }
            if (collectExperience) {
                for (ExperienceOrb orb : world.getEntitiesOfClass(ExperienceOrb.class, bounds)) {
                    Integer oldValue = experienceValues.get(orb.getId());
                    if (oldValue == null || orb.getValue() > oldValue) {
                        redirect(orb);
                    }
                }
            }
        }
    }
}
