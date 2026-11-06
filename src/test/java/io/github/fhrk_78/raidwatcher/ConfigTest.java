package io.github.fhrk_78.raidwatcher;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    private static Config.HighRateLoginBlocker load(String yaml) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        return Config.HighRateLoginBlocker.from(file);
    }

    private static final String OLD_CONFIG = """
            HighRateLoginBlocker:
              enable: true
              allowed: 3
              per: 200
              cooldown: 10s
            """;

    @Test
    void configWithoutTheNewKeysGetsSafeLimits() throws Exception {
        Config.HighRateLoginBlocker login = load(OLD_CONFIG);
        assertTrue(login.enable());
        assertEquals(3, login.allowed());
        assertEquals(200, login.per());
        assertEquals("10s", login.cooldown());
        assertEquals(10_000L, login.windowMillis());
        assertEquals(10, login.ipAllowed());
        assertEquals(40, login.globalAllowed());
    }

    @Test
    void valuesOfTheJarConfigDoNotReplaceTheCalculatedLimits() throws Exception {
        // JavaPlugin#getConfig は jar の config.yml を既定の値として持つ
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(OLD_CONFIG.replace("per: 200", "per: 1200"));
        try (Reader jarConfig = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            file.setDefaults(YamlConfiguration.loadConfiguration(jarConfig));
        }
        Config.HighRateLoginBlocker login = Config.HighRateLoginBlocker.from(file);
        assertEquals(10, login.ipAllowed());
        assertEquals(210, login.globalAllowed());
    }

    @Test
    void theJarConfigHasTheSameLimitsAsTheCodeDefaults() throws Exception {
        try (Reader jarConfig = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            Config.HighRateLoginBlocker fromJar = Config.HighRateLoginBlocker.from(YamlConfiguration.loadConfiguration(jarConfig));
            Config.HighRateLoginBlocker fromOldConfig = load(OLD_CONFIG);
            assertEquals(fromOldConfig, fromJar);
        }
    }

    @Test
    void defaultGlobalLimitIsAboveTheProxyReturnRateForAnyWindow() throws Exception {
        for (int per : new int[] {1, 20, 60, 61, 200, 1200, 12000}) {
            Config.HighRateLoginBlocker login = load(OLD_CONFIG.replace("per: 200", "per: " + per));
            assertTrue(login.globalAllowed() > Config.HighRateLoginBlocker.proxyReturns(per), "per=" + per);
            // 1つの IP だけで全体の上限に届かないこと
            assertTrue(login.globalAllowed() > login.ipAllowed(), "per=" + per);
        }
    }

    @Test
    void defaultsFollowALargerAllowed() throws Exception {
        Config.HighRateLoginBlocker login = load(OLD_CONFIG.replace("allowed: 3", "allowed: 100"));
        assertEquals(100, login.allowed());
        assertEquals(100, login.ipAllowed());
        assertEquals(101, login.globalAllowed());
    }

    @Test
    void explicitValuesAreUsed() throws Exception {
        Config.HighRateLoginBlocker login = load(OLD_CONFIG + "  ipAllowed: 5\n  globalAllowed: 25\n");
        assertEquals(5, login.ipAllowed());
        assertEquals(25, login.globalAllowed());
    }

    @Test
    void brokenValuesFallBackToSafeOnes() throws Exception {
        Config.HighRateLoginBlocker login = load(OLD_CONFIG.replace("allowed: 3", "allowed: 0").replace("per: 200", "per: 0"));
        assertEquals(1, login.allowed());
        assertEquals(200, login.per());
    }

    @Test
    void emptyConfigStillGivesUsableLimits() throws Exception {
        Config.HighRateLoginBlocker login = load("");
        assertEquals(3, login.allowed());
        assertEquals(200, login.per());
        assertEquals(10, login.ipAllowed());
        assertEquals(40, login.globalAllowed());
    }
}
