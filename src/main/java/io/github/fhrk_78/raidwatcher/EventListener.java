package io.github.fhrk_78.raidwatcher;

import io.github.fhrk_78.raidwatcher.adapter.EssentialsAdapter;
import io.github.fhrk_78.raidwatcher.functions.ChatFilter;
import io.github.fhrk_78.raidwatcher.functions.HighRateLoginBlocker;
import io.github.fhrk_78.raidwatcher.functions.OneIPMultiAccountBlocker;
import io.github.fhrk_78.raidwatcher.functions.SpamDetector;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class EventListener implements Listener {
    Config config;
    Raidwatcher plugin;
    EssentialsAdapter essentialsAdapter;

    OneIPMultiAccountBlocker oneIPMultiAccountBlocker;
    HighRateLoginBlocker highRateLoginBlocker;
    SpamDetector spamDetector;
    ChatFilter chatFilter;

    public EventListener(Config config, Raidwatcher plugin, EssentialsAdapter essentialsAdapter) {
        this.config = config;
        this.plugin = plugin;
        this.essentialsAdapter = essentialsAdapter;

        oneIPMultiAccountBlocker = new OneIPMultiAccountBlocker(plugin);
        highRateLoginBlocker = new HighRateLoginBlocker(config, plugin, essentialsAdapter);
        spamDetector = new SpamDetector(config, plugin);
        chatFilter = new ChatFilter(config);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("raidwatcher.bypass")) return;

        if (config.oneIPMultiAccountBlockerConfig().enable() && oneIPMultiAccountBlocker.isDuplicated(event.getAddress()))
            event.disallow(
                    PlayerLoginEvent.Result.KICK_OTHER,
                    Component
                            .text("同一IPでの同時ログインは禁止されています。ご確認ください。")
                            .color(TextColor.color(0xFF0000))
            );

        if (config.highRateLoginBlockerConfig().enable() && !highRateLoginBlocker.canJoin())
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    Component
                            .text("現在、サーバーが大変込み合っておりますので入室制限をかけております。時間を置いて再度お試しください。"));
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        if (event.getPlayer().hasPermission("raidwatcher.bypass")) return;

        if (config.spamDetectorConfig().enable() && spamDetector.isSpam(event.getPlayer())) {
            essentialsAdapter.setMuted(event.getPlayer(), "spamming", config.spamDetectorConfig().penaltyMute());
            plugin.getServer().getScheduler().runTask(plugin, () -> event.getPlayer().kick(Component
                    .text("スパム行為が検知されました。")
                    .color(TextColor.color(0xFF0000))));
        }

        if (config.chatFilterConfig().enable() && chatFilter.isMessageMatched(event.message())) {
            essentialsAdapter.setMuted(event.getPlayer(), "ng word", config.chatFilterConfig().penaltyMute());
            plugin.getServer().getScheduler().runTask(plugin, () -> event.getPlayer().kick(Component
                    .text("あなたのチャットには禁止ワードが含まれていました。")
                    .color(TextColor.color(0xFF0000))));
        }
    }
}
