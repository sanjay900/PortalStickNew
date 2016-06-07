package net.tangentmc.portalStick.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;

public class SayCommand extends BaseCommand {

	public SayCommand() {
		super("say", -1, "<- Say things however you like", false);
	}
	
	public boolean execute() {
		
	    if(args.length < 1) {
	        sendUsage();
	        return true;
	    }
		@SuppressWarnings("deprecation")
		Player p = Bukkit.getPlayer(args[0]);
		if(p != null) {
		    StringBuilder sb = new StringBuilder(args[1]);
		    for (int i = 2; i < args.length ;i++ )
		        sb.append(' ').append(args[i]);
		    p.sendMessage(ChatColor.translateAlternateColorCodes('&', sb.toString()));
		} else
		    Utils.sendMessage(sender, plugin.getI18n().getString("SayFailed", playerName, args[0]));
		
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_SAY);
	}

}