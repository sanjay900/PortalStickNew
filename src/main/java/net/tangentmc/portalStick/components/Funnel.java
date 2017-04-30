package net.tangentmc.portalStick.components;

import java.util.HashMap;

import net.tangentmc.nmsUtils.utils.MetadataSaver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.Util;

@Getter
@NoArgsConstructor
public class Funnel extends Bridge {
	HashMap<V10Block,ArmorStand> setBlocks2;
	Vector currentdir;
	Vector newdir;
	public Funnel(Block clickedBlock) {
		super(clickedBlock);
		BlockFace bf = FaceUtil.getDirection(start.getDirection());
		if (isPowered(init.getBlock().getRelative(FaceUtil.rotate(bf, -2)))) {
			setReversed(true);
			update();
		}
		if (isPowered(init.getBlock().getRelative(FaceUtil.rotate(bf, 2)))) {
			toggleState(true);
			update();
		}
	}
	private boolean isPowered(Block b) {
		return b.isBlockPowered() || b.isBlockIndirectlyPowered();
	}
	@Override
	public void update() {
		if (!open || !complete) return;
		if (setBlocks2 == null) setBlocks2 = new HashMap<>();
		portals.clear();
		collided.clear();
		HashMap<V10Block,ArmorStand> tempSet = new HashMap<>();
		currentdir = newdir = FaceUtil.faceToVector(FaceUtil.getDirection(start.getDirection()));
		it = new BlockIterator(start.setDirection(FaceUtil.faceToVector(FaceUtil.getDirection(start.getDirection()))));
		for (Block b=it.next(); it.hasNext() && checkBlock(b); ) {
			if (setBlocks2.containsKey(new V10Block(b)))
				tempSet.put(new V10Block(b), setBlocks2.get(new V10Block(b)));
			else
				tempSet.put(new V10Block(b), setBlock(b));
			b=it.next();
			currentdir = newdir;
		}
		//Remove all unchanged
		setBlocks2.keySet().removeAll(tempSet.keySet());
		setBlocks2.values().forEach(ArmorStand::remove);
		setBlocks2.clear();
		setBlocks2.putAll(tempSet);
		updateAll();
	}
	private ArmorStand setBlock(Block b) {	
		ArmorStand holo = (ArmorStand) b.getWorld().spawnEntity(b.getLocation().add(0.5,-0.1,0.5).setDirection(currentdir),EntityType.ARMOR_STAND);
		new FunnelEn(holo);
		NMSArmorStand.wrap(holo).lock();
		NMSArmorStand.wrap(holo).setWillSave(false);
		holo.setHelmet(getItemStack());
		holo.setGravity(false);
		holo.setSmall(true);
		holo.setVisible(false);
		holo.setMarker(true);
		return holo;
	}
	private ItemStack getItemStack() {
		return new ItemStack((reversed?Material.RED_SANDSTONE:Material.STAINED_CLAY),1,(byte)(reversed?0:11));
	}
	//Set if reversed or not
	private void updateAll() {
		setBlocks2.values().forEach(holo -> holo.setHelmet(getItemStack()));
	}
	private boolean checkBlock(Block b) {
		if (b.getType().isSolid() && !b.getType().equals(Material.STAINED_GLASS) || !b.getChunk().isLoaded() || b.getY() < 0 || b.getY() >= b.getWorld().getMaxHeight()) {
			return false;
		}	
		Grill.SubGrill g = Util.retrieveMetadata(b,1,Grill.SubGrill.class);
		if (g != null) return false;
		Portal.PortalFrame pf = Util.retrieveMetadata(b,1,Portal.PortalFrame.class);
		if (pf != null) {
			Portal p = pf.getPortal();
			if (!p.isOpen() || p.getDestination() == null) return false;
			if (collided.contains(p)) return true;
			collided.add(p);
			collided.add(p.getDestination());
			Location b2 = p.getDestination().getBottom().getLocation();
			if (p.getTop() != null && b.getY() != p.getBottom().getY() && p.getDestination().getTop() != null) {
				b2 = p.getDestination().getTop().getLocation();
			}
			b2 = b2.getBlock().getLocation().setDirection(p.getDestination().getFacing());
			newdir = FaceUtil.faceToVector(FaceUtil.getDirection(p.getDestination().getFacing()));
			it = new BlockIterator(b2.add(p.getDestination().getFacing()));
			
			portals.put(new V10Block(b), p);
			return true;

		}
		return true;
	}
	@Override
	public boolean intersectsWith(V10Block b) {
		return this.setBlocks2.containsKey(b) || this.portals.containsKey(b);
	}
	@Override
	public String getStringLocation()
	{
		return "Funnel,"+init.getWorld().getName() + "," + init.getBlockX() + "," + init.getBlockY() + "," + init.getBlockZ();
	}


	@Override
	public boolean isNearby(V10Block block) {
		for (BlockFace bf: BlockFace.values()) {
			if (this.setBlocks2.containsKey(block.getRelative(bf))||this.portals.containsKey(block.getRelative(bf))) {
				return true;
			}
		}
		return false;
	}
	@Override
	public void remove() {
		open = false;
		setBlocks2.values().forEach(Entity::remove);
		Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), () -> {
			start.getWorld().getEntities().stream().filter(en -> en.hasMetadata("inFunnel") && en.getMetadata("inFunnel").get(0).value() == this).forEach(en -> {
				en.removeMetadata("inFunnel", PortalStick.getInstance());
				if (en instanceof Player) {
					((Player) en).setFlying(false);
				} else {
					en.setGravity(false);
				}
			});
		}, 1L);
		this.setBlocks2.clear();
	}

	@MetadataSaver.Metadata(metadataName = "funnelobj")
	public class FunnelEn extends MetadataSaver {
		@Getter
		private Funnel funnel = Funnel.this;
		protected FunnelEn(Entity en) {
			initMetadata(en);
		}
	}

}
