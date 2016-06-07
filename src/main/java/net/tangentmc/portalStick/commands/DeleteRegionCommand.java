package net.tangentmc.portalStick.commands;

import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.portalStick.PortalStick;

public class DeleteRegionCommand extends BaseCommand {

	public DeleteRegionCommand() {
		super("deleteregion", 1, "<name> <- deletes specified region", false);
	}
	
	public boolean execute() {
		if (args[0].equalsIgnoreCase("global"))
			Utils.sendMessage(sender, plugin.getI18n().getString("CanNotDeleteGlobalRegion", playerName));
		else if (plugin.getRegionManager().getRegion(args[0]) != null) {
			plugin.getRegionManager().deleteRegion(args[0]);
			plugin.getConfiguration().reLoad();
			Utils.sendMessage(sender, plugin.getI18n().getString("RegionDeleted", playerName, args[0]));
		}
		else Utils.sendMessage(sender, plugin.getI18n().getString("RegionNotFound", playerName, args[0]));
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_ADMIN_REGIONS);
	}

}