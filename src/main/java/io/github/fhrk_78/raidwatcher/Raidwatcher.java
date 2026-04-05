package io.github.fhrk_78.raidwatcher;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.utils.DateUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class Raidwatcher extends JavaPlugin implements Listener {
    //どこでも使えるように一応(初期化おっせーのでnullにアクセスしないよう注意)
    public static Config CONFIG;
    public static Logger LOGGER;

    private Essentials ess;

    //変数をｲｯｷに宣言
    public AtomicInteger loginRate = new AtomicInteger(0);
    public long lockdownEnd = 0;
    public HashMap<UUID, AtomicInteger> chatRate = new HashMap<>();

    @Override
    public void onEnable() {
        LOGGER = getLogger();

        //コンフィグ関連初期化
        saveDefaultConfig();
        //いちおうコンフィグを検査して、扱いやすいように変換(めっちゃミスりそー)
        try {
            CONFIG = Config.asConfig(getConfig());
        } catch (Config.IllegalConfigFormatException e) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //イベント関連初期化
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, () -> loginRate.set(0), 0, CONFIG.highRateLoginBlocker().per());
        getServer().getScheduler().runTaskTimer(this, () -> chatRate.forEach((k, v) -> v.set(0)), 0, CONFIG.spamDetector().per());

        ess = (Essentials) getServer().getPluginManager().getPlugin("essentials");
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("raidwatcher.bypass")) return;

        //同じIPで別アカウントの同時参加
        if (CONFIG.oneIPMultiAccountBlocker().enable() && getServer()
                .getOnlinePlayers()
                .stream()
                .map(Player::getAddress)
                .filter(Objects::nonNull)
                .map(InetSocketAddress::getHostString)
                .anyMatch(ip -> ip.equals(event.getAddress().getHostAddress()))
        ) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    Component.text("同一IPでの同時ログインは禁止されています。ご確認ください。").color(TextColor.color(0xFF0000)));
        }

        //高速複数アカウントログインへの対抗
        if (CONFIG.highRateLoginBlocker().enable()) {
            if (loginRate.get() > CONFIG.highRateLoginBlocker().allowed()) {
                try {
                    lockdownEnd = DateUtil.parseDateDiff(CONFIG.highRateLoginBlocker().cooldown(), true);
                } catch (Exception ignored) {}
            }

            if (lockdownEnd > System.currentTimeMillis())
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                        Component.text("現在、サーバーが大変込み合っておりますので入室制限をかけております。時間を置いて再度お試しください。"));

            loginRate.incrementAndGet();
        }
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        if (event.getPlayer().hasPermission("raidwatcher.bypass")) return;

        final UUID uuid = event.getPlayer().getUniqueId();

        if (CONFIG.spamDetector().enable()) {
            if (chatRate.containsKey(uuid)) chatRate.get(uuid).incrementAndGet();
            else chatRate.put(uuid, new AtomicInteger());
            if (chatRate.get(uuid).get() > CONFIG.spamDetector().allowed()) {
                event.setCancelled(true);
                try {
                    var user = ess.getUser(event.getPlayer());
                    user.setMuted(true);
                    user.setMuteReason("spam");
                    user.setMuteTimeout(DateUtil.parseDateDiff(CONFIG.spamDetector().penaltyMute(), true));
                } catch (Exception ignored) {}
                if (CONFIG.spamDetector().penaltyKick()) Bukkit.getScheduler().runTask(this, () -> {
                    if (getServer().getPlayer(uuid) != null) Objects.requireNonNull(getServer().getPlayer(uuid))
                            .kick(Component.text("スパム行為が検知されました。")
                                    .color(TextColor.color(0xFF0000)));});
            }
        }

        //ChatFilter
        if (CONFIG.chatFilter().enable() && CONFIG.chatFilter().pattern().stream().anyMatch(pattern ->
                pattern.matcher(PlainTextComponentSerializer.plainText().serialize(event.message())).find())) {
            event.setCancelled(true);
            try {
                var user = ess.getUser(event.getPlayer());
                user.setMuted(true);
                user.setMuteReason("blocked word");
                user.setMuteTimeout(DateUtil.parseDateDiff(CONFIG.chatFilter().penaltyMute(), true));
            } catch (Exception ignored) {}
            if (CONFIG.chatFilter().penaltyKick()) Bukkit.getScheduler().runTask(this, () ->
                    event.getPlayer().kick(Component.text("あなたのチャットには禁止ワードが含まれていました。").color(TextColor.color(0xFF0000))));
        }
    }
}
