package io.github.fhrk_78.raidwatcher.functions;

import io.github.fhrk_78.raidwatcher.Config;
import io.github.fhrk_78.raidwatcher.Raidwatcher;
import io.github.fhrk_78.raidwatcher.adapter.EssentialsAdapter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class HighRateLoginBlocker {
    Config config;
    Raidwatcher plugin;
    Logger logger;
    EssentialsAdapter essentials;

    AtomicInteger loginRate = new AtomicInteger(0);
    long lockdownExpired = 0;

    public HighRateLoginBlocker(Config config, Raidwatcher plugin, EssentialsAdapter essentialsAdapter) {
        this.config = config;
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        essentials = essentialsAdapter;

        plugin
                .getServer()
                .getScheduler()
                .runTaskTimer(plugin, () -> loginRate.set(0), 0, config.highRateLoginBlockerConfig().per());
    }

    public boolean canJoin() {
        if (lockdownExpired > System.currentTimeMillis()) {
            return false;
        }

        if (loginRate.getAndIncrement() > config.highRateLoginBlockerConfig().allowed()) {
            try {
                lockdownExpired = essentials.parseFutureDateDiff(config.highRateLoginBlockerConfig().cooldown());
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }

        return true;
    }
}
