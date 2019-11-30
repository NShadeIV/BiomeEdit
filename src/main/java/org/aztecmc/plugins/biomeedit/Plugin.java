package org.aztecmc.plugins.biomeedit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.function.FlatRegionFunction;
import com.sk89q.worldedit.function.FlatRegionMaskingFilter;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.FlatRegionVisitor;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin {

    @Override
    public void onEnable() {
        //saveDefaultConfig();
        getCommand("/replacebiome").setExecutor(this);
        getLogger().info("loaded");
    }

    @Override
    public void onDisable() {
        getLogger().info("unloaded");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final String USAGE = "Usage: //replacebiome <old_biome>[,<old_biome>]* <new_biome>";

        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) {
            sender.sendMessage("Error: Player is null.");
            return false;
        }

        WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager()
                .getPlugin("WorldEdit");
        if (worldEditPlugin == null) {
            sender.sendMessage("Error: WorldEdit is null.");
            return false;
        }

        Set<BiomeType> oldBiomes;
        try {
            oldBiomes = Arrays.stream(args[0].split(",")).map(s -> BiomeTypes.get(s)).collect(Collectors.toSet());
        } catch (Exception e) {
            sender.sendMessage(USAGE);
            log(e);
            return false;
        }

        BiomeType newBiome;
        try {
            newBiome = BiomeTypes.get(args[1]);
        } catch (Exception e) {
            sender.sendMessage(USAGE);
            log(e);
            return false;
        }

        LocalSession localSession = worldEditPlugin.getSession(player);
        BukkitPlayer localPlayer = worldEditPlugin.wrapPlayer(player);
        EditSession editSession = worldEditPlugin.createEditSession(player);

        Region region;
        try {
            region = localSession.getSelection(localPlayer.getWorld());

        } catch (IncompleteRegionException e) {
            player.sendMessage("Error: Selection is null.");
            log(e);
            return false;
        }

        Mask mask = localSession.getMask();
        Mask2D mask2d = mask != null ? mask.toMask2D() : null;

        FlatRegionFunction replace = (BlockVector2 position) -> {
            BiomeType oldBiome = editSession.getBiome(position);
            if (oldBiomes.contains(oldBiome))
                return editSession.setBiome(position, newBiome);
            return false;
        };
        if (mask2d != null) {
            replace = new FlatRegionMaskingFilter(mask2d, replace);
        }
        FlatRegionVisitor visitor = new FlatRegionVisitor(Regions.asFlatRegion(region), replace);

        try {
            Operations.completeLegacy(visitor);
        } catch (MaxChangedBlocksException e) {
            localPlayer.print("Biomes were changed in " + visitor.getAffected() + " columns. You may have to rejoin your game (or close and reopen your world) to see a change.");
            localPlayer.printError("Not all requested edits were made.");
            log(e);
            return false;
        }

        localPlayer.print("Biomes were changed in " + visitor.getAffected() + " columns. You may have to rejoin your game (or close and reopen your world) to see a change.");

        return true;
    }

    private void log(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        getLogger().warning(writer.toString());
    }
}