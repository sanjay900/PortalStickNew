package net.tangentmc.portalStick.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.RegionSetting;
import net.tangentmc.portalStick.utils.Util;

public class Region extends PortalUser 
{
	public HashMap<RegionSetting, Object> settings = new HashMap<RegionSetting, Object>();
	
	private final PortalStick plugin;
	
	public V10Block min, max;
	
	public final String name;
	
	public Portal blueDestination; // Destination of the blue automated portal
	public Portal orangeDestination; // Destination of the orange automated portal
	
	public Region(PortalStick plugin, String name)
	{
		super("§region§_"+name);
		this.plugin = plugin;
		this.name = name;
	}
	@Override
	public Portal getPrimary() {
		if (super.getPrimary() != null) return super.getPrimary();
		World world = null;
		if (min.getWorldName() != null) {
			 world = min.getHandle().getWorld();
		} else {
			if (secondary != null) {
				world = secondary.bottom.getWorld();
			} else {
				return null;
			}
		}
		Portal portal;
		for (Entity en : world.getEntities()) {
			portal = Util.getInstance(Portal.class, en);
			if (portal != null && portal.isPrimary()) {
				return portal;
			}
		}
		return null;
	}

	@Override
	public Portal getSecondary() {
		if (super.getSecondary() != null) return super.getSecondary();
		Portal portal;
		World world = null;
		if (min.getWorldName() != null) {
			 world = min.getHandle().getWorld();
		} else {
			if (secondary != null) {
				world = secondary.bottom.getWorld();
			} else {
				return null;
			}
		}
		for (Entity en : world.getEntities()) {
			portal = Util.getInstance(Portal.class, en);
			if (portal != null && !portal.isPrimary()) {
				return portal;
			}
		}
		return null;
	}
	public boolean updateLocation(Player player) {
		String[] loc = getString(RegionSetting.LOCATION).split(":");
		V10Block min, max;
		if(this.name.equals("global"))
		  min = max = new V10Block((String)null, 0, 0, 0);
		else
		{
		  String[] loc1 = loc[1].split(",");
		  
		  int aX = Integer.parseInt(loc1[0]);
		  int aY = Integer.parseInt(loc1[1]);
		  int aZ = Integer.parseInt(loc1[2]);
		  
		  loc1 = loc[2].split(",");
		  int bX = Integer.parseInt(loc1[0]);
		  int bY = Integer.parseInt(loc1[1]);
		  int bZ = Integer.parseInt(loc1[2]);
		  
		  if(aX > bX)
		  {
			int tmp = aX;
			aX = bX;
			bX = tmp;
		  }
		  if(aY > bY)
		  {
			int tmp = aY;
			aY = bY;
			bY = tmp;
		  }
		  if(aZ > bZ)
		  {
			int tmp = aZ;
			aZ = bZ;
			bZ = tmp;
		  }
		  
		  ArrayList<V10Block> locs = new ArrayList<V10Block>(); 
		  for(int x = aX; x <= bX; x++)
			for(int y = aY; y <= bY; y++)
			  for(int z = aZ; z <= bZ; z++)
				locs.add(new V10Block(loc[0], x, y, z));
		  
		  min = new V10Block(loc[0], aX, aY, aZ);
		  max = new V10Block(loc[0], bX, bY, bZ);
		  
		  for(Region region: plugin.getRegionManager().regions.values())
			if(region != this && !region.name.equals("global"))
			  for(V10Block vLoc: locs)
				if(region.contains(vLoc))
				{
				  if(player != null)
					  Utils.sendMessage(player, plugin.getI18n().getString("RegionsOverlap", player.getName(), name, region.name));
				  if(plugin.getConfiguration().debug)
					  plugin.getLogger().info("Region \""+name+"\" overlaps with region \""+region.name+"\". Removing.");
				  return false;
				}
		}
		this.min = min;
		this.max = max;
		return true;
	}
	
	public boolean setLocation(Player player, V10Block one, V10Block two) {
		String old = (String)settings.get(RegionSetting.LOCATION);
		settings.put(RegionSetting.LOCATION, one.getWorldName() + ":" + one.getX()+","+one.getY()+","+one.getZ() + ":" + two.getX()+","+two.getY()+","+two.getZ());
		if(updateLocation(player))
		  return true;
		if(old == null)
		  settings.remove(RegionSetting.LOCATION);
		else
		  settings.put(RegionSetting.LOCATION, old);
		return false;
	}
	
	//Called when any portal in this region is deleted
	public void portalDeleted(Portal portal)
	{
		
	}
	
	//Called when any portal in this region is created
	public void portalCreated(Portal portal)
	{
		
	}
		
	public boolean contains(V10Block loc) {
		return loc.getWorldName().equals(min.getWorldName()) &&
				loc.getX() >= min.getX() && loc.getX() <= max.getX() &&
				loc.getY() >= min.getY() && loc.getY() <= max.getY() &&
				loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ();
	}
	
	public boolean getBoolean(RegionSetting setting) {
		return (Boolean)settings.get(setting);
	}
	public int getInt(RegionSetting setting) {
		return (Integer)settings.get(setting);
	}
	public List<?> getList(RegionSetting setting) {
		return (List<?>)settings.get(setting);
	}
	public String getString(RegionSetting setting) {
		Object ret = settings.get(setting);
		if(ret instanceof String)
		  return (String)ret;
		else if(ret instanceof Integer || ret instanceof Long)
		  return ""+ret;
		
		return ret.toString();
	}
	public double getDouble(RegionSetting setting) {
		return (Double)settings.get(setting);
	}
	
	public boolean validateRedGel()
	{
		if(getDouble(RegionSetting.RED_GEL_MAX_VELOCITY) > 1.0D)
		{
			settings.remove(RegionSetting.RED_GEL_MAX_VELOCITY);
			settings.put(RegionSetting.RED_GEL_MAX_VELOCITY, 1.0D);
			return false;
		}
		return true;
	}
	
	public boolean isException(String string) {
        return getList(RegionSetting.GRILL_REMOVE_EXCEPTIONS).contains(string);
    }
}
