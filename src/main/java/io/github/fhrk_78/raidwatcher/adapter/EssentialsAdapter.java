package io.github.fhrk_78.raidwatcher.adapter;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.DateUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

public class EssentialsAdapter {
    public final boolean isEssentialsEnabled;
    Essentials essentials;

    public EssentialsAdapter(PluginManager pluginManager) {
        if (pluginManager.isPluginEnabled("Essentials")
                && pluginManager.getPlugin("Essentials") instanceof Essentials ePlugin) {
            essentials = ePlugin;
            isEssentialsEnabled = true;
        }
        else isEssentialsEnabled = false;
    }

    public long parseFutureDateDiff(String time) throws Exception {
        if (isEssentialsEnabled) return DateUtil.parseDateDiff(time, true);
        //TODO: DateUtil非依存関数の作成
        else return 0;
    }

    public void setMuted(Player player, String reason, String timeout) {
        if (isEssentialsEnabled) {
            User user = essentials.getUser(player);
            user.setMuted(true);
            user.setMuteReason(reason);
            try {
                user.setMuteTimeout(parseFutureDateDiff(timeout));
            } catch (Exception ignored) {}
        }
        //TODO:
    }
}
