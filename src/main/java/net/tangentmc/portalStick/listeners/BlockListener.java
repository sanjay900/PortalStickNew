package net.tangentmc.portalStick.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

import net.tangentmc.nmsUtils.utils.BlockUtil;
import net.tangentmc.portalStick.components.*;
import net.tangentmc.portalStick.managers.CubeManager;
import net.tangentmc.portalStick.utils.GelType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
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
import org.bukkit.material.*;
import org.bukkit.util.EulerAngle;

import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Wire.PoweredReason;
import net.tangentmc.portalStick.utils.RegionSetting;
import net.tangentmc.portalStick.utils.Util;

public class BlockListener implements Listener{
	public BlockListener() {
		Bukkit.getPluginManager().registerEvents(this, PortalStick.getInstance());
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent evt) {
		PortalStick.getInstance().getBridgeManager().blockUpdate(evt.getBlock());
        PortalStick.getInstance().getWireManager().blockBreak(evt.getBlock());
        PortalStick.getInstance().getGrillManager().blockBreak(evt.getBlock());
        Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(),()->
                PortalStick.getInstance().getWireManager().blockUpdate(evt.getBlock()),1L);
	}
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent evt) {
		PortalStick.getInstance().getBridgeManager().blockUpdate(evt.getBlock());
        PortalStick.getInstance().getWireManager().blockUpdate(evt.getBlock());
	}
	ArrayList<Grill> offGrills = new ArrayList<>();
	Predicate<V10Block> notPowered = b -> !b.getHandle().getBlock().isBlockPowered()&&!b.getHandle().getBlock().isBlockIndirectlyPowered();
	Predicate<V10Block> isPowered = b -> b.getHandle().getBlock().isBlockPowered()||b.getHandle().getBlock().isBlockIndirectlyPowered();
	@EventHandler
	public void onRedstone(BlockRedstoneEvent evt) {
		PortalStick.getInstance().getBridgeManager().powerBlock(evt.getBlock(),evt.getNewCurrent() > 0);
		Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(),() -> {
			Grill.SubGrill g = Util.retrieveMetadata(evt.getBlock(), 3, Grill.SubGrill.class);
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
			if (g != null && evt.getNewCurrent() > 0 && g.getGrill().getBorder().stream().anyMatch(isPowered)) {
				g.getGrill().close();
				offGrills.add(g.getGrill());
			}
		}, 1L);
		AutomatedPortal portal = Util.retrieveMetadata(evt.getBlock(), 4, AutomatedPortal.class);
		if (portal != null) {
			Block wool = portal.getColor();
			Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), () -> {
				if (FaceUtil.isVertical(portal.getFacing())) {
					BlockFace face = BlockFace.WEST;
					if (wool.getRelative(face).isBlockPowered() || wool.getRelative(face).isBlockIndirectlyPowered()) {
						portal.open();
					}
					face = BlockFace.NORTH;
					if (wool.getRelative(face).isBlockPowered() || wool.getRelative(face).isBlockIndirectlyPowered()) {
						portal.open();
					}
					face = BlockFace.EAST;
					if (wool.getRelative(face).isBlockPowered() || wool.getRelative(face).isBlockIndirectlyPowered()) {
						portal.close();
					}
					face = BlockFace.SOUTH;
					if (wool.getRelative(face).isBlockPowered() || wool.getRelative(face).isBlockIndirectlyPowered()) {
						portal.close();
					}
					return;
				}
				BlockFace face = FaceUtil.rotate(portal.getFacing(), 2);
				if (wool.getRelative(face).isBlockPowered() || wool.getRelative(face).isBlockIndirectlyPowered()) {
					portal.open();
				}
				face = FaceUtil.rotate(portal.getFacing(), -2);
				if (wool.getRelative(face).isBlockPowered() || wool.getRelative(face).isBlockIndirectlyPowered()) {
					portal.close();
				}
			}, 1L);
		}
		// PortalStick signs
		BlockUtil.getNearbyBlocks(evt.getBlock()
				.getLocation(), 1).stream().filter(blk -> blk.getType() == Material.WALL_SIGN).forEach(blk -> {
			Sign s = (Sign) blk.getState();
			if (s.getLine(0).equalsIgnoreCase("[PortalStick]")) {
				Cube.CubeType cubeType = Cube.CubeType.fromSign(s.getLine(1));
				//  Cube sign
				if (cubeType != null) {
					Block attachedBlock = blk.getRelative(((org.bukkit.material.Sign) s
							.getData()).getAttachedFace());

					final V10Block hatchMiddleLoc = new V10Block(attachedBlock.getRelative(
							BlockFace.DOWN, 2));
					final V10Block loc2 = new V10Block(blk);

					Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), () -> {
						Block blk1 = loc2.getHandle().getBlock();
						if (blk1.isBlockPowered()
								|| blk1.isBlockIndirectlyPowered()) {
							Block next = blk1.getRelative(((org.bukkit.material.Sign) blk1
									.getState().getData()).getFacing());
							CubeManager.spawnCube(hatchMiddleLoc.getHandle().getBlock(), next.isBlockPowered() || next.isBlockIndirectlyPowered(), cubeType, blk, true);
						}
					}, 1L);
				}
			}

		});
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
			if(GelType.fromDispenser(is) != null)
			{
				event.setCancelled(true);
				Block to = bs.getBlock();
				V10Block from = new V10Block(to);
				plugin.getGelManager().createTube(from, ((DirectionalContainer) bs.getData()).getFacing(), GelType.fromDispenser(is));
				plugin.getConfiguration().saveAll();
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
        location = location.add(0.5, 0.2, 0.5)
                .add(FaceUtil.faceToVector(FaceUtil.rotate(facing,-2),0.3))
                .add(FaceUtil.faceToVector(facing.getOppositeFace(),0.2));
        if(location.getWorld().getNearbyEntities(location,1,1,1).stream().anyMatch(en -> en.getCustomName().equals("portalstand"))) return;
		location.setDirection(FaceUtil.faceToVector(facing));
		ArmorStand as = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
		NMSArmorStand.wrap(as).lock();
		NMSArmorStand.wrap(as).setWillSave(false);
		as.setRightArmPose(new EulerAngle(Math.toRadians(0),0,Math.toRadians(-5)));
		as.setItemInHand(new ItemStack(Material.STICK));
		as.setCustomName("portalstand");
		as.setGravity(false);
		as.setVisible(false);
	}
}
