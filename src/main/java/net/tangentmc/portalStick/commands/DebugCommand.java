package net.tangentmc.portalStick.commands;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;

public class DebugCommand extends BaseCommand
{
	public DebugCommand()
	{
		super("language", 1, "<- toggles debugging", false);
	}

	public boolean execute()
	{
		if (player != null && player.isOp()) {
			Fireball fireball = (Fireball) player.getWorld().spawnEntity(player.getLocation().add(player.getLocation().getDirection()), EntityType.FIREBALL);
			fireball.setVelocity(player.getLocation().getDirection());
		}

		plugin.getConfiguration().debug = !plugin.getConfiguration().debug;
		Utils.sendMessage(sender, plugin.getI18n().getString(plugin.getConfiguration().debug ? "DebuggingEnabled" : "DebuggingDisabled", playerName));
		plugin.getConfiguration().saveAll();
		return true;
	}

	public boolean permission(Player player) {
		return plugin.hasPermission(player, plugin.PERM_DEBUG);
	}
}
