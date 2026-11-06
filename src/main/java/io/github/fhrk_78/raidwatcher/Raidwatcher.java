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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class Raidwatcher extends JavaPlugin implements Listener {
    //どこでも使えるように一応(初期化おっせーのでnullにアクセスしないよう注意)
    public static Config CONFIG;
    public static Logger LOGGER;

    private static final long DEFAULT_COOLDOWN_MILLIS = 10_000L;

    private Essentials ess;

    //変数をｲｯｷに宣言
    private LoginRateLimiter loginLimiter;
    public ConcurrentHashMap<UUID, AtomicInteger> chatRate = new ConcurrentHashMap<>();

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

        final Config.HighRateLoginBlocker login = CONFIG.highRateLoginBlocker();
        if (login.enable() && login.globalAllowed() <= Config.HighRateLoginBlocker.proxyReturns(login.per())) {
            LOGGER.warning("HighRateLoginBlocker.globalAllowed が低いため、TORO-Proxy が再起動のあとにプレイヤーを戻すときに入室制限がかかることがあります");
        }
        loginLimiter = new LoginRateLimiter(login.allowed(), login.ipAllowed(), login.globalAllowed(),
                login.windowMillis(), cooldownMillis(login.cooldown()), monotonicMillis());

        //イベント関連初期化
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this,
                () -> chatRate.forEach((k, v) -> v.set(0)), 0, CONFIG.spamDetector().per());

        ess = (Essentials) getServer().getPluginManager().getPlugin("essentials");
    }

    /** 入室を止める長さ。読めないときは既定の長さにする (止められないよりよい) */
    private static long cooldownMillis(String cooldown) {
        try {
            return Math.max(0L, DateUtil.parseDateDiff(cooldown, true) - System.currentTimeMillis());
        } catch (Exception e) {
            LOGGER.warning("HighRateLoginBlocker.cooldown を読めないので " + DEFAULT_COOLDOWN_MILLIS / 1000 + " 秒にします: " + cooldown);
            return DEFAULT_COOLDOWN_MILLIS;
        }
    }

    //時計を合わせても数え方が狂わないように、経過時間だけを測る時計を使う
    private static long monotonicMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        //先に断られたログイン(BANなど)は入れないので数えない
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) return;
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
                    Component.text("同一IPでの同時ログインは禁止されています。ご確認ください。")
                            .color(TextColor.color(0xFF0000)));
            return;
        }

        //高速複数アカウントログインへの対抗
        if (CONFIG.highRateLoginBlocker().enable()) {
            final String ip = event.getAddress().getHostAddress();
            switch (loginLimiter.check(ip, event.getPlayer().getUniqueId(), monotonicMillis())) {
                case ALLOWED -> {}
                case SOURCE_LIMITED -> event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                        Component.text("短い時間にログインを繰り返しています。少し待ってから再度お試しください。"));
                case LOCKDOWN_STARTED -> {
                    LOGGER.warning("短い時間のログインが多すぎるので、入室を制限します");
                    disallowLockdown(event);
                }
                case LOCKED_DOWN -> disallowLockdown(event);
            }
        }
    }

    private static void disallowLockdown(PlayerLoginEvent event) {
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                Component.text("現在、サーバーが大変込み合っておりますので入室制限をかけております。時間を置いて再度お試しください。"));
    }

    //LunaChat はこのイベントで発言をチャンネルへ送って取り消すので、チャンネルの発言を止めるにはそれより先に調べる
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPermission("raidwatcher.bypass")) return;

        if (isSpam(player) || hasBlockedWord(player, event.getMessage())) event.setCancelled(true);
    }

    //通常のチャットは Japanize などで書き換えられたあとの発言もここで調べられる。連投はもう数えたので数えない
    @EventHandler(ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPermission("raidwatcher.bypass")) return;

        if (hasBlockedWord(player, PlainTextComponentSerializer.plainText().serialize(event.message()))) {
            event.setCancelled(true);
        }
    }

    //LunaChat の /tell などはチャットのイベントを通らない
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        final ChatCommands.Message message = ChatCommands.parse(event.getMessage());
        if (message == null) return;

        final Player player = event.getPlayer();
        if (player.hasPermission("raidwatcher.bypass")) return;

        if ((message.rateLimited() && isSpam(player)) || hasBlockedWord(player, message.text())) {
            event.setCancelled(true);
        }
    }

    //連投の判定。発言1つにつき1回だけ呼ぶ
    private boolean isSpam(Player player) {
        final Config.SpamDetector spam = CONFIG.spamDetector();
        if (!spam.enable()) return false;

        final UUID uuid = player.getUniqueId();
        if (chatRate.computeIfAbsent(uuid, k -> new AtomicInteger()).incrementAndGet() <= spam.allowed()) return false;

        mute(player, "spam", spam.penaltyMute());
        if (spam.penaltyKick()) Bukkit.getScheduler().runTask(this, () -> {
            if (getServer().getPlayer(uuid) != null) Objects.requireNonNull(getServer().getPlayer(uuid))
                    .kick(Component.text("スパム行為が検知されました。")
                            .color(TextColor.color(0xFF0000)));});
        return true;
    }

    //ChatFilter
    private boolean hasBlockedWord(Player player, String message) {
        final Config.ChatFilter filter = CONFIG.chatFilter();
        if (!filter.enable() || filter.pattern().stream().noneMatch(pattern -> pattern.matcher(message).find())) {
            return false;
        }

        mute(player, "blocked word", filter.penaltyMute());
        if (filter.penaltyKick()) Bukkit.getScheduler().runTask(this, () ->
                player.kick(Component.text("あなたのチャットには禁止ワードが含まれていました。")
                        .color(TextColor.color(0xFF0000))));
        return true;
    }

    private void mute(Player player, String reason, String time) {
        try {
            var user = ess.getUser(player);
            user.setMuted(true);
            user.setMuteReason(reason);
            user.setMuteTimeout(DateUtil.parseDateDiff(time, true));
        } catch (Exception ignored) {}
    }
}
