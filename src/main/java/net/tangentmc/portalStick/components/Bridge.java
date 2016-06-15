package net.tangentmc.portalStick.components;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockIterator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.BlockStorage;
import net.tangentmc.portalStick.utils.Util;

@NoArgsConstructor
//TODO: Boats have hit boxes. We should replace bridges with most of the code from funnel, and then use boats to make them solid
//TODO: Merge funnel and bridge
public class Bridge {
	BlockIterator it;
	@Getter
	Location start;
	HashMap<V10Block,BlockStorage> setBlocks = new HashMap<>();
	HashMap<V10Block,Portal> portals = new HashMap<>();
	ArrayList<Portal> collided = new ArrayList<>();
	@Getter
	boolean complete = false;
	@Getter
	boolean open = true;
	@Getter
	@Setter
	boolean reversed = false;
	@Getter
	Location init;
	public Bridge(Block clickedBlock) {
		BlockFace bf = Util.getGlassPaneDirection(clickedBlock,Material.STAINED_GLASS);
		if (bf == null) return;
		init = clickedBlock.getLocation();
		Location loc = clickedBlock.getRelative(bf).getLocation().add(0.5, 0, 0.5).setDirection(FaceUtil.faceToVector(bf));

		this.start = loc.clone();
		complete = true;
		update();
	}
	public void update() {
		if (!complete || !open) return;
		portals.clear();
		collided.clear();
		HashMap<V10Block,BlockStorage> tempSet = new HashMap<>();
		it = new BlockIterator(start.setDirection(FaceUtil.faceToVector(FaceUtil.getDirection(start.getDirection()))));
		for (Block b=it.next(); it.hasNext() && checkBlock(b); ) {
			BlockStorage s = new BlockStorage(b);
			if (setBlocks.containsKey(new V10Block(b))) {
				s = setBlocks.get(new V10Block(b));
			} else {
				setBlock(b);
			}
			tempSet.put(new V10Block(b), s);
			b=it.next();
		}
		//Remove all unchanged
		setBlocks.keySet().removeAll(tempSet.keySet());
		setBlocks.values().forEach(BlockStorage::set);
		setBlocks.clear();
		setBlocks.putAll(tempSet);
	}
	private void setBlock(Block b) {
		b.setType(Material.STAINED_GLASS);
		b.setData((byte)3);
	}
	private boolean checkBlock(Block b) {
		if (b.getType().isSolid() && !b.getType().equals(Material.STAINED_GLASS)) {
			return false;
		}	
		Grill g = Util.retrieveMetadata(b,1,Grill.class);
		if (g != null) return false;
		Portal p = Util.retrieveMetadata(b,1,Portal.class);
		if (p != null) {
			if (!p.isOpen() || p.getDestination() == null) return false;
			if (collided.contains(p)) return true;
			collided.add(p);
			collided.add(p.getDestination());
			Location b2 = p.getDestination().getBottom().getLocation();
			if (p.getTop() != null && b.getY() != p.getBottom().getY()) {
				b2 = p.getDestination().getTop().getLocation();
			}
			b2 = b2.getBlock().getLocation().add(p.getDestination().getFacing()).setDirection(p.getDestination().getFacing());
			it = new BlockIterator(b2);
			portals.put(new V10Block(b), p);
			return true;

		}
		return true;
	}
	public boolean intersectsWith(V10Block b) {
		return this.setBlocks.containsKey(b) || this.portals.containsKey(b);
	}
	public String getStringLocation()
	{
		return "Bridge,"+init.getWorld().getName() + "," + init.getBlockX() + "," + init.getBlockY() + "," + init.getBlockZ();
	}

	public boolean isNearby(V10Block block) {
		for (BlockFace bf: BlockFace.values()) {
			if (this.setBlocks.containsKey(block.getRelative(bf))||this.portals.containsKey(block.getRelative(bf))) {
				return true;
			}
		}
		return false;
	}
	public void remove() {
		//Stop updates from happening
		open = false;
		this.setBlocks.values().forEach(BlockStorage::set);
		this.setBlocks.clear();
	}
	public void toggleState(boolean powered) {
		if (powered) {
			this.remove();
		} else {
			open = true;
		}
	}

}
