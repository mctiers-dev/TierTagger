package net.uku3lig.tiertagger;

import net.uku3lig.tiertagger.lua.LuaApi;
import net.uku3lig.tiertagger.model.GameMode;
import net.uku3lig.tiertagger.model.PlayerInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class TierCache {
    private static final List<GameMode> GAMEMODES = new ArrayList<>();
    private static final Map<UUID, Optional<Map<String, PlayerInfo.Ranking>>> TIERS = new ConcurrentHashMap<>();

    public static void init() {
        // try {
            GAMEMODES.clear();
            GAMEMODES.addAll(LuaApi.getGameModes());
            TierTagger.getLogger().info("Found {} gamemodes: {}", GAMEMODES.size(), GAMEMODES.stream().map(GameMode::id).toList());
        /* } catch (ExecutionException e) {
            TierTagger.getLogger().error("Failed to load gamemodes!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } */
    }

    public static List<GameMode> getGamemodes() {
        if (GAMEMODES.isEmpty()) {
            return Collections.singletonList(GameMode.NONE);
        } else {
            return GAMEMODES;
        }
    }

    public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankings(UUID uuid) {
        return TIERS.computeIfAbsent(uuid, _ -> {
            if (uuid.version() == 4) {
                CompletableFuture.runAsync(() -> TIERS.put(uuid, Optional.ofNullable(LuaApi.fetchPlayer(uuid).rankings())));
                // PlayerInfo.getRankings(TierTagger.getClient(), uuid).thenAccept(info -> TIERS.put(uuid, Optional.ofNullable(info)));
            }

            return Optional.empty();
        });
    }

    public static CompletableFuture<PlayerInfo> searchPlayer(String query) {
        return PlayerInfo.search(TierTagger.getClient(), query).thenApply(p -> {
            UUID uuid = parseUUID(p.uuid());
            TIERS.put(uuid, Optional.of(p.rankings()));
            return p;
        });
    }

    public static void clearCache() {
        TIERS.clear();
    }

    public static GameMode findNextMode(GameMode current) {
        if (GAMEMODES.isEmpty()) {
            return GameMode.NONE;
        } else {
            return GAMEMODES.get((GAMEMODES.indexOf(current) + 1) % GAMEMODES.size());
        }
    }

    public static Optional<GameMode> findMode(String id) {
        return GAMEMODES.stream().filter(m -> m.id().equalsIgnoreCase(id)).findFirst();
    }

    public static GameMode findModeOrUgly(String id) {
        return findMode(id).orElseGet(() -> new GameMode(id, id));
    }

    private static UUID parseUUID(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (Exception e) {
            long mostSignificant = Long.parseUnsignedLong(uuid.substring(0, 16), 16);
            long leastSignificant = Long.parseUnsignedLong(uuid.substring(16), 16);
            return new UUID(mostSignificant, leastSignificant);
        }
    }

    private TierCache() {
    }
}
