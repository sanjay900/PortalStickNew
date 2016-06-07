package net.tangentmc.portalStick.commands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;

public class HelpCommand extends BaseCommand {

	public HelpCommand()
	{
	  super("help", 0, "<- lists all PortalStick commands", false);
	}
	
	public boolean execute() {
		player.sendMessage(ChatColor.RED+"--------------------- "+ChatColor.GRAY+"PortalStick "+ChatColor.RED+"---------------------");
		for (BaseCommand cmd : plugin.getCommands())
			if ((player != null && cmd.permission(player)) || (player == null && !cmd.bePlayer))
				Utils.sendMessage(sender, "&7- /"+usedCommand+" &c" + cmd.name + " &7" + cmd.usage);
		return true;
	}
	
	public boolean permission(Player player) {
		return true;
	}

}