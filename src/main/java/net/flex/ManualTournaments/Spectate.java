package net.flex.ManualTournaments;

import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class Spectate implements TabCompleter, CommandExecutor {
    private static final FileConfiguration config = Main.getPlugin().getConfig();
    private final FileConfiguration ArenasConfig = Main.getPlugin().getArenaConfig();
    static final List<Player> spectators = new ArrayList<>();
    GameMode gameMode = Bukkit.getServer().getDefaultGameMode();

    public Spectate() {
    }

    @SneakyThrows
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String s, @NotNull final String[] args) {
        config.load(Main.getPlugin().customConfigFile);
        ArenasConfig.load(Main.getPlugin().ArenaConfigFile);
        if (!(sender instanceof Player)) sender.sendMessage(Main.conf("sender-not-a-player"));
        else {
            final Player p = ((OfflinePlayer) sender).getPlayer();
            assert p != null;
            if (args.length == 0) {
                if (Main.getPlugin().arenaNames.contains(config.getString("current-arena"))) {
                    final String path = "Arenas." + config.getString("current-arena") + "." + "spectator" + ".";
                    if (ArenasConfig.isSet(path)) {
                        p.teleport(Arena.pathing(path, ArenasConfig));
                        send(p, "spectator-started-spectating");
                    } else {
                        send(p, "arena-not-set");
                        return true;
                    }
                } else {
                    send(p, "current-arena-not-set");
                    return true;
                }
                if (!config.getBoolean("spectator-visibility")) {
                    for (final Player other : Bukkit.getServer().getOnlinePlayers()) other.hidePlayer(p);
                } else {
                    for (final Player other : Bukkit.getServer().getOnlinePlayers()) other.showPlayer(p);
                }
                if (Objects.equals(config.getString("spectator-gamemode"), "spectator")) {
                    p.setGameMode(GameMode.SPECTATOR);
                } else if (Objects.equals(config.getString("spectator-gamemode"), "adventure")) {
                    p.setGameMode(GameMode.ADVENTURE);
                } else if (Objects.equals(config.getString("spectator-gamemode"), "survival")) {
                    p.setGameMode(GameMode.SURVIVAL);
                } else if (Objects.equals(config.getString("spectator-gamemode"), "creative")) {
                    p.setGameMode(GameMode.CREATIVE);
                } else {
                    send(p, "spectator-wrong-arguments");
                    return true;
                }
                spectators.add(p);
            } else if (args.length == 1) {
                if (args[0].equals("stop")) {
                    if (config.getBoolean("kill-on-fight-end")) {
                        p.setGameMode(gameMode);
                        p.setHealth(0.0f);
                        send(p, "spectator-stopped-spectating");
                    } else {
                        p.setGameMode(gameMode);
                        for (final Player other : Bukkit.getServer().getOnlinePlayers()) other.showPlayer(p);
                        p.teleport(Arena.pathing("fight-end-spawn.", config));
                        send(p, "spectator-stopped-spectating");
                    }
                    spectators.remove(p);
                } else send(p, "not-allowed");
            } else return false;
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull final CommandSender commandSender, @NotNull final Command command, @NotNull final String s, @NotNull final String[] args) {
        if (args.length == 1) return Collections.singletonList("stop");
        return null;
    }

    private static void send(final Player p, final String s) {
        p.sendMessage(Main.conf(s));
    }
}
