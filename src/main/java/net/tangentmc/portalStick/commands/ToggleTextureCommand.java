package net.tangentmc.portalStick.commands;

import org.bukkit.World;
import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.portalStick.components.PortalUser;

public class ToggleTextureCommand extends BaseCommand
{
    public ToggleTextureCommand()
    {
        super("texture", 0, "<- toggles the texture", false);
    }
    
    public boolean execute()
    {
        boolean useTexture = plugin.getConfiguration().toggleTextureURL(true);
        for(Player p: plugin.getServer().getOnlinePlayers()) {
    		World world = p.getWorld();
    		boolean disabled = plugin.getConfiguration().DisabledWorlds.contains(world.getName());
    		PortalUser user = plugin.getUser(p.getName());
    		String texturePack = disabled ? plugin.getConfiguration().defaultTextureURL : plugin.getConfiguration().textureURL;
    		if((user.isHasDefaultTexture() && !disabled) || (!user.isHasDefaultTexture() && disabled)) {
    			p.setResourcePack(texturePack);
    			user.setHasDefaultTexture(disabled);
    		}
        }
        
        Utils.sendMessage(sender, plugin.getI18n().getString(useTexture ? "TextureEnabled" : "TextureDisabled", playerName));
        return true;
    }
    
    public boolean permission(Player player) {
        return plugin.hasPermission(player, plugin.PERM_TEXTURE);
    }
}
