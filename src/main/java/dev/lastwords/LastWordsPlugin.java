package dev.lastwords;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class LastWordsPlugin extends JavaPlugin implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("LastWords " + getDescription().getVersion() + " enabled.");
    }

    private Component parse(String raw) {
        // Legacy codes win over the angle-bracket heuristic: '&7<&bVIP&7>' is
        // decoration, not a MiniMessage tag (portfolio-wide fix, 04.09.2026).
        if (!hasLegacyCode(raw) && raw.indexOf('<') >= 0 && raw.indexOf('>') > raw.indexOf('<')) {
            try {
                return MINI.deserialize(raw);
            } catch (Exception ignored) {
            }
        }
        return LEGACY.deserialize(raw);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        String pool = poolFor(player);
        List<String> options = getConfig().getStringList("messages." + pool);
        if (options.isEmpty()) options = getConfig().getStringList("messages.DEFAULT");
        if (!options.isEmpty()) {
            String raw = options.get(ThreadLocalRandom.current().nextInt(options.size()));
            event.deathMessage(parse(fill(raw, player)));
        }
        if (getConfig().getBoolean("tell-coordinates", true)) {
            String coords = getConfig().getString("coordinates-message", "");
            if (!coords.isEmpty()) {
                player.sendMessage(parse(fill(coords, player)));
            }
        }
    }

    private String poolFor(Player player) {
        if (player.getKiller() != null) return "PLAYER";
        EntityDamageEvent last = player.getLastDamageCause();
        if (last == null) return "DEFAULT";
        String cause = switch (last.getCause()) {
            case FALL -> "FALL";
            case LAVA -> "LAVA";
            case FIRE, FIRE_TICK -> "FIRE";
            case DROWNING -> "DROWNING";
            case VOID -> "VOID";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "EXPLOSION";
            case PROJECTILE -> "PROJECTILE";
            case STARVATION -> "STARVATION";
            case FREEZE -> "FREEZE";
            case MAGIC -> "MAGIC";
            case WITHER -> "WITHER";
            case POISON -> "POISON";
            case SUFFOCATION -> "SUFFOCATION";
            case CONTACT -> "CONTACT";
            default -> "DEFAULT";
        };
        return getConfig().contains("messages." + cause) ? cause : "DEFAULT";
    }

    private String fill(String raw, Player player) {
        Location loc = player.getLocation();
        String killer = player.getKiller() != null ? player.getKiller().getName() : "?";
        String weapon = "fists";
        if (player.getKiller() != null) {
            ItemStack item = player.getKiller().getInventory().getItemInMainHand();
            if (item.getType() != Material.AIR) {
                weapon = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                        ? PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
                        : prettify(item.getType().name());
            }
        }
        return raw.replace("{player}", player.getName())
                .replace("{killer}", killer)
                .replace("{weapon}", weapon)
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ()))
                .replace("{world}", loc.getWorld().getName());
    }

    private String prettify(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command cmd,
                             String label, String[] args) {
        reloadConfig();
        sender.sendMessage(Component.text("[LastWords] Reloaded."));
        return true;
    }

    /** Every character that may follow '&' / section sign in a legacy code. */
    private static final String LEGACY_CODES = "0123456789abcdefklmnorxABCDEFKLMNORX";

    /** True if the text carries at least one real legacy colour/format code. */
    private static boolean hasLegacyCode(String s) {
        for (int i = 0; i + 1 < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '\u00A7') && LEGACY_CODES.indexOf(s.charAt(i + 1)) >= 0) {
                return true;
            }
        }
        return false;
    }
}
