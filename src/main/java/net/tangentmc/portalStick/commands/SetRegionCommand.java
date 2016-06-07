package net.tangentmc.portalStick.commands;

import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.PortalUser;

public class SetRegionCommand extends BaseCommand {
	
	public SetRegionCommand() {
		super("setregion", 1, "<name> <- saves selected region", true);
	}
	
	public boolean execute() {
		PortalUser user = plugin.getUser(player.getName());
		args[0] = args[0].toLowerCase();
		if (user.getPointOne() == null || user.getPointTwo() == null)
			Utils.sendMessage(sender, plugin.getI18n().getString("RegionToolNoPointsSelected", playerName, args[0]));
		else if (plugin.getRegionManager().getRegion(args[0]) != null)
			Utils.sendMessage(sender, plugin.getI18n().getString("RegionExists", playerName, args[0]));
		else if (plugin.getRegionManager().createRegion(player, args[0], user.getPointOne(), user.getPointTwo()))
			Utils.sendMessage(sender, plugin.getI18n().getString("RegionCreated", playerName, args[0]));
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_ADMIN_REGIONS);
	}
	
}
