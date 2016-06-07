package net.tangentmc.portalStick.commands;

import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.portalStick.components.Region;

public class RegionListCommand extends BaseCommand {

	public RegionListCommand() {
		super("regionlist", 0, "<- list all portal regions", false);
	}
	
	public boolean execute() {
		Utils.sendMessage(sender, "&c---------- &7Portal Regions &c----------");
		for (Region region : plugin.getRegionManager().regions.values())
			Utils.sendMessage(sender, "&7- &c" + region.name + " &7- &c" + region.min.toString() + " &7-&c " + region.max.toString());
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_ADMIN_REGIONS);
	}

}