package dev.circuit.circuittest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CircuitTestCommand implements TabExecutor {

    private static final List<String> SUBS = List.of(
            "help", "info", "tps", "ping", "pos", "heal", "feed", "fly", "god",
            "speed", "kit", "time", "weather", "clearmobs", "welcome", "reload");
    private static final List<String> TIMES = List.of("day", "noon", "night", "midnight");
    private static final List<String> WEATHER = List.of("clear", "rain", "thunder");

    private final CircuitTest plugin;

    public CircuitTestCommand(CircuitTest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("circuittest.use")) {
            err(sender, "You don't have permission.");
            return true;
        }
        if (args.length == 0) {
            help(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> help(sender, label);
            case "info" -> info(sender);
            case "tps" -> tps(sender);
            case "ping" -> ping(sender, args);
            case "pos" -> pos(sender);
            case "heal" -> heal(sender);
            case "feed" -> feed(sender);
            case "fly" -> fly(sender);
            case "god" -> god(sender);
            case "speed" -> speed(sender, args);
            case "kit" -> kit(sender);
            case "time" -> time(sender, args);
            case "weather" -> weather(sender, args);
            case "clearmobs" -> clearMobs(sender);
            case "welcome" -> welcome(sender);
            case "reload" -> {
                plugin.reloadConfig();
                ok(sender, "Config reloaded.");
            }
            default -> err(sender, "Unknown subcommand. Try /" + label + " help");
        }
        return true;
    }

    // ---------------------------------------------------------------- commands

    private void help(CommandSender s, String label) {
        ok(s, "CircuitTest commands (/" + label + " <sub>):");
        String[][] rows = {
                {"info", "Server version, Java, memory, TPS, worlds"},
                {"tps", "TPS (1m/5m/15m) and MSPT"},
                {"ping [player]", "Show latency"},
                {"pos", "Coordinates, chunk, biome, light, facing"},
                {"heal / feed", "Restore health / hunger"},
                {"fly", "Toggle flight"},
                {"god", "Toggle damage immunity"},
                {"speed <1-10|reset>", "Set walk and fly speed"},
                {"kit", "Give a set of testing items"},
                {"time <day|noon|night|midnight>", "Set world time"},
                {"weather <clear|rain|thunder>", "Set weather"},
                {"clearmobs", "Remove hostile mobs in your world"},
                {"welcome", "Replay the join message"},
                {"reload", "Reload config.yml"}
        };
        for (String[] r : rows) {
            s.sendMessage(Component.text(" /" + label + " " + r[0], NamedTextColor.GOLD)
                    .append(Component.text(" - " + r[1], NamedTextColor.GRAY)));
        }
    }

    private void info(CommandSender s) {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1048576;
        long max = rt.maxMemory() / 1048576;

        ok(s, "Server info");
        line(s, "Software", Bukkit.getName() + " (MC " + Bukkit.getMinecraftVersion() + ")");
        line(s, "Build", Bukkit.getVersion());
        line(s, "Java", System.getProperty("java.version"));
        line(s, "Players", Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        line(s, "Memory", used + " MB / " + max + " MB");
        line(s, "Plugins", String.valueOf(Bukkit.getPluginManager().getPlugins().length));
        tps(s);
        for (World w : Bukkit.getWorlds()) {
            line(s, "World " + w.getName(), w.getChunkCount() + " chunks, " + w.getEntityCount() + " entities");
        }
    }

    private void tps(CommandSender s) {
        double[] t = Bukkit.getTPS();
        Component c = Component.text("TPS: ", NamedTextColor.GRAY);
        for (int i = 0; i < t.length; i++) {
            c = c.append(Component.text(String.format(Locale.ROOT, "%.2f", Math.min(t[i], 20.0)), tpsColor(t[i])));
            if (i < t.length - 1) c = c.append(Component.text(" / ", NamedTextColor.DARK_GRAY));
        }
        c = c.append(Component.text("  MSPT: ", NamedTextColor.GRAY))
                .append(Component.text(String.format(Locale.ROOT, "%.2f ms", Bukkit.getAverageTickTime()), NamedTextColor.YELLOW));
        s.sendMessage(c);
    }

    private void ping(CommandSender s, String[] args) {
        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                err(s, "Player not found.");
                return;
            }
        } else {
            target = requirePlayer(s);
            if (target == null) return;
        }
        line(s, target.getName() + " ping", target.getPing() + " ms");
    }

    private void pos(CommandSender s) {
        Player p = requirePlayer(s);
        if (p == null) return;
        Location l = p.getLocation();
        Block b = l.getBlock();
        line(s, "World", p.getWorld().getName());
        line(s, "XYZ", String.format(Locale.ROOT, "%.2f / %.2f / %.2f", l.getX(), l.getY(), l.getZ()));
        line(s, "Chunk", (l.getBlockX() >> 4) + ", " + (l.getBlockZ() >> 4));
        line(s, "Yaw/Pitch", String.format(Locale.ROOT, "%.1f / %.1f", l.getYaw(), l.getPitch()));
        line(s, "Facing", p.getFacing().name());
        line(s, "Biome", b.getBiome().getKey().toString());
        line(s, "Light", "total " + b.getLightLevel() + ", sky " + b.getLightFromSky() + ", block " + b.getLightFromBlocks());
    }

    private void heal(CommandSender s) {
        Player p = requirePlayer(s);
        if (p == null) return;
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max != null ? max.getValue() : 20.0);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        ok(s, "Healed.");
    }

    private void feed(CommandSender s) {
        Player p = requirePlayer(s);
        if (p == null) return;
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setExhaustion(0f);
        ok(s, "Fed.");
    }

    private void fly(CommandSender s) {
        Player p = requirePlayer(s);
        if (p == null) return;
        boolean enable = !p.getAllowFlight();
        p.setAllowFlight(enable);
        if (!enable) p.setFlying(false);
        ok(s, "Flight " + (enable ? "enabled" : "disabled") + ".");
    }

    private void god(CommandSender s) {
        Player p = requirePlayer(s);
        if (p == null) return;
        boolean enable = plugin.godMode().add(p.getUniqueId());
        if (!enable) plugin.godMode().remove(p.getUniqueId());
        ok(s, "God mode " + (enable ? "enabled" : "disabled") + ".");
    }

    private void speed(CommandSender s, String[] args) {
        Player p = requirePlayer(s);
        if (p == null) return;
        if (args.length < 2) {
            err(s, "Usage: /ct speed <1-10|reset>");
            return;
        }
        if (args[1].equalsIgnoreCase("reset")) {
            p.setWalkSpeed(0.2f);
            p.setFlySpeed(0.1f);
            ok(s, "Speed reset to default.");
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            err(s, "Speed must be a number from 1 to 10.");
            return;
        }
        if (level < 1 || level > 10) {
            err(s, "Speed must be from 1 to 10.");
            return;
        }
        float v = level / 10f;
        p.setWalkSpeed(v);
        p.setFlySpeed(v);
        ok(s, "Walk and fly speed set to " + level + " (2 = vanilla walking).");
    }

    private void kit(CommandSender s) {
        Player p = requirePlayer(s);
        if (p == null) return;
        ItemStack[] items = {
                new ItemStack(Material.DIAMOND_SWORD),
                new ItemStack(Material.DIAMOND_PICKAXE),
                new ItemStack(Material.DIAMOND_AXE),
                new ItemStack(Material.DIAMOND_SHOVEL),
                new ItemStack(Material.ELYTRA),
                new ItemStack(Material.FIREWORK_ROCKET, 64),
                new ItemStack(Material.COOKED_BEEF, 64),
                new ItemStack(Material.ENDER_PEARL, 16),
                new ItemStack(Material.WATER_BUCKET),
                new ItemStack(Material.TORCH, 64),
                new ItemStack(Material.OAK_PLANKS, 64),
                new ItemStack(Material.COBBLESTONE, 64),
                new ItemStack(Material.REDSTONE, 64),
                new ItemStack(Material.REPEATER, 16),
                new ItemStack(Material.OBSERVER, 16),
                new ItemStack(Material.PISTON, 16),
                new ItemStack(Material.TNT, 16),
                new ItemStack(Material.OAK_SIGN, 16)
        };
        for (ItemStack leftover : p.getInventory().addItem(items).values()) {
            p.getWorld().dropItem(p.getLocation(), leftover);
        }
        ok(s, "Test kit given.");
    }

    private void time(CommandSender s, String[] args) {
        if (args.length < 2) {
            err(s, "Usage: /ct time <day|noon|night|midnight>");
            return;
        }
        long ticks;
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "day" -> ticks = 1000;
            case "noon" -> ticks = 6000;
            case "night" -> ticks = 13000;
            case "midnight" -> ticks = 18000;
            default -> {
                err(s, "Use: day, noon, night or midnight.");
                return;
            }
        }
        World w = worldOf(s);
        w.setTime(ticks);
        ok(s, "Time set to " + args[1].toLowerCase(Locale.ROOT) + " in " + w.getName() + ".");
    }

    private void weather(CommandSender s, String[] args) {
        if (args.length < 2) {
            err(s, "Usage: /ct weather <clear|rain|thunder>");
            return;
        }
        World w = worldOf(s);
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                w.setStorm(false);
                w.setThundering(false);
            }
            case "rain" -> {
                w.setStorm(true);
                w.setThundering(false);
            }
            case "thunder" -> {
                w.setStorm(true);
                w.setThundering(true);
            }
            default -> {
                err(s, "Use: clear, rain or thunder.");
                return;
            }
        }
        ok(s, "Weather set to " + args[1].toLowerCase(Locale.ROOT) + " in " + w.getName() + ".");
    }

    private void clearMobs(CommandSender s) {
        World w = worldOf(s);
        int count = 0;
        for (Entity e : w.getEntitiesByClass(Monster.class)) {
            e.remove();
            count++;
        }
        ok(s, "Removed " + count + " hostile mob(s) in " + w.getName() + ".");
    }

    private void welcome(CommandSender s) {
        Player p = requirePlayer(s);
        if (p == null) return;
        plugin.sendWelcome(p);
    }

    // ---------------------------------------------------------------- helpers

    private Player requirePlayer(CommandSender s) {
        if (s instanceof Player p) return p;
        err(s, "This subcommand can only be used by a player.");
        return null;
    }

    private World worldOf(CommandSender s) {
        return s instanceof Player p ? p.getWorld() : Bukkit.getWorlds().get(0);
    }

    private static NamedTextColor tpsColor(double tps) {
        if (tps >= 19.0) return NamedTextColor.GREEN;
        if (tps >= 15.0) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }

    private static void ok(CommandSender s, String text) {
        s.sendMessage(Component.text("[CT] ", NamedTextColor.GOLD).append(Component.text(text, NamedTextColor.YELLOW)));
    }

    private static void err(CommandSender s, String text) {
        s.sendMessage(Component.text("[CT] ", NamedTextColor.GOLD).append(Component.text(text, NamedTextColor.RED)));
    }

    private static void line(CommandSender s, String key, String value) {
        s.sendMessage(Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.YELLOW)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("circuittest.use")) return List.of();
        if (args.length == 1) return filter(SUBS, args[0]);
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "time" -> {
                    return filter(TIMES, args[1]);
                }
                case "weather" -> {
                    return filter(WEATHER, args[1]);
                }
                case "speed" -> {
                    return filter(List.of("1", "2", "3", "5", "10", "reset"), args[1]);
                }
                case "ping" -> {
                    List<String> names = new ArrayList<>();
                    for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                    return filter(names, args[1]);
                }
                default -> {
                    return List.of();
                }
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.toLowerCase(Locale.ROOT).startsWith(p)) out.add(o);
        return out;
    }
}
