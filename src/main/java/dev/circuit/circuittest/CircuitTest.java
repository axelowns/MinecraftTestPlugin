package dev.circuit.circuittest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CircuitTest extends JavaPlugin {

    private final Set<UUID> godMode = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        PluginCommand cmd = getCommand("circuittest");
        if (cmd != null) {
            CircuitTestCommand handler = new CircuitTestCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
        getLogger().info("CircuitTest enabled.");
    }

    @Override
    public void onDisable() {
        godMode.clear();
    }

    public Set<UUID> godMode() {
        return godMode;
    }

    /** Shows the yellow welcome text to a player (chat / title / action bar per config). */
    public void sendWelcome(Player player) {
        String text = getConfig().getString("welcome.message", "Circuit Test Server");
        String sub = getConfig().getString("welcome.subtitle", "");
        Component message = Component.text(text, NamedTextColor.YELLOW);

        if (getConfig().getBoolean("welcome.chat", true)) {
            player.sendMessage(message);
        }
        if (getConfig().getBoolean("welcome.title", true)) {
            Component subtitle = sub.isEmpty() ? Component.empty() : Component.text(sub, NamedTextColor.GRAY);
            player.showTitle(Title.title(message, subtitle, Title.Times.times(
                    Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(1000))));
        }
        if (getConfig().getBoolean("welcome.actionbar", false)) {
            player.sendActionBar(message);
        }
    }
}
