package net.tangentmc.portalStick.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.AutomatedPortal;
import net.tangentmc.portalStick.components.Region;
import net.tangentmc.portalStick.components.Wire;
import net.tangentmc.portalStick.components.Wire.PoweredReason;
import net.tangentmc.portalStick.components.Wire.WireType;
import net.tangentmc.portalStick.utils.Util;

public class WireManager {
	private final PortalStick plugin = PortalStick.getInstance();
	public final HashMap<V10Block,ArrayList<Wire>> wiresupport = new HashMap<>();
	public final HashMap<V10Block,ArrayList<Wire>> wireloc = new HashMap<>();
	public void loadAllWire() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> wiresupport.values().forEach(w2 -> w2.forEach(Wire::update)), 1l, 1l);
		Bukkit.getWorlds().forEach(this::loadWorld);
		wiresupport.values().forEach(w2 -> w2.forEach(Wire::orient));
	}
	public void loadChunk(Chunk w) {
		for (Entity en: w.getEntities()) {
			if (en instanceof ArmorStand) {
				if (en.getCustomName() != null && (en.getCustomName().startsWith("wiret")||en.getCustomName().equals("wire"))) {
					Wire w2 = new Wire((ArmorStand) en,plugin);
					V10Block blk = new V10Block(w2.getSupport());
					if (!wiresupport.containsKey(blk)) wiresupport.put(blk, new ArrayList<Wire>());
					if (!wireloc.containsKey(w2.loc)) wireloc.put(w2.loc, new ArrayList<Wire>());
					wiresupport.get(blk).add(w2);
					wireloc.get(w2.loc).add(w2);
				}
				if (en.getCustomName() != null && en.getCustomName().equals(new AutomatedPortal().getMetadataName())) {
					System.out.print(en);
					new AutomatedPortal((ArmorStand) en);
				}
			}
		}
	}
	public void loadWorld(World w) {
		for (Chunk c : w.getLoadedChunks()) {
			this.loadChunk(c);
		}
	}
	public void unloadWorld(World w) {
		Iterator<Entry<V10Block, ArrayList<Wire>>> it = wiresupport.entrySet().iterator();
		while (it.hasNext()) {
			Entry<V10Block, ArrayList<Wire>> e = it.next();
			if (e.getKey().getWorldName().equals(w.getName())) {
				it.remove();
			}
		}
	}
	public void createWire(Block block, BlockFace facing) {
		createWire(block,facing,WireType.NORMAL);
	}
	public void createSign(Block block, BlockFace facing) {
		createWire(block,facing,WireType.INDICATOR);
	}
	private void createWire(Block block, BlockFace facing,WireType type) {
		V10Block blk = new V10Block(block);
		Wire w = new Wire(block,facing,type,plugin);

		if (wiresupport.containsKey(blk) && wireloc.containsKey(w.loc) && wiresupport.get(blk).stream().anyMatch(e -> wireloc.get(w.loc).contains(e))) {
			w.remove();
			return;
		}
		if (!wiresupport.containsKey(blk)) wiresupport.put(blk, new ArrayList<Wire>());
		if (!wireloc.containsKey(w.loc)) wireloc.put(w.loc, new ArrayList<Wire>());
		wiresupport.get(blk).add(w);
		wireloc.get(w.loc).add(w);
		w.orient();
		w.updateNearby();
	}
	public List<Wire> getNearbyWire(V10Block loc) {
		List<Wire> wire = new ArrayList<Wire>();
		for (Wire w: getInWorld(loc)) {
			if (w.loc.getHandle().distance(loc.getHandle())<2) {
				wire.add(w);
			}
		}
		return wire;
	}
	public List<Wire> getInWorld(V10Block loc) {
		List<Wire> wire = new ArrayList<Wire>();
		for (Entry<V10Block, ArrayList<Wire>> w: wiresupport.entrySet()) {
			if (w.getKey().getHandle().getWorld()==loc.getHandle().getWorld()) {
				wire.addAll(w.getValue());
			}
		}
		return wire;
	}

	public List<Wire> getSupported(Block blk) {
		V10Block vblk = new V10Block(blk);
		if (wiresupport.containsKey(vblk)) {
			return wiresupport.get(vblk);
		}
		return null;
	}
	public boolean comp (Block b, Block b2) {
		return Utils.compareLocation(b.getLocation(), b2.getLocation());
	}
	public Wire getWire(V10Block loc, V10Block support) {
		for (Wire w: getInWorld(loc)) {
			if (comp(loc.getHandle().getBlock(),w.loc.getHandle().getBlock()) && comp(support.getHandle().getBlock(),w.getSupport())) {
				return w;
			}
		}
		return null;
	}
	public List<Wire> getInRegion(V10Block loc) {
		List<Wire> wire = new ArrayList<Wire>();
		Region r = plugin.getRegionManager().getRegion(loc);
		for (Entry<V10Block, ArrayList<Wire>> w: wiresupport.entrySet()) {
			if (r == plugin.getRegionManager().getRegion(w.getKey())) {
				wire.addAll(w.getValue());
			}
		}
		return wire;
	}

	public void clearWire(Block blk) {
		V10Block vblk = new V10Block(blk);
		if (wiresupport.containsKey(vblk)) {
			wiresupport.get(vblk).stream().collect(Collectors.toList()).stream().forEach(w ->w.remove());
		}
	}
	public Wire getWire(Entity entity) {
		return Util.getInstance(Wire.class, entity);
	}
	BlockFace[] faces = new BlockFace[]{BlockFace.NORTH,BlockFace.SOUTH,BlockFace.UP,BlockFace.DOWN,BlockFace.EAST,BlockFace.WEST};
	public HashMap<Wire, BlockFace> getNearbyWire(Wire wire) {
		HashMap<Wire,BlockFace> nearby = new HashMap<>();
		V10Block support = new V10Block(wire.getSupport());
		for (Wire w : wiresupport.get(support)) {
			if (w == wire) continue;
			Vector v = wire.getSupport().getLocation().toVector().subtract(w.loc.getHandle().toVector());
			BlockFace face =FaceUtil.getDirection(v); 
			if (w.facing == wire.facing.getOppositeFace()) continue;
			if (nearby.values().contains(face.getOppositeFace())) continue;
			nearby.put(w, face.getOppositeFace());
		}
		for (Wire w : wireloc.get(wire.loc)) {
			if (w == wire) continue;
			Vector v = w.getSupport().getLocation().toVector().subtract(wire.loc.getHandle().toVector());
			BlockFace face =FaceUtil.getDirection(v); 
			if (w.facing == wire.facing.getOppositeFace()) continue;
			if (nearby.values().contains(face)) continue;
			nearby.put(w, face);
		}

		for (BlockFace face:faces) {
			V10Block support2 = wire.loc.getRelative(face);
			if (wireloc.containsKey(support2)) {
				for (Wire w : wireloc.get(support2)){
					if (w.facing != wire.facing) continue;
					if (nearby.values().contains(face)) continue;
					nearby.put(w, face);
				}
			}
		}
		return nearby;
	}
	public void powerBlock(Block block, boolean power, PoweredReason reason) {
		if (this.wiresupport.containsKey(new V10Block(block))) {
			Iterator<Wire> it = this.wiresupport.get(new V10Block(block)).stream().iterator();
			while (it.hasNext()) {
				it.next().setPowered(power, reason);
			}
		}
		if (this.wireloc.containsKey(new V10Block(block))) {
			Iterator<Wire> it = this.wireloc.get(new V10Block(block)).stream().iterator();
			while (it.hasNext()) {
				it.next().setPowered(power, reason);
			}
		}
	}
	public void blockBreak(Block block) {
		if (this.wiresupport.containsKey(new V10Block(block))) {
			Iterator<Wire> it = this.wiresupport.get(new V10Block(block)).stream().iterator();
			while (it.hasNext()) {
				it.next().remove();
			}
		}
	}
}
