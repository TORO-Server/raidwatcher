package io.github.fhrk_78.raidwatcher.functions;

import io.github.fhrk_78.raidwatcher.Config;
import io.github.fhrk_78.raidwatcher.Raidwatcher;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SpamDetector {
    Config config;
    Raidwatcher plugin;

    HashMap<UUID, AtomicInteger> chatRate = new HashMap<>();

    public SpamDetector(Config config, Raidwatcher plugin) {
        this.config = config;
        this.plugin = plugin;

        plugin
                .getServer()
                .getScheduler()
                .runTaskTimer(plugin, () -> chatRate.forEach((k, v) -> v.set(0)), 0, config.spamDetectorConfig().per());
    }

    public boolean isSpam(Player player) {
        UUID uuid = player.getUniqueId();

        if (chatRate.containsKey(uuid)) chatRate.get(uuid).getAndIncrement();
        else  chatRate.put(uuid, new AtomicInteger(1));

        return chatRate.get(uuid).get() > config.spamDetectorConfig().allowed();
    }
}
