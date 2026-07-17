package fr.blue.abyssalplanet.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AbyssalPoacherGroupData extends SavedData {
    private static final String DATA_NAME = "abyssalplanet_poacher_groups";
    private static final long PLAYER_ALERT_COOLDOWN_TICKS = 20L * 30L;

    private final Map<UUID, GroupState> groups = new HashMap<>();

    public static AbyssalPoacherGroupData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                AbyssalPoacherGroupData::load,
                AbyssalPoacherGroupData::new,
                DATA_NAME
        );
    }

    private static AbyssalPoacherGroupData load(CompoundTag tag) {
        AbyssalPoacherGroupData data = new AbyssalPoacherGroupData();
        ListTag groupTags = tag.getList("Groups", Tag.TAG_COMPOUND);
        for (int index = 0; index < groupTags.size(); index++) {
            CompoundTag groupTag = groupTags.getCompound(index);
            if (!groupTag.hasUUID("Group") || !groupTag.hasUUID("Leader")) {
                continue;
            }

            UUID groupId = groupTag.getUUID("Group");
            GroupState state = new GroupState(
                    groupTag.getUUID("Leader"),
                    BlockPos.of(groupTag.getLong("Home")),
                    groupTag.contains("LastCombat") ? BlockPos.of(groupTag.getLong("LastCombat")) : null,
                    groupTag.getInt("Fallen"),
                    groupTag.getBoolean("LeaderAlive"),
                    groupTag.contains("LastPlayerAlert")
                            ? groupTag.getLong("LastPlayerAlert")
                            : Long.MIN_VALUE
            );
            data.groups.put(groupId, state);
        }
        return data;
    }

    public void createGroup(UUID groupId, UUID leaderId, BlockPos home) {
        this.groups.put(groupId, new GroupState(
                leaderId,
                home.immutable(),
                null,
                0,
                true,
                Long.MIN_VALUE
        ));
        setDirty();
    }

    public boolean tryTriggerPlayerAlert(UUID groupId, long gameTime) {
        GroupState state = this.groups.get(groupId);
        if (state == null) {
            return false;
        }
        if (state.lastPlayerAlertGameTime != Long.MIN_VALUE
                && gameTime - state.lastPlayerAlertGameTime < PLAYER_ALERT_COOLDOWN_TICKS) {
            return false;
        }

        state.lastPlayerAlertGameTime = gameTime;
        setDirty();
        return true;
    }

    public void recordCompanionDeath(UUID groupId, BlockPos combatPos) {
        GroupState state = this.groups.get(groupId);
        if (state == null || !state.leaderAlive) {
            return;
        }

        state.fallenCompanions = Math.min(4, state.fallenCompanions + 1);
        state.lastCombat = combatPos.immutable();
        setDirty();
    }

    public void recordLeaderDeath(UUID groupId) {
        GroupState state = this.groups.get(groupId);
        if (state == null) {
            return;
        }

        state.leaderAlive = false;
        setDirty();
    }

    public void recordCombat(UUID groupId, BlockPos combatPos) {
        GroupState state = this.groups.get(groupId);
        if (state == null || !state.leaderAlive) {
            return;
        }

        state.lastCombat = combatPos.immutable();
        setDirty();
    }

    public int getFallenCompanions(UUID groupId) {
        GroupState state = this.groups.get(groupId);
        return state == null || !state.leaderAlive ? 0 : state.fallenCompanions;
    }

    public BlockPos getWandererAnchor(UUID groupId, BlockPos fallback) {
        GroupState state = this.groups.get(groupId);
        if (state == null) {
            return fallback.immutable();
        }
        return state.lastCombat == null ? state.home : state.lastCombat;
    }

    public void finishGroup(UUID groupId) {
        if (this.groups.remove(groupId) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag groupTags = new ListTag();
        for (Map.Entry<UUID, GroupState> entry : this.groups.entrySet()) {
            GroupState state = entry.getValue();
            CompoundTag groupTag = new CompoundTag();
            groupTag.putUUID("Group", entry.getKey());
            groupTag.putUUID("Leader", state.leaderId);
            groupTag.putLong("Home", state.home.asLong());
            if (state.lastCombat != null) {
                groupTag.putLong("LastCombat", state.lastCombat.asLong());
            }
            groupTag.putInt("Fallen", state.fallenCompanions);
            groupTag.putBoolean("LeaderAlive", state.leaderAlive);
            if (state.lastPlayerAlertGameTime != Long.MIN_VALUE) {
                groupTag.putLong("LastPlayerAlert", state.lastPlayerAlertGameTime);
            }
            groupTags.add(groupTag);
        }
        tag.put("Groups", groupTags);
        return tag;
    }

    private static final class GroupState {
        private final UUID leaderId;
        private final BlockPos home;
        private BlockPos lastCombat;
        private int fallenCompanions;
        private boolean leaderAlive;
        private long lastPlayerAlertGameTime;

        private GroupState(
                UUID leaderId,
                BlockPos home,
                BlockPos lastCombat,
                int fallenCompanions,
                boolean leaderAlive,
                long lastPlayerAlertGameTime
        ) {
            this.leaderId = leaderId;
            this.home = home;
            this.lastCombat = lastCombat;
            this.fallenCompanions = fallenCompanions;
            this.leaderAlive = leaderAlive;
            this.lastPlayerAlertGameTime = lastPlayerAlertGameTime;
        }
    }
}
