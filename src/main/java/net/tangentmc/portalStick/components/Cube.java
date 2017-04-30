package net.tangentmc.portalStick.components;

import lombok.Getter;
import net.tangentmc.nmsUtils.NMSUtils;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.entities.NMSEntity;
import net.tangentmc.nmsUtils.resourcepacks.ResourcePackAPI;
import net.tangentmc.nmsUtils.utils.MetadataSaver;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.GelType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;

@Getter
@MetadataSaver.Metadata(metadataName = "cuben")
public class Cube extends MetadataSaver {
    public enum CubeType {
        COMPANION("ccube"),
        NORMAL("cube"),
        LASER("lcube");
        private HashMap<GelType,ItemStack> cubeMap = new HashMap<>();
        CubeType(String name) {
            this.name = name;
        }
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
        private String getResource() {
            return "cube/"+name().toLowerCase();
        }
        public ItemStack getCube(GelType type) {
            return cubeMap.getOrDefault(type,cubeMap.get(null));
        }
        static {
            ResourcePackAPI a = NMSUtils.getInstance().getResourcePackAPI();
            for (CubeType type : CubeType.values()) {
                type.cubeMap.put(null,a.getItemStack(type.getResource()));
                type.cubeMap.put(GelType.JUMP,a.getItemStack(type.getResource()+"_jump"));
                type.cubeMap.put(GelType.SPEED,a.getItemStack(type.getResource()+"_speed"));
            }
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
        initMetadata(as);
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
 