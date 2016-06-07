package net.tangentmc.portalStick.commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.portalStick.components.PortalUser;

public class RegionToolCommand extends BaseCommand {

	public RegionToolCommand() {
		super("regiontool", 0, "<- enable/disable region selection mode", true);
	}
	
	@SuppressWarnings("deprecation")
	public boolean execute() {
		PortalUser user = plugin.getUser(player.getName());
		if (user.isUsingTool()) {
			Utils.sendMessage(sender, plugin.getI18n().getString("RegionToolDisabled", playerName));
		}
		else {
			Utils.sendMessage(sender, plugin.getI18n().getString("RegionToolEnabled", playerName));
			if (!player.getInventory().contains(plugin.getConfiguration().RegionTool))
					player.getInventory().addItem(new ItemStack(plugin.getConfiguration().RegionTool, 1));
		}
		user.setUsingTool(!user.isUsingTool());
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_ADMIN_REGIONS);
	}

}
