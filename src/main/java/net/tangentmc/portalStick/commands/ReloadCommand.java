package net.tangentmc.portalStick.commands;

import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;

public class ReloadCommand extends BaseCommand {

	public ReloadCommand() {
		super("reload", 0, "<- reloads the PortalStick config", false);
	}
	
	public boolean execute() {
		plugin.getConfiguration().reLoad();
		Utils.sendMessage(sender, plugin.getI18n().getString("ConfigurationReloaded", playerName));
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_ADMIN_REGIONS);
	}
}
