package net.flex.ManualTournaments;

import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

class MyListener implements Listener {
    private static final FileConfiguration config = Main.getPlugin().getConfig();
    static int y;

    @EventHandler
    private void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        removeEntries();
        if (Fight.team1.contains(p.getUniqueId()) || Fight.team2.contains(p.getUniqueId())) {
            e.setDroppedExp(0);
            if (!config.getBoolean("drop-on-death")) {
                e.getDrops().clear();
            }
        }
        teamRemover(p, Fight.team1, Fight.team2);
        teamRemover(p, Fight.team2, Fight.team1);
        if (config.getBoolean("create-fights-folder")) {
            endCounter();
        }
    }

    private void endCounter() {
        if (Fight.team1.isEmpty() && Fight.team2.isEmpty()) {
            try {
                Fight.FightsConfig.load(Fight.FightsConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
            Fight.FightsConfig.set("Fight-duration", Fight.l - 3);
            y = 1;
            try {
                Fight.FightsConfig.save(Fight.FightsConfigFile);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void removeEntries() {
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (Fight.team1.contains(p.getUniqueId())) {
                Fight.teamA.removeEntry(p.getDisplayName());
            } else if (Fight.team2.contains(p.getUniqueId())) {
                Fight.teamB.removeEntry(p.getDisplayName());
            }
        }
    }

    @SneakyThrows
    private void teamRemover(Player p, Collection<UUID> team1, Collection<UUID> team2) {
        if (team1.contains(p.getUniqueId())) {
            team1.remove(p.getUniqueId());
            if (team1.isEmpty() && !team2.isEmpty()) {
                if (config.getBoolean("create-fights-folder")) {
                    Collection<String> h = new ArrayList<>();
                    Fight.FightsConfig.load(Fight.FightsConfigFile);
                    Fight.FightsConfig.set("Fight-winners", Fight.teamList(team2, h));
                    Fight.FightsConfig.save(Fight.FightsConfigFile);
                }
                new BukkitRunnable() {
                    int i = config.getInt("teleport-countdown-time");

                    @SneakyThrows
                    public void run() {
                        if (i == 0) {
                            Collection<String> a = new ArrayList<>();
                            for (UUID uuid : team2) {
                                a.add(Objects.requireNonNull(Bukkit.getPlayer(uuid)).getDisplayName());
                            }
                            String listString = String.join(", ", a);
                            String replace = Objects.requireNonNull(Main.getPlugin().getConfig().getString("fight-winners")).replace("{team}", listString);
                            Bukkit.getServer().broadcastMessage(replace);
                            if (config.getBoolean("kill-on-fight-end")) {
                                for (Player p1 : Bukkit.getServer().getOnlinePlayers()) {
                                    if (team2.contains(p1.getUniqueId())) {
                                        p1.setHealth(0);
                                    }
                                }
                            } else {
                                String path = "fight-end-spawn.";
                                for (Player p1 : Bukkit.getServer().getOnlinePlayers()) {
                                    if (team2.contains(p1.getUniqueId())) {
                                        if (config.isSet(path)) {
                                            p1.teleport(Arena.pathing(path, config));
                                        }
                                    }
                                }
                            }
                            cancel();
                        }

                        --i;
                    }
                }.runTaskTimer(Main.getPlugin(), 0L, 20L);
            }
        }
    }

    @EventHandler
    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (Fight.temporary.contains(p)) {
            Location from = e.getFrom();
            if (from.getZ() != Objects.requireNonNull(e.getTo()).getZ() && from.getX() != e.getTo().getX() && from.getY() != e.getTo().getY()) {
                p.teleport(e.getFrom());
            }
        }
    }

    @EventHandler
    private void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (Fight.team1.contains(p.getUniqueId()) || Fight.team2.contains(p.getUniqueId())) {
            if (!config.getBoolean("drop-items")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (Fight.team1.contains(p.getUniqueId()) || Fight.team2.contains(p.getUniqueId())) {
            if (!config.getBoolean("break-blocks")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (Fight.temporary.contains(p) || Fight.team1.contains(p.getUniqueId()) || Fight.team2.contains(p.getUniqueId())) {
            if (config.getBoolean("kill-on-fight-end")) {
                p.setHealth(0);
                p.setWalkSpeed(0.2f);
            } else {
                String path = "fight-end-spawn.";
                if (config.isSet(path)) {
                    p.teleport(Arena.pathing(path, config));
                    p.setWalkSpeed(0.2f);
                }
            }
        }
    }
}