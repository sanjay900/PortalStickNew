package net.tangentmc.portalStick.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.material.Directional;
import org.bukkit.material.Lever;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import lombok.SneakyThrows;
import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.nmsUtils.utils.VelocityUtil;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.Config.Sound;

public class Util {

	public static void playSound(Sound sound, V10Block loc)
	{
		PortalStick plugin = PortalStick.getInstance();
		if (!plugin.getRegionManager().getRegion(loc).getBoolean(RegionSetting.ENABLE_SOUNDS))
			return;

		String raw = plugin.getConfiguration().soundNative[sound.ordinal()];
		if(raw == null || raw.equals(""))
		{
			if(plugin.getConfiguration().debug)
				plugin.getLogger().info("Sound "+sound.toString()+" not found!");
			return;
		}
		String[] split = raw.split(":");
		float volume = 1.0F;
		float pitch = volume;
		if(split.length > 1)
			try
		{
				volume = Float.parseFloat(split[1]);
		}
		catch(Exception e)
		{
			if(plugin.getConfiguration().debug)
				plugin.getLogger().info("Warning: Invalid volume \""+split[1]+"\" for sound "+split[0]);
			volume = 1.0F;
		}
		if(split.length > 2)
		{
			try
			{
				pitch = Float.parseFloat(split[2]);
			}
			catch(Exception e)
			{
				if(plugin.getConfiguration().debug)
					plugin.getLogger().info("Warning: Invalid pitch \""+split[2]+"\" for sound "+split[0]);
				pitch = 1.0F;
			}
		}
		try
		{
			org.bukkit.Sound s = org.bukkit.Sound.valueOf(split[0]);
			loc.getHandle().getWorld().playSound(loc.getHandle(), s, volume, pitch);
		}
		catch(IllegalArgumentException e)
		{
			playWorldCustomSound(loc.getHandle(), split[0],10);
		}


	}
	public static void playWorldCustomSound(Location l, String sound, int radius) {
		l.getWorld().playSound(l, sound, 1, 1);
	}
	public static int getLeftPortalColor(int preset)
	{
		return Integer.parseInt(PortalStick.getInstance().getConfiguration().ColorPresets.get(preset).split("-")[0]);
	}

	public static int getRightPortalColor(int preset)
	{
		return Integer.parseInt(PortalStick.getInstance().getConfiguration().ColorPresets.get(preset).split("-")[1]);
	}

	@SuppressWarnings("deprecation")
	public static boolean isPortalGun(ItemStack item) {
		return item != null && item.getTypeId() == PortalStick.getInstance().getConfiguration().PortalTool &&
				item.getDurability() == PortalStick.getInstance().getConfiguration().portalToolData &&
				item.getItemMeta().getDisplayName() != null &&
				item.getItemMeta().getDisplayName().equals(PortalStick.getInstance().getConfiguration().portalToolName);
	}

	public static boolean isGravityGun(ItemStack item) {
		return item != null && item.getType() ==Material.BLAZE_ROD  &&
				item.getItemMeta().getDisplayName() != null &&
				item.getItemMeta().getDisplayName().equals(PortalStick.getInstance().getConfiguration().gravityGunName);
	}

	public static ItemStack createPortalGun() {
		PortalStick plugin = PortalStick.getInstance();
		@SuppressWarnings("deprecation")
		ItemStack gun = new ItemStack(plugin.getConfiguration().PortalTool, 1, plugin.getConfiguration().portalToolData);
		Utils.setItemNameAndDesc(gun, plugin.getConfiguration().portalToolName, plugin.getConfiguration().portalToolDesc);
		return gun;
	}

	public static ItemStack createGravityGun() {
		PortalStick plugin = PortalStick.getInstance();
		ItemStack gun = new ItemStack(Material.BLAZE_ROD, 1);
		Utils.setItemNameAndDesc(gun, plugin.getConfiguration().gravityGunName, plugin.getConfiguration().gravityGunDesc);
		return gun;
	}
	public static boolean setState(Block block) {
		Block source = block.getRelative(((Attachable)block.getState().getData()).getAttachedFace());
		// if the state changed lets apply physics to the source block and the lever itself
		MaterialData md = block.getState().getData();
		BlockState bs = block.getState();
		boolean state;
		if (md instanceof Lever) {
			Lever l = (Lever)md;
			l.setPowered(!l.isPowered());
			state = l.isPowered();
			bs.setData(l);
			bs.update();
		} else {
			org.bukkit.material.Button l = (org.bukkit.material.Button)md;
			l.setPowered(!l.isPowered());
			bs.setData(l);
			state = l.isPowered();
			bs.update();
		}
		BlockState initialSupportState = source.getState();
		BlockState supportState = source.getState();
		supportState.setType(Material.AIR);
		supportState.update(true, false);
		initialSupportState.update(true);

		// lets call blockredstone events on the source block and the lever
		// in order to correctly update all surrounding blocks
		BlockRedstoneEvent leverEvent = new BlockRedstoneEvent(block, state ? 0 : 15, state ? 15 : 0);
		BlockRedstoneEvent sourceEvent = new BlockRedstoneEvent(source, state ? 0 : 15, state ? 15 : 0);
		Bukkit.getServer().getPluginManager().callEvent(leverEvent);
		Bukkit.getServer().getPluginManager().callEvent(sourceEvent);
		return state;
	}
	static BlockFace[] faces = new BlockFace[]{BlockFace.WEST,BlockFace.NORTH,BlockFace.EAST,BlockFace.SOUTH};
	public static BlockFace getGlassPaneDirection(Block glass, Material... allowed) {
		ArrayList<Material> allowedl = new ArrayList<>(Arrays.asList(allowed));
		allowedl.add(Material.AIR);
		List<BlockFace> facing = Arrays.stream(faces).filter(f -> allowedl.contains(glass.getRelative(f).getType())).collect(Collectors.toList());
		if (facing.size() == 1) return facing.get(0);
		return null;
	}
	public static boolean checkPiston(Location piston, Entity entity) {
		PortalStick plugin = PortalStick.getInstance();
		if (entity.hasMetadata("pistonen")) return false;
		Block pistonBlock = piston.getBlock();
		if (pistonBlock.getType() != Material.PISTON_BASE || pistonBlock.getType() != Material.PISTON_STICKY_BASE) {
			return false;
		}
		final Location entityLoc = pistonBlock.getLocation();
		BlockFace pistondir = ((Directional) pistonBlock.getState().getData())
				.getFacing().getOppositeFace();
		Block sBlock = pistonBlock.getRelative(pistondir);
		if ((sBlock.getType()
				.equals(Material.WALL_SIGN) || sBlock.getType()
				.equals(Material.SIGN_POST))) {
			Sign s = (Sign) sBlock.getState();

			if(!s.getLine(0).equalsIgnoreCase("[PortalStick]"))
				return false;
			double x = 0.0D;
			double y = 0.0D;
			double z = 0.0D;
			int height = 0;
			final boolean pos = !s.getLine(1).contains("direction");
			boolean ok = true;
			if (!pos) {
				y = entity.getLocation().getDirection().getY();
				double tmp = y;
				if (s.getLine(1).contains(",")) {

					String[] text = s.getLine(1).split(",");
					if (y < Double.parseDouble(text[1]))
					{
						try {
							y = Double.parseDouble(text[1]);
						} catch (Exception nfe) {
							y = tmp;
						}
					}
				}
			} else {
				String[] text = s.getLine(1).split(",");
				try {
					x = Double.parseDouble(text[0]);
					y = Double.parseDouble(text[1]);
					z = Double.parseDouble(text[2]);
				} catch (Exception nfe) {
					ok = false;
				}
				try {
					height = Integer.parseInt(s.getLine(2));
				} catch (Exception nfe) {
					height=10;
				}
			}
			if (ok) {

				Bukkit.getScheduler().scheduleSyncDelayedTask(
						plugin,
						new SignResetter(s),
						2L);
				if (entity.isInsideVehicle()) {
					entity.getVehicle().eject();
				}
				Vector vector = new Vector(0, 0, 0);
				if (pos) {
					Location dest = new Location(
							entityLoc.getWorld(), x, y, z);
					vector = VelocityUtil.calculateVelocity(entityLoc.toVector(), dest.toVector(), height);

				} else {
					vector = entity.getLocation().getDirection();
				}
				FallingBlock as = entity.getWorld().spawnFallingBlock(entityLoc, Material.BARRIER, (byte)0);
				as.setPassenger(entity);
				as.setVelocity(vector);
				as.setDropItem(false);
				as.setMetadata("pistonen", new FixedMetadataValue(PortalStick.getInstance(),true));
				final V10Block loc = new V10Block(sBlock);
				Bukkit.getScheduler().scheduleSyncDelayedTask(
						plugin, new Runnable() {
							@Override
							public void run() {
								Block block = loc.getHandle().getBlock();
								Sign s = (Sign) block.getState();
								block.setType(
										Material.REDSTONE_BLOCK);
								Bukkit.getScheduler()
								.scheduleSyncDelayedTask(
										plugin,
										new SignResetter(s),
										2L);
							}
						}, 2L);

			}
			return ok;
		}



		return false;
	}

	private static class SignResetter implements Runnable {
		final V10Block v10loc;
		final Material type;
		final String[] lines;
		final byte data;

		SignResetter(Sign s) {
			this.v10loc = new V10Block(s.getLocation());
			this.type = s.getType();
			this.data = s.getRawData();
			this.lines = s.getLines();
		}
		public void run() {
			Block block = v10loc.getHandle().getBlock();
			block.setType(type);
			block.setData(data);
			Sign newSign = (Sign) block.getState();
			for (int i = 0; i < 4; i++) {
				newSign.setLine(i, lines[i]);
			}
			newSign.update();
		}
	}
	@SneakyThrows
	public static <T extends MetadataSaver>  T retrieveMetadata(Block b, int radius, Class<T> clazz) {
		for (Entity e:b.getWorld().getNearbyEntities(b.getLocation(), radius, radius, radius)) {
			if (checkInstance(clazz, e)) {
				return getInstance(clazz,e);
			}
		}
		return null;
	}
	@SneakyThrows
	public static <T extends MetadataSaver> boolean checkInstance(Class<T> clazz, Entity en) {
		return MetadataSaver.isInstance(en,clazz);
	}
	@SneakyThrows
	public static <T extends MetadataSaver> T getInstance(Class<T> clazz, Entity en) {
		return MetadataSaver.get(en,clazz);
	}
	@SneakyThrows
	public static <T extends MetadataSaver> T getInstance(String name, Entity en) {
		return MetadataSaver.get(en,name);
	}
	public static boolean isTranslucent(Material mt) {
		return mt.name().contains("GLASS") || !mt.isSolid() || mt.isTransparent();
	}
}
