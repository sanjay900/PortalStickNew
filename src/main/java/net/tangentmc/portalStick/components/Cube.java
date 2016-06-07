package net.tangentmc.portalStick.components;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.tangentmc.nmsUtils.NMSUtils;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.entities.NMSEntity;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.MetadataSaver.Metadata;
@Getter
@NoArgsConstructor
@Metadata(metadataName = "cuben")
public class Cube implements MetadataSaver{
	@AllArgsConstructor
	public enum CubeType {
		COMPANION(new ItemStack(Material.STAINED_CLAY,1,(byte)0)),
		NORMAL(new ItemStack(Material.STAINED_CLAY,1,(byte)9)),
		LASER(new ItemStack(Material.STAINED_CLAY,1,(byte)3));
		ItemStack mt;
	}
	CubeType type;
	Entity holder;
	ArmorStand as;
	V10Block spawner;
	public Cube(CubeType type, Location loc, V10Block spawner) {
		this.spawner = spawner;
		this.type = type;
		as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		as.setHelmet(type.mt);
		NMSEntity.wrap(as).setWillSave(false);
		as.setSmall(true);
		as.setVisible(false);
		NMSArmorStand.wrap(as).lock();
		as.setMetadata("cuben", new FixedMetadataValue(PortalStick.getInstance(),this));
	}
	BukkitTask holdTask;
	public void hold(Entity en) {
		if (holdTask != null) {
			holdTask.cancel();
			boolean treturn = en == holder;
			PortalStick.getInstance().getUser(holder.getName()).setCube(null);
			holder = null;
			as.setGravity(true);
			if (treturn) return;
		}
		this.holder = en;
		PortalStick.getInstance().getUser(holder.getName()).setCube(this);
		as.setGravity(false);
		//TODO since boats allow you to mount objects away from you, could we did this with invis boats
		holdTask = Bukkit.getScheduler().runTaskTimer(PortalStick.getInstance(), ()->{
			/**
			Location to = en.getLocation().add(0, 1, 0).add(en.getLocation().getDirection());
			NMSUtils.getInstance().getUtil().teleportFast(as, to);
			 */
		},1l,1l);
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
 