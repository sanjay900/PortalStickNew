package net.tangentmc.portalStick.components;

import net.tangentmc.nmsUtils.utils.BlockUtil;
import net.tangentmc.portalStick.utils.MetadataSaver;
import net.tangentmc.portalStick.utils.RegionSetting;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.entities.NMSEntity;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.MetadataSaver.Metadata;

@Getter
@NoArgsConstructor
@Metadata(metadataName = "cuben")
public class Cube implements MetadataSaver {
	@AllArgsConstructor
	public enum CubeType {
		COMPANION(RegionSetting.COMPANION_CUBE_BLOCK,"ccube"),
		NORMAL(RegionSetting.CUBE_BLOCK,"cube"),
		LASER(RegionSetting.LASER_CUBE_BLOCK,"lcube");
        RegionSetting setting;
		String name;
		public static CubeType fromSign(String signText) {
			try {
				return CubeType.valueOf(signText.toUpperCase());
			} catch (Exception ex) {
				for (CubeType c : values()) {
					if (c.name.equalsIgnoreCase(signText)) return c;
				}
			}
			return null;
		}
        public ItemStack getCube(V10Block cubeLocation) {
            String bstr = PortalStick.getInstance().getRegionManager().getRegion(cubeLocation).getString(setting);
            return BlockUtil.getStackFromString(bstr);
        }
	}
	CubeType type;
	Entity holder;
	ArmorStand as;
	V10Block spawner;

	public Cube(CubeType type, Location loc, V10Block spawner) {
		this.spawner = spawner;
		this.type = type;
		as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		as.setHelmet(type.getCube(new V10Block(loc)));
		NMSEntity.wrap(as).setWillSave(false);
        NMSEntity.wrap(as).setCollides(true);
		as.setSmall(true);
		as.setVisible(false);
		NMSArmorStand.wrap(as).lock();
		as.setMetadata("cuben", new FixedMetadataValue(PortalStick.getInstance(),this));
	}
	BukkitTask holdTask;
	public void hold(Entity en) {
		if (holdTask != null) {
			holdTask.cancel();
            holdTask = null;
			boolean treturn = en == holder;
			PortalStick.getInstance().getUser(holder.getName()).setCube(null);
			holder = null;
			as.setGravity(true);
			if (treturn) return;
		}
		this.holder = en;
		PortalStick.getInstance().getUser(holder.getName()).setCube(this);
		as.setGravity(false);
		//TODO Should we use packets to spam this faster than the server clock?
		holdTask = Bukkit.getScheduler().runTaskTimer(PortalStick.getInstance(), ()->{
			Location to = holder.getLocation().add(0, 1, 0).add(holder.getLocation().getDirection());
			as.teleport(to);
		}, 1L, 1L);
	}
	public void remove() {
		as.remove();
	}
	public void respawn() {
		if (holdTask != null) holdTask.cancel();
		as.setGravity(true);
		as.teleport(spawner.getHandle());
	}
	
}
 