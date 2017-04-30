package net.tangentmc.portalStick.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Portal;
import net.tangentmc.portalStick.utils.Util;

import java.util.Objects;

public class DeleteAllCommand extends BaseCommand {
	public DeleteAllCommand()
	{
		super("deleteall", 0, "<- deletes all portals", false);
	}
	
	public boolean execute() {
		//Loop through all worlds and delete all portals
		Bukkit.getWorlds().stream().forEach(world -> world.getEntities().stream().map(en -> Util.getInstance(Portal.PortalEntity.class, en)).filter(Objects::nonNull).forEach(p -> p.getPortal().delete()));
		Utils.sendMessage(sender, plugin.getI18n().getString("AllPortalsDeleted", playerName));
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_DELETE_ALL);
	}

}