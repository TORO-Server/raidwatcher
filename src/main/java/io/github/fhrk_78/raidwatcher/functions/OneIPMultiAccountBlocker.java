package io.github.fhrk_78.raidwatcher.functions;

import io.github.fhrk_78.raidwatcher.Raidwatcher;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

public class OneIPMultiAccountBlocker {
    Raidwatcher plugin;

    public OneIPMultiAccountBlocker(Raidwatcher plugin) {
        this.plugin = plugin;
    }

    public boolean isDuplicated(InetAddress address) {
        return plugin.getServer()
                .getOnlinePlayers()
                .stream()
                .map(Player::getAddress)
                .filter(Objects::nonNull)
                .map(InetSocketAddress::getHostString)
                .anyMatch(ip -> ip.equals(address.getHostAddress()));
    }
}
