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
import java.util.Map;
import java.util.regex.Pattern;
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
        // FREMDER ZUSTAND: hat ein Plugin mit niedrigerer Prioritaet die Meldung
        // stummgeschaltet (Minigame-Arena, Vanish), ist das eine Entscheidung, keine
        // Luecke. Vanilla liefert hier immer eine Meldung; null/leer kommt nur von
        // anderen Plugins (gefunden 11.09.2026).
        Component vanilla = event.deathMessage();
        if (vanilla == null || PlainTextComponentSerializer.plainText().serialize(vanilla).isEmpty()) return;
        Player player = event.getPlayer();
        String pool = poolFor(player);
        List<String> options = pool(pool);
        if (options.isEmpty()) options = pool("DEFAULT");
        if (!options.isEmpty()) {
            String raw = options.get(ThreadLocalRandom.current().nextInt(options.size()));
            event.deathMessage(fill(raw, player));
        }
        if (getConfig().getBoolean("tell-coordinates", true)) {
            String coords = getConfig().getString("coordinates-message", "");
            if (!coords.isEmpty()) {
                player.sendMessage(fill(coords, player));
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
        return getConfig().contains("messages." + cause, true) ? cause : "DEFAULT";
    }

    /**
     * Liest einen Pool NUR aus der Datei des Betreibers. getConfig() legt die
     * config.yml aus dem Jar als Standard darunter -- ein geloeschter Pool waere
     * damit still durch den mitgelieferten ersetzt worden, und "messages: {}"
     * haette die Vanilla-Meldungen nicht zurueckgebracht (gefunden 11.09.2026).
     * Ein Pool darf auch ein einzelner String statt einer Liste sein.
     */
    private List<String> pool(String name) {
        String path = "messages." + name;
        if (!getConfig().contains(path, true)) return List.of();
        if (getConfig().isList(path)) return getConfig().getStringList(path);
        String single = getConfig().getString(path);
        return single == null || single.isEmpty() ? List.of() : List.of(single);
    }

    /**
     * Setzt die Platzhalter ein. {weapon} ist FREMDER Text: der Anzeigename eines
     * Items, den jeder Spieler am Amboss frei waehlt. Frueher wurde er als
     * Zeichenkette in die Vorlage eingesetzt und erst danach geparst -- ein Schwert
     * namens "&kBoss" haette damit die ganze Todesmeldung des Servers umformatiert.
     * Deshalb wird jetzt die Vorlage geparst (die stammt vom Serverbetreiber) und die
     * Werte werden als fertige Komponenten eingesetzt.
     */
    private Component fill(String raw, Player player) {
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
        Map<String, String> values = Map.of(
                "player", player.getName(), "killer", killer, "weapon", weapon,
                "x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ()), "world", loc.getWorld().getName());
        // EIN Durchlauf ueber die Vorlage: sieben Durchlaeufe nacheinander haetten den
        // schon eingesetzten Waffennamen erneut durchsucht -- ein Schwert namens
        // "{x} {y} {z}" haette die Koordinaten des Opfers in den Broadcast gesetzt.
        return parse(raw).replaceText(r -> r.match(PLACEHOLDER)
                .replacement((match, b) -> Component.text(values.get(match.group(1)))));
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(player|killer|weapon|x|y|z|world)}");

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
