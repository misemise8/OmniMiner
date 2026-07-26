package net.misemise;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.misemise.mixin.ExperienceOrbAccessor;

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

        // On this Fabric API version ENTITY_LOAD runs after the entity was
        // added. Capture windows are open only during a specific block break.
        ServerEntityEvents.ENTITY_LOAD.register(AutoCollector::redirectSpawnedDrop);
        ServerTickEvents.END_SERVER_TICK.register(server -> finishOriginalBreakCaptures());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
        registered = true;
    }

    public static void captureOriginalBreak(
            ServerWorld world,
            BlockPos pos,
            ServerPlayerEntity player,
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

    public static void finishOriginalBreak(
            ServerWorld world,
            BlockPos pos,
            ServerPlayerEntity player) {
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

    public static DropCapture beginAdditionalBreak(
            ServerWorld world,
            BlockPos pos,
            ServerPlayerEntity player,
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

    private static void redirectSpawnedDrop(Entity entity, ServerWorld world) {
        DropCapture capture = matchingActiveCapture(entity, world);
        if (capture == null) {
            capture = matchingOriginalCapture(entity, world);
        }
        if (capture != null) {
            capture.redirect(entity);
        }
    }

    private static DropCapture matchingActiveCapture(Entity entity, ServerWorld world) {
        var iterator = ACTIVE_CAPTURES.descendingIterator();
        while (iterator.hasNext()) {
            DropCapture capture = iterator.next();
            if (capture.matches(entity, world)) {
                return capture;
            }
        }
        return null;
    }

    private static DropCapture matchingOriginalCapture(Entity entity, ServerWorld world) {
        for (int index = ORIGINAL_BREAK_CAPTURES.size() - 1; index >= 0; index--) {
            DropCapture capture = ORIGINAL_BREAK_CAPTURES.get(index);
            if (capture.matches(entity, world)) {
                return capture;
            }
        }
        return null;
    }

    private static DropCapture snapshot(
            ServerWorld world,
            BlockPos pos,
            ServerPlayerEntity player,
            boolean collectItems,
            boolean collectExperience) {
        Map<Integer, Integer> itemCounts = new HashMap<>();
        Map<Integer, Integer> experienceOrbCounts = new HashMap<>();
        Box bounds = mergeBounds(pos);

        if (collectItems) {
            for (ItemEntity item : world.getEntitiesByClass(
                    ItemEntity.class, bounds, entity -> true)) {
                itemCounts.put(item.getId(), item.getStack().getCount());
            }
        }
        if (collectExperience) {
            for (ExperienceOrbEntity orb : world.getEntitiesByClass(
                    ExperienceOrbEntity.class, bounds, entity -> true)) {
                experienceOrbCounts.put(orb.getId(), pickingCount(orb));
            }
        }

        return new DropCapture(
                world,
                pos.toImmutable(),
                player,
                collectItems,
                collectExperience,
                itemCounts,
                experienceOrbCounts);
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

    private static Box mergeBounds(BlockPos pos) {
        return new Box(pos).expand(MERGE_SEARCH_MARGIN);
    }

    private static int pickingCount(ExperienceOrbEntity orb) {
        return ((ExperienceOrbAccessor) (Object) orb).omniminer$getPickingCount();
    }

    public static final class DropCapture {
        private final ServerWorld world;
        private final BlockPos pos;
        private final ServerPlayerEntity player;
        private final boolean collectItems;
        private final boolean collectExperience;
        private final Map<Integer, Integer> itemCounts;
        private final Map<Integer, Integer> experienceOrbCounts;
        private boolean finished;

        private DropCapture(
                ServerWorld world,
                BlockPos pos,
                ServerPlayerEntity player,
                boolean collectItems,
                boolean collectExperience,
                Map<Integer, Integer> itemCounts,
                Map<Integer, Integer> experienceOrbCounts) {
            this.world = world;
            this.pos = pos;
            this.player = player;
            this.collectItems = collectItems;
            this.collectExperience = collectExperience;
            this.itemCounts = itemCounts;
            this.experienceOrbCounts = experienceOrbCounts;
        }

        private boolean matches(Entity entity, ServerWorld entityWorld) {
            if (finished
                    || world != entityWorld
                    || player.isRemoved()
                    || player.getEntityWorld() != world
                    || !entity.getBlockPos().equals(pos)) {
                return false;
            }

            return (collectItems && entity instanceof ItemEntity)
                    || (collectExperience && entity instanceof ExperienceOrbEntity);
        }

        private void redirect(Entity entity) {
            if (collectItems && entity instanceof ItemEntity item) {
                item.resetPickupDelay();
                item.setPosition(player.getX(), player.getY(), player.getZ());
            } else if (collectExperience && entity instanceof ExperienceOrbEntity orb) {
                orb.setPosition(player.getX(), player.getY(), player.getZ());
            }
        }

        private void finish() {
            if (finished) {
                return;
            }
            finished = true;

            if (player.isRemoved() || player.getEntityWorld() != world) {
                return;
            }

            Box bounds = mergeBounds(pos);
            if (collectItems) {
                for (ItemEntity item : world.getEntitiesByClass(
                        ItemEntity.class, bounds, entity -> true)) {
                    Integer oldCount = itemCounts.get(item.getId());
                    if (oldCount == null || item.getStack().getCount() > oldCount) {
                        redirect(item);
                    }
                }
            }
            if (collectExperience) {
                for (ExperienceOrbEntity orb : world.getEntitiesByClass(
                        ExperienceOrbEntity.class, bounds, entity -> true)) {
                    Integer oldCount = experienceOrbCounts.get(orb.getId());
                    if (oldCount == null || pickingCount(orb) > oldCount) {
                        redirect(orb);
                    }
                }
            }
        }
    }
}
