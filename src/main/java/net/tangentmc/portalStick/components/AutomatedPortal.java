package net.tangentmc.portalStick.components;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.MetadataSaver.Metadata;

@NoArgsConstructor
@Getter
@Metadata(metadataName = "autoportalobj")
public class AutomatedPortal implements MetadataSaver{
	BlockFace facing;
	ArmorStand autoStand;
	Block loc;
	public AutomatedPortal(Block b, BlockFace clicked) {
		if (!FaceUtil.isVertical(clicked))
			this.loc = b.getRelative(BlockFace.DOWN);
		else
			this.loc = b;
		facing = FaceUtil.rotate(clicked, 2);
		autoStand = (ArmorStand) b.getWorld().spawnEntity(b.getLocation().add(0.5, -1, FaceUtil.isVertical(clicked)?0:0.5).add(FaceUtil.faceToVector(clicked).multiply(0.8)).setDirection(FaceUtil.faceToVector(facing)), EntityType.ARMOR_STAND);
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
		autoStand.setCustomName(getMetadataName());
		autoStand.setCustomNameVisible(false);
		autoStand.setMetadata(getMetadataName(), new FixedMetadataValue(PortalStick.getInstance(),this));
	}
	public AutomatedPortal(ArmorStand en) {
		this.autoStand = en;
		this.facing = FaceUtil.rotate(FaceUtil.getDirection(en.getLocation().getDirection()), -2);
		loc = autoStand.getLocation().subtract(0.5, 0, 0.5).subtract(FaceUtil.faceToVector(facing).multiply(0.8)).getBlock();
		autoStand.setMetadata(getMetadataName(), new FixedMetadataValue(PortalStick.getInstance(),this));
	}
	public Block getColor() {
		return loc.getRelative(facing.getOppositeFace(),FaceUtil.isVertical(facing)?2:1);
	}
	public void open() {
		Region r = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(loc));
		if (getColor().getData() == (byte)1) {
			//orange
			r.setSecondary(loc,FaceUtil.faceToVector(facing));
		}
		if (getColor().getData() == (byte)3) {
			r.setPrimary(loc,FaceUtil.faceToVector(facing));
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
