package io.github.fhrk_78.raidwatcher;

import io.github.fhrk_78.raidwatcher.adapter.EssentialsAdapter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Raidwatcher extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        //コンフィグ関連初期化
        saveDefaultConfig();
        final Config config;
        try {
            config = Config.asConfig(getConfig());
        } catch (Config.IllegalConfigFormatException e) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //イベント関連初期化
        getServer().getPluginManager().registerEvents(
                new EventListener(config, this, new EssentialsAdapter(getServer().getPluginManager())), this);
    }
}
