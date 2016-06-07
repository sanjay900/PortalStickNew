package net.tangentmc.portalStick.commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tangentmc.portalStick.utils.Util;

public class GetGunCommand extends BaseCommand {

    public GetGunCommand()
    {
      super( "gun", 0, "<- gives you the portal gun.", true);
    }
    
    public boolean execute() {
        ItemStack gun = Util.createPortalGun();
        if(!player.getInventory().addItem(gun).isEmpty())
            player.getWorld().dropItemNaturally(player.getLocation(), gun);
        return true;
    }
    
    public boolean permission(Player player) {
        return plugin.hasPermission(player, plugin.PERM_GET_GUN);
    }
}
