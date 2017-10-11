package net.tangentmc.portalStick.commands;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.portalStick.PortalStick;
import org.bukkit.entity.Player;

public class DebugCommand extends BaseCommand {
    public DebugCommand() {
        super("debug", 1, "<- toggles debugging", false);
    }

    public boolean execute() {
         plugin.getConfiguration().debug = !plugin.getConfiguration().debug;
        Utils.sendMessage(sender, plugin.getI18n().getString(plugin.getConfiguration().debug ? "DebuggingEnabled" : "DebuggingDisabled", playerName));
        plugin.getConfiguration().saveAll();
        return true;
    }

    public boolean permission(Player player) {
        return plugin.hasPermission(player, PortalStick.PERM_DEBUG);
    }
}
