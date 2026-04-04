package io.github.fhrk_78.raidwatcher;

import com.earth2me.essentials.Essentials;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public final class Raidwatcher extends JavaPlugin implements Listener {
    //どこでも使えるように一応(初期化おっせーのでnullにアクセスしないよう注意)
    public static Config CONFIG;
    public static Logger LOGGER;

    private Essentials ess;

    //変数をｲｯｷに宣言
    public long loginRateTickCounter = 0;
    public int loginRate = 0;
    public Instant lockdownEnd = Instant.now();
    public long chatRateTickCounter = 0;
    public HashMap<UUID, Integer> chatRate = new HashMap<>();

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
        getServer().getScheduler().runTaskTimer(this, this::onTick, 0, 1);

        ess = (Essentials) getServer().getPluginManager().getPlugin("essentials");
    }

    public void onTick() {
        //ここらへんはレート系のカウンターを下げてく用
        loginRateTickCounter++;
        if (loginRateTickCounter >= CONFIG.highRateLoginBlocker().per() * 20) {
            loginRateTickCounter = 0;
            loginRate = 0;
        }

        chatRateTickCounter++;
        if (chatRateTickCounter >= CONFIG.spamDetector().per() * 20) {
            chatRateTickCounter = 0;
            chatRate.replaceAll((u, v) -> 0);
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("raidwatcher.bypass")) return;

        loginRate++;

        if (loginRate > CONFIG.highRateLoginBlocker().allowed())
            lockdownEnd = Instant.now().plusSeconds(CONFIG.highRateLoginBlocker().cooldown());

        if (lockdownEnd.toEpochMilli() > Instant.now().toEpochMilli())
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("現在、サーバーが大変込み合っておりますので入室制限をかけております。時間を置いて再度お試しください。"));
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        if (event.getPlayer().hasPermission("raidwatcher.bypass")) return;

        final UUID uuid = event.getPlayer().getUniqueId();

        chatRate.put(uuid, chatRate.getOrDefault(uuid, 0) + 1);
        if (chatRate.get(uuid) > CONFIG.spamDetector().allowed()) {
            var user = ess.getUser(event.getPlayer());
            event.setCancelled(true);
            user.setMuted(true);
            user.setMuteReason("spam");
            user.setMuteTimeout(Instant.now().plusSeconds(CONFIG.spamDetector().penaltyMute()).toEpochMilli());
            if (CONFIG.spamDetector().penaltyKick()) Bukkit.getScheduler().runTask(this, () -> {
                if (getServer().getPlayer(uuid) != null) Objects.requireNonNull(getServer().getPlayer(uuid))
                        .kick(Component.text("スパム行為が検知されました。")
                                .color(TextColor.color(0xFF0000)));});
        }

        //ChatFilter
        if (CONFIG.chatFilter().pattern().stream().anyMatch(word -> PlainTextComponentSerializer
                .plainText()
                .serialize(event.message())
                .matches(word))) {
            event.setCancelled(true);
            var user = ess.getUser(event.getPlayer());
            user.setMuted(true);
            user.setMuteReason("blocked word");
            user.setMuteTimeout(Instant.now().plusSeconds(CONFIG.chatFilter().penaltyMute()).toEpochMilli());
            if (CONFIG.chatFilter().penaltyKick()) Bukkit.getScheduler().runTask(this, () ->
                    event.getPlayer().kick(Component.text("あなたのチャットには禁止ワードが含まれていました。").color(TextColor.color(0xFF0000))));
        }
    }
}
