package net.tangentmc.portalStick.components;

import lombok.Getter;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.MetadataSaver;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

@Getter
@MetadataSaver.Metadata(metadataName = "autoportalobj")
public class AutomatedPortal extends MetadataSaver {
	BlockFace facing;
	ArmorStand autoStand;
	Block loc;
	public AutomatedPortal(Block b, BlockFace clicked, Player pl) {
		if (!FaceUtil.isVertical(clicked))
			this.loc = b.getRelative(BlockFace.DOWN);
		else
			this.loc = b;
		facing = FaceUtil.rotate(clicked, 2);
		Location loc = b.getLocation().add(0.5, -1, FaceUtil.isVertical(clicked)?0:0.5).add(FaceUtil.faceToVector(clicked).multiply(0.8)).setDirection(FaceUtil.faceToVector(facing));
		if (FaceUtil.isVertical(clicked)) {
			loc.setDirection(pl.getLocation().getDirection().setY(0));
		}
		autoStand = (ArmorStand) b.getWorld().spawnEntity(b.getLocation(),EntityType.ARMOR_STAND);
		facing = clicked;
		if (facing == BlockFace.UP) {
			autoStand.setHeadPose(new EulerAngle(Math.toRadians(90),0,Math.toRadians(-90)));
		} else if (facing == BlockFace.DOWN) {
			autoStand.setHeadPose(new EulerAngle(Math.toRadians(-90),0,Math.toRadians(-90)));
		}
		NMSArmorStand.wrap(autoStand).lock();
		autoStand.setVisible(false);
		autoStand.setGravity(false);
		autoStand.setHelmet(new ItemStack(Material.NETHER_FENCE));
		autoStand.setRemoveWhenFarAway(false);
		initMetadata(autoStand);
	}
	public AutomatedPortal(Entity en) {
		this.autoStand = (ArmorStand) en;
		this.facing = FaceUtil.rotate(FaceUtil.getDirection(en.getLocation().getDirection()), -2);
		loc = autoStand.getLocation().subtract(0.5, 0, 0.5).subtract(FaceUtil.faceToVector(facing).multiply(0.8)).getBlock();
		initMetadata(autoStand);
	}
	public Block getColor() {
		return loc.getRelative(facing.getOppositeFace(),FaceUtil.isVertical(facing)?2:1);
	}
	public void open() {
		Region r = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(loc));
		if (getColor().getData() == (byte)1) {
			//orange
			r.setSecondary(loc,FaceUtil.faceToVector(facing),autoStand);
			if (r.getPrimary() != null) r.getPrimary().open();
		}
		if (getColor().getData() == (byte)3) {
			r.setPrimary(loc,FaceUtil.faceToVector(facing),autoStand);
			if (r.getSecondary() != null) r.getSecondary().open();
		}
	}
	public void close() {
		Region r = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(loc));
		if (getColor().getData() == (byte)1) {
			if (r.getSecondary() != null) {
				if (r.getSecondary().getDestination() != null) r.getSecondary().getDestination().close();
				r.getSecondary().delete();
			}
			r.setSecondary(null);
		}
		if (getColor().getData() == (byte)3) {
			if (r.getPrimary() != null) {
				if (r.getPrimary().getDestination() != null) r.getPrimary().getDestination().close();
				r.getPrimary().delete();
			}
			r.setPrimary(null);
		}
	}

}
