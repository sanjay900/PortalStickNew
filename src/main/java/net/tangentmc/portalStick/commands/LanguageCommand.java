package net.tangentmc.portalStick.commands;

import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;

public class LanguageCommand extends BaseCommand
{
	public LanguageCommand()
	{
		super("language", 1, "<- switches the language", false);
	}
	
	public boolean execute() {
		if(plugin.getI18n().setLang(args[0]))
		{
		  Utils.sendMessage(sender, plugin.getI18n().getString("LanguageChanged", playerName, args[0]));
		  plugin.getConfiguration().lang = args[0];
		  plugin.getConfiguration().saveAll();
		}
		else
		  Utils.sendMessage(sender, plugin.getI18n().getString("LanguageNotChanged", playerName, args[0]));
		return true;
	}
	
	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_LANGUAGE);
	}
}
