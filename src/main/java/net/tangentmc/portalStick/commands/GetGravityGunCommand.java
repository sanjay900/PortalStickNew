package net.tangentmc.portalStick.commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tangentmc.portalStick.utils.Util;

public class GetGravityGunCommand extends BaseCommand {

    public GetGravityGunCommand()
    {
      super("gravitygun", 0, "<- gives you the gravity gun.", true);
    }
    
    public boolean execute() {
        ItemStack gun = Util.createGravityGun();
        if(!player.getInventory().addItem(gun).isEmpty())
            player.getWorld().dropItemNaturally(player.getLocation(), gun);
        return true;
    }
    
    public boolean permission(Player player) {
        return plugin.hasPermission(player, plugin.PERM_GET_GRAVITY_GUN);
    }
}
