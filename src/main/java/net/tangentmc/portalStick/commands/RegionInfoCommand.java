package net.tangentmc.portalStick.commands;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.components.Region;

public class RegionInfoCommand extends BaseCommand {

	public RegionInfoCommand() {
		super("regioninfo", 0, "<- says the region you are in", true);
	}
	
	public boolean execute() {
	    Location loc = player.getLocation();
		Region region = plugin.getRegionManager().getRegion(new V10Block(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
		Utils.sendMessage(sender, "&7- &c" + region.name + " &7- &c" + region.min.toString() + " &7-&c " + region.max.toString());
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_ADMIN_REGIONS);
	}

}