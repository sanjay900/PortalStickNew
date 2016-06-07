package net.tangentmc.portalStick.commands;

import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;

public class DeleteCommand extends BaseCommand {

	public DeleteCommand() {
		super("delete", 0, "<- deletes your portals", true);
	}
	
	public boolean execute() {
		plugin.getUser(playerName).removeAllPortals();
		Utils.sendMessage(sender, plugin.getI18n().getString("OwnPortalsDeleted", playerName));
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_PLACE_PORTAL);
	}

}