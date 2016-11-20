package net.tangentmc.portalStick.components;

import net.tangentmc.nmsUtils.utils.BlockUtil;
import net.tangentmc.portalStick.utils.GelType;
import net.tangentmc.portalStick.utils.MetadataSaver;
import net.tangentmc.portalStick.utils.RegionSetting;
import net.tangentmc.portalStick.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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
        COMPANION(57,58,59,"ccube"),
        NORMAL(54,55,56,"cube"),
        LASER(60,61,62,"lcube");
        int normal;
        int speed;
        int jump;
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
        public static CubeType fromId(int id) {
            for (CubeType c : values()) {
                if (c.speed == id || c.normal == id|| c.jump == id) return c;
            }

            return null;
        }
        public ItemStack getCube(GelType type) {
            if (type == null) {
                return Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE,1,(short)normal));
            }
            switch (type) {
                case SPEED:
                    return Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE,1,(short)speed));
                case JUMP:
                    return Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE,1,(short)jump));
            }
            return Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE,1,(short)normal));
        }
    }
    GelType gelType;
    CubeType type;
    Player holder;
    ArmorStand as;
    V10Block spawner;

    public Cube(CubeType type, Location loc, V10Block spawner) {
        this.spawner = spawner;
        this.type = type;
        as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setHelmet(type.getCube(null));
        NMSEntity.wrap(as).setWillSave(false);
        NMSEntity.wrap(as).setCollides(true);
        as.setSmall(true);
        as.setVisible(false);
        as.setAI(false);
        as.setSilent(true);
        NMSArmorStand.wrap(as).lock();
        as.setMetadata("cuben", new FixedMetadataValue(PortalStick.getInstance(),this));
    }
    public void setGelType(GelType type) {
        if (type == GelType.CONVERSION) return;
        if (type == GelType.WATER) type = null;
        this.gelType = type;
        as.setHelmet(this.type.getCube(type));
    }
    BukkitTask holdTask;
    public void hold(Player en) {
        if (holdTask != null) {
            holdTask.cancel();
            holdTask = null;
            boolean treturn = en == holder;
            PortalStick.getInstance().getUser(holder.getName()).setCube(null);
            holder = null;
            if (treturn) {
                as.setVelocity(en.getVelocity());
                as.setGravity(true);
                return;
            }
        }
        this.holder = en;
        PortalStick.getInstance().getUser(holder.getName()).setCube(this);
        as.setGravity(false);
        holdTask = Bukkit.getScheduler().runTaskTimer(PortalStick.getInstance(), ()->{

            Location to = en.getLocation().add(0, 1, 0).add(holder.getLocation().getDirection());
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
 