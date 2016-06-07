package net.tangentmc.portalStick.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Portal;
import net.tangentmc.portalStick.utils.Util;

public class DeleteAllCommand extends BaseCommand {
	public DeleteAllCommand()
	{
		super("deleteall", 0, "<- deletes all portals", false);
	}
	
	public boolean execute() {
		//Loop through all worlds and delete all portals
		Bukkit.getWorlds().stream().forEach(world -> world.getEntities().stream().filter(en -> Util.checkInstance(Portal.class, en)).map(en -> Util.getInstance(Portal.class, en)).forEach(Portal::delete));
		Utils.sendMessage(sender, plugin.getI18n().getString("AllPortalsDeleted", playerName));
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_DELETE_ALL);
	}

}