package net.tangentmc.portalStick.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.AutomatedPortal;
import net.tangentmc.portalStick.components.Grill;
import net.tangentmc.portalStick.components.Region;
import net.tangentmc.portalStick.components.Wire.PoweredReason;
import net.tangentmc.portalStick.utils.RegionSetting;
import net.tangentmc.portalStick.utils.Util;

public class BlockListener implements Listener{
	public BlockListener() {
		Bukkit.getPluginManager().registerEvents(this, PortalStick.getInstance());
	}

	@EventHandler
	public void loadWorld(WorldLoadEvent evt) {
		PortalStick.getInstance().getWireManager().loadWorld(evt.getWorld());
	}

	@EventHandler
	public void loadChunk(ChunkLoadEvent evt) {
		PortalStick.getInstance().getWireManager().loadChunk(evt.getChunk());
	}
	@EventHandler
	public void onBlockBreak(BlockBreakEvent evt) {
		PortalStick.getInstance().getBridgeManager().blockUpdate(evt.getBlock());
	}
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent evt) {
		PortalStick.getInstance().getBridgeManager().blockUpdate(evt.getBlock());
	}
	ArrayList<Grill> offGrills = new ArrayList<>();
	Predicate<V10Block> notPowered = b -> !b.getHandle().getBlock().isBlockPowered()&&!b.getHandle().getBlock().isBlockIndirectlyPowered();
	Predicate<V10Block> isPowered = b -> b.getHandle().getBlock().isBlockPowered()||b.getHandle().getBlock().isBlockIndirectlyPowered();
	@EventHandler
	public void onRedstone(BlockRedstoneEvent evt) {
		PortalStick.getInstance().getWireManager().powerBlock(evt.getBlock(),evt.getNewCurrent() > 0,PoweredReason.REDSTONE);
		PortalStick.getInstance().getBridgeManager().powerBlock(evt.getBlock(),evt.getNewCurrent() > 0);
		Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(),() -> {
			Grill g = Util.retrieveMetadata(evt.getBlock(), 3, Grill.class);
			if (g == null && evt.getNewCurrent() == 0) {
				Iterator<Grill> it = offGrills.iterator();
				while (it.hasNext()) {
					Grill g2 = it.next();
					if (g2.getBorder().stream().allMatch(notPowered)) {
						g2.open();
						it.remove();
					}
				}
			}
			if (g != null && evt.getNewCurrent() > 0 && g.getBorder().stream().anyMatch(isPowered)) {
				g.close();
				offGrills.add(g);
			}
		},1l);
		AutomatedPortal portal = Util.retrieveMetadata(evt.getBlock(), 4, AutomatedPortal.class);
		if (portal == null) return;
		Block wool = portal.getColor();

		Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), ()->{
			if (FaceUtil.isVertical(portal.getFacing())) {
				BlockFace face = BlockFace.WEST;
				if (wool.getRelative(face).isBlockPowered()||wool.getRelative(face).isBlockIndirectlyPowered()) {
					portal.open();
				}
				face = BlockFace.NORTH;
				if (wool.getRelative(face).isBlockPowered()||wool.getRelative(face).isBlockIndirectlyPowered()) {
					portal.open();
				}
				face = BlockFace.EAST;
				if (wool.getRelative(face).isBlockPowered()||wool.getRelative(face).isBlockIndirectlyPowered()) {
					portal.close();
				}
				face = BlockFace.SOUTH;
				if (wool.getRelative(face).isBlockPowered()||wool.getRelative(face).isBlockIndirectlyPowered()) {
					portal.close();
				}
				return;
			}
			BlockFace face = FaceUtil.rotate(portal.getFacing(), 2);
			if (wool.getRelative(face).isBlockPowered()||wool.getRelative(face).isBlockIndirectlyPowered()) {
				portal.open();
			}
			face = FaceUtil.rotate(portal.getFacing(), -2);
			if (wool.getRelative(face).isBlockPowered()||wool.getRelative(face).isBlockIndirectlyPowered()) {
				portal.close();
			}
		},1l);
	}
	@EventHandler
	public void infiniteDispenser(BlockDispenseEvent event)
	{
		PortalStick plugin = PortalStick.getInstance();
		if(plugin.getConfiguration().DisabledWorlds.contains(event.getBlock().getLocation().getWorld().getName()))
			return;
		BlockState bs = event.getBlock().getState();
		org.bukkit.material.Dispenser disp = (org.bukkit.material.Dispenser) bs.getData();
		boolean dropper = bs instanceof Dropper;
		if(!(bs instanceof Dispenser) && !dropper)
			return;
		InventoryHolder ih = (InventoryHolder) bs;
		ItemStack is = ih.getInventory().getItem(4);
		if(is == null)
			return;
		Material mat = is.getType();
		if (mat == Material.STICK) {
			if (FaceUtil.isVertical(disp.getFacing())) {
				Block b = event.getBlock().getRelative(BlockFace.UP,2);
				if (b.getType() == Material.DROPPER) {
					disp = (org.bukkit.material.Dispenser) b.getState().getData();
					summonStand(b.getLocation(),disp.getFacing());
					event.setCancelled(true);
					return;
				}
			}
		}
		if(!dropper && (mat == Material.BUCKET || mat == Material.WATER_BUCKET || mat == Material.LAVA_BUCKET || mat == Material.FLINT_AND_STEEL))
			return;
		Region region = plugin.getRegionManager().getRegion(new V10Block(bs.getLocation()));
		if(region.getBoolean(RegionSetting.GEL_TUBE))
		{

			ItemStack gel = Utils.getItemData(region.getString(RegionSetting.RED_GEL_BLOCK));


			if(mat == gel.getType() && is.getDurability() == gel.getDurability())
			{
				event.setCancelled(true);
				Block to = bs.getBlock();
				V10Block from = new V10Block(to);
				plugin.getGelManager().createTube(from, ((org.bukkit.material.DirectionalContainer) bs.getData()).getFacing(), is);
				plugin.getConfiguration().saveAll();
				return;
			}
			else
			{
				gel = Utils.getItemData(region.getString(RegionSetting.BLUE_GEL_BLOCK));
				if(mat == gel.getType() && is.getDurability() == gel.getDurability())
				{
					event.setCancelled(true);
					Block to = bs.getBlock();
					V10Block from = new V10Block(to);
					plugin.getGelManager().createTube(from, ((org.bukkit.material.DirectionalContainer) bs.getData()).getFacing(), is);
					plugin.getConfiguration().saveAll();
					return;
				}
			}
			gel = new ItemStack(Material.ICE);
			if(mat == gel.getType())
			{
				event.setCancelled(true);
				Block to = bs.getBlock();
				V10Block from = new V10Block(to);
				plugin.getGelManager().createTube(from, ((org.bukkit.material.DirectionalContainer) bs.getData()).getFacing(), is);
				return;
			}
		}
		if(region.getBoolean(RegionSetting.INFINITE_DISPENSERS))
		{
			if(is.getType() != Material.AIR)
				is.setAmount(is.getAmount() + 1);
		}
	}

	private void summonStand(Location location, BlockFace facing) {
		location.setDirection(FaceUtil.faceToVector(facing));
		ArmorStand as = (ArmorStand) location.getWorld().spawnEntity(
				location.add(0.5, -0.25, 0.5)
				.add(FaceUtil.faceToVector(FaceUtil.rotate(facing,-2),0.2))
				.add(FaceUtil.faceToVector(facing.getOppositeFace(),0.2))
				, EntityType.ARMOR_STAND);
		NMSArmorStand.wrap(as).lock();
		NMSArmorStand.wrap(as).setWillSave(false);
        //Fix
		as.setRightArmPose(new EulerAngle(Math.toRadians(0),0,Math.toRadians(-5)));
		as.setItemInHand(new ItemStack(Material.STICK));
		as.setCustomName("portalstand");
		as.setGravity(false);
		as.setVisible(false);
	}
}
