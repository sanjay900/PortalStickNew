package net.tangentmc.portalStick.managers;

import java.util.HashMap;

import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Region;

public class RegionManager {
	private final PortalStick plugin=PortalStick.getInstance();
	
	public final HashMap<String, Region> regions = new HashMap<String, Region>();
	
	public Region loadRegion(String name, Player player, Region region) {
		name = name.toLowerCase();
		if(region == null)
		  region = getRegion(name);
		if(region == null)
		  region = new Region(plugin, name);
		if(plugin.getConfiguration().loadRegionSettings(region, player))
		  regions.put(name, region);
		else
		{
		  region = null;
		  plugin.getConfiguration().deleteRegion(name);
		}
		return region;
	}
	
	public void deleteRegion(String name) {
		Region region = getRegion(name);
		regions.remove(region.name);
		plugin.getConfiguration().deleteRegion(name);
	}
	
	public boolean createRegion(Player player, String name, V10Block one, V10Block two) {
		name = name.toLowerCase();
		Region region = new Region(plugin, name);
		boolean ret = region.setLocation(player, one, two);
		if(ret)
		{
		  ret = (loadRegion(name, player, region) != null);
		  plugin.getConfiguration().saveAll();
		}
		return ret;
	}
	
	public Region getRegion(V10Block location) {
		for (Region region : regions.values())
			if (region.contains(location) && !region.name.equals("global"))
				return region;
		return getRegion("global");
	}
	
	public Region getRegion(String name) {
		return regions.get(name.toLowerCase());
	}
	
}
