package net.tangentmc.portalStick.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import net.tangentmc.nmsUtils.utils.Utils;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class DebugCommand extends BaseCommand {
    public DebugCommand() {
        super("debug", 1, "<- toggles debugging", false);
    }

    public boolean execute() {
        if (player != null && player.isOp()) {
            Boat b = (Boat) player.getWorld().spawnEntity(player.getLocation(), EntityType.BOAT);

            player.setPassenger(b);
            if (argLength > 2) {
                PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.MOUNT);
                pc.getIntegers().write(0, player.getEntityId());
                pc.getIntegerArrays().write(0, new int[]{b.getEntityId()});
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, pc);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
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
