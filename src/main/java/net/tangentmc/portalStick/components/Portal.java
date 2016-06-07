package net.tangentmc.portalStick.components;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import net.tangentmc.nmsUtils.NMSUtil;
import net.tangentmc.nmsUtils.entities.NMSEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.tangentmc.nmsUtils.NMSUtils;
import net.tangentmc.nmsUtils.entities.HologramFactory;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.entities.NMSHologram;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.MetadataSaver.Metadata;
import net.tangentmc.portalStick.util.math.Quaternion;
import net.tangentmc.portalStick.utils.BlockStorage;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.Util;
import net.tangentmc.portalStick.utils.VectorUtil;
@Getter
@NoArgsConstructor
@Metadata(metadataName = "portalobj")
public class Portal implements MetadataSaver{
    private Vector facing;
    private String owner;
    private NMSHologram back;
    private ArmorStand portal;
    boolean finished = false;
    boolean open = false;
    Block bottom = null;
    Block top = null;
    BlockStorage bottomStorage;
    BlockStorage topStorage;
    private ArrayList<UUID> disabledFor = new ArrayList<>();
    boolean primary;
    public ArrayList<Bridge> intersects = new ArrayList<>();
    public Portal(String owner, Vector facing, Block clicked, boolean primary) {
        this.facing = facing;
        this.primary = primary;
        this.owner = owner;
        if (facing.getX() == 0 && facing.getBlockZ() == 0) {
            bottom = clicked;
            PortalStick.getInstance().getPortals().put(new V10Block(bottom), this);
            finished = true;
            return;
        }
        bottom = clicked.getRelative(BlockFace.DOWN);
        top = clicked;
        Material type = bottom.getRelative(FaceUtil.getDirection(facing)).getType();
        if (type.isSolid() || !type.isTransparent()) {
            bottom = clicked;
            top = clicked.getRelative(BlockFace.UP);
        }
        type = top.getRelative(FaceUtil.getDirection(facing)).getType();
        if (type.isSolid() || !type.isTransparent()) {
            return;
        }
        if (!top.getType().isSolid()) return;
        if (!bottom.getType().isSolid()) return;
        PortalStick.getInstance().getPortals().put(new V10Block(bottom), this);
        PortalStick.getInstance().getPortals().put(new V10Block(top), this);
        finished = true;
    }
    public Portal getDestination() {
        PortalUser user = PortalStick.getInstance().getUser(owner);
        Portal p = primary?user.getSecondary():user.getPrimary();
        if (p == null) {
            Region r = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(bottom));
            return primary?r.getSecondary():r.getPrimary();
        }
        if (p.isOpen())
            return p;
        return null;
    }
    public boolean isValid() {
        PortalUser user = PortalStick.getInstance().getUser(owner);
        return primary?user.getPrimary()==this:user.getSecondary()==this;
    }
    public void spawnPortalStand() {
        if (portal == null)
            portal = (ArmorStand) bottom.getWorld().spawnEntity((top!=null?top:bottom).getLocation().add(facing).add(0.5, top!=null?-1.5:-0.7, 0.5).setDirection(new Vector(0,0,0).subtract(facing)), EntityType.ARMOR_STAND);
        NMSArmorStand wrapped = NMSArmorStand.wrap(portal);
        wrapped.lock();
        portal.setGravity(false);
        portal.setRemoveWhenFarAway(false);
        portal.setVisible(false);
        wrapped.setWillSave(false);
        if (facing.getZ() == 0 && facing.getX() == 0) {
            portal.setHeadPose(new EulerAngle(Math.toRadians(180), 0, 0));
        }
        else
            portal.setHeadPose(new EulerAngle(Math.toRadians(90),0,0));
        wrapped.setCollides(true);
    }
    public boolean open() {
        if (!finished) return false;
        spawnPortalStand();

        if (getDestination() == null) {
            portal.setHelmet(new ItemStack(Material.STAINED_GLASS,1,(short) (primary?3:1)));
            portal.setMetadata("portalobj2", new FixedMetadataValue(PortalStick.getInstance(),this));
            open = true;
            if (this.back != null) {
                back.remove();
            }
            if (topStorage != null) {
                topStorage.set();
            }
            if (bottomStorage != null) {
                bottomStorage.set();
            }
            return true;
        }

        portal.setHelmet(new ItemStack(Material.WOOL,1,(short) (primary?3:1)));
        if (top == null) {
            portal.setMetadata("portalobj", new FixedMetadataValue(PortalStick.getInstance(),this));
        }
        //Used for checking if its a portal (funnel and removing)
        portal.setMetadata("portalobj2", new FixedMetadataValue(PortalStick.getInstance(),this));
        //Dont recreate these if the portal has been opened already, and is just being updated.
        if (bottomStorage == null) {
            bottomStorage = new BlockStorage(bottom);
            if (top != null) {
                topStorage = new BlockStorage(top);
            }
        } else {
            if (topStorage != null) {
                topStorage.set();
            }
            bottomStorage.set();
        }
        //Back is null if we go from disconnected -> connected
        if (back == null) {
            if (top != null) {
                back = new HologramFactory().withLocation(top.getLocation().add(0.5, 0.5, 0.5)).withHead(new ItemStack(Material.AIR), 1).withHead(new ItemStack(Material.AIR), 1).build();
            } else {
                back = new HologramFactory().withLocation(bottom.getLocation().add(0.5, 0.5, 0.5)).withHead(bottomStorage.toStack(), 1).build();
                bottom.setType(Material.AIR);
            }
            back.getEntity().setMetadata(getMetadataName(), new FixedMetadataValue(PortalStick.getInstance(), this));
        }

        open = true;
        return true;
    }
    /*TODO: instead of using this for entities, it should be easy enough
      to test their velocities and check they are going to get into the portal
    */
    public void openFor(Entity en) {
        if (top == null) return;
        final Player pl = en instanceof Player?(Player) en:null;
        setTemporary(pl,bottomStorage.toStack(), back.getLines().get(0),true);
        setTemporary(pl,topStorage.toStack(), back.getLines().get(1),true);
        checkPortal(en);
    }
    private void checkPortal(final Entity en) {
        final Player pl = en instanceof Player?(Player) en:null;
        Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(),()->{
            if (portal.getNearbyEntities(1,1,1).contains(pl)) {
                checkPortal(en);
            } else {
                setTemporary(pl,bottomStorage.toStack(), back.getLines().get(0),false);
                setTemporary(pl,topStorage.toStack(), back.getLines().get(1),false);
                bottomStorage.set();
                topStorage.set();
            }
        },1L);
    }

    /**
     *
     * @param pl
     * @param head
     * @param as
     * @param toggleEntity true to apply head to the armorstand, false to apply it to the block
     */
    private void setTemporary(Player pl, ItemStack head, Entity as, boolean toggleEntity) {

        try {
            PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            pc.getItemSlots().write(0, EnumWrappers.ItemSlot.HEAD);
            pc.getItemModifier().write(0,toggleEntity?head:new ItemStack(Material.AIR));
            pc.getIntegers().write(0,as.getEntityId());
            if (pl != null) {
                ProtocolLibrary.getProtocolManager().sendServerPacket(pl, pc.deepClone());
            }
            pc.getItemModifier().write(0,toggleEntity?new ItemStack(Material.AIR):head);
            Block b = as.getLocation().getBlock().getRelative(BlockFace.UP);
            if (toggleEntity) {
                b.setType(Material.AIR);
            }
            for (Entity entity : as.getNearbyEntities(16, 16, 16)) {
                if (entity instanceof Player && pl != entity) {
                    ProtocolLibrary.getProtocolManager().sendServerPacket((Player) entity, pc);
                }
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    public void close() {
        if (!this.open) return;
        open = false;
        if (PortalStick.getInstance().isEnabled()) {
            //Update bridges a tick later so that all portals are placed.
            Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), () -> {
                for (Bridge b : PortalStick.getInstance().getBridgeManager().getBridges()) {
                    BlockFace df = FaceUtil.getDirection(facing);
                    if (b.intersectsWith(new V10Block(bottom.getRelative(df))) || (top != null && b.intersectsWith(new V10Block(top.getRelative(df))))) {
                        b.update();
                    }
                    if (getDestination() != null) {
                        df = FaceUtil.getDirection(getDestination().facing);
                        if (b.intersectsWith(new V10Block(getDestination().bottom.getRelative(df)))
                                || (getDestination().top != null && b.intersectsWith(new V10Block(getDestination().top.getRelative(df))))) {
                            b.update();
                        }
                    }
                }
            }, 2l);
        }
        if (topStorage != null) {
            topStorage.set();
        }
        if (bottomStorage != null)
            bottomStorage.set();
        if (this.back != null) {
            back.remove();
        }
        portal.remove();
        bottomStorage = null;
        portal= null;
    }
    Random r = new Random();
    public void teleportEntity(Entity entity, Vector motion) {
        if (this.disabledFor.contains(entity.getUniqueId())) return;
        if (!(entity instanceof Player) && !(entity instanceof Item))
            motion = entity.getVelocity();
        else {
            motion = entity.getVelocity();
            motion = motion.setY(entity.getVelocity().getY());
        }
        if(getDestination() != null && getDestination().disabledFor.contains(entity.getUniqueId())) {
            return;
        }
        Location eloc = entity.getLocation();
        //Special case for entities in a funnel - the y coords aren't calculated correctly otherwise
        Optional<Entity> ent = entity.getNearbyEntities(1, 1, 1).stream().filter(en -> Util.checkInstance(Funnel.class, en)).findAny();
        if (ent.isPresent() && !(entity instanceof Player)) {
            Location b2 = getDestination().getBottom().getLocation();
            if (getTop() != null && ent.get().getLocation().getY() != getBottom().getY() && getDestination().getTop() != null) {
                b2 = getDestination().getTop().getLocation();
            }
            b2 = b2.add(0.5, -0.2, 0.5).add(getDestination().facing);
            TeleportLoc loc = teleportEntity(eloc,motion);
            if (loc == null) return;
            getDestination().disabledFor.add(entity.getUniqueId());
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(PortalStick.getInstance(), () -> getDestination().disabledFor.remove(entity.getUniqueId()), 10L);
            NMSUtils.getInstance().getUtil().teleportFast(entity, b2.setDirection(loc.destination.getDirection()),loc.getVelocity());
            return;
        }
        TeleportLoc loc = teleportEntity(eloc,motion);
        if (loc == null) return;
        if (!(entity instanceof Player)) {
            NMSUtils.getInstance().getUtil().teleportFast(entity, loc.destination,loc.getVelocity());
        } else {
            entity.teleport(loc.destination);
        }
        //Adjust velocity for gel.
        if (!FaceUtil.isVertical(FaceUtil.getDirection(loc.velocity))&&Util.checkInstance(GelTube.class, entity)) {
            loc.velocity = loc.velocity.multiply(r.nextInt(20)/10d);
        }
        entity.setVelocity(loc.velocity);
        Util.playSound(primary?Sound.PORTAL_EXIT_BLUE:Sound.PORTAL_EXIT_ORANGE, new V10Block(loc.destination));
        getDestination().disabledFor.add(entity.getUniqueId());
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(PortalStick.getInstance(), () -> getDestination().disabledFor.remove(entity.getUniqueId()), 10L);

    }
    @AllArgsConstructor
    @Getter
    public class TeleportLoc {
        Location destination;
        Vector velocity;
    }
    public TeleportLoc teleportEntity(Location loc, Vector motion) {
        Portal destination = getDestination();
        if (destination == null) return null;
        Block block = destination.bottom;
        if (top != null && loc.getY() > bottom.getY()) {
            block = destination.bottom;
        }
        Material mat = block.getType();
        //TODO: These validation checks are bad since there is no gurantee that the block will be air now
        //though, if they failed before the block was just set to air so we might be fine.
        boolean valid = mat == Material.AIR;
        if(!valid) {
            valid = !mat.isSolid();
        }
        if(!valid) {
            valid = block.isLiquid();
        }
        Vector v = this.facing;
        Quaternion q = VectorUtil.getRotationTo(v, new Vector(0,0,0).subtract(destination.facing));

        Vector outvector = VectorUtil.rotate(q.cpy(), motion);
        Vector facing = VectorUtil.rotate(q.cpy(), loc.getDirection());
        double x = Math.abs(destination.facing.getX())*0.5;
        double z = Math.abs(destination.facing.getZ())*0.5;
        if (destination.facing.getX() < 0) z-=0.5;
        if (destination.facing.getZ() < 0) x-=0.5;
        if (destination.facing.getX() > 0) z+=1.5;
        if (destination.facing.getZ() > 0) x+=1.5;
        if (destination.facing.getX() == 0 && destination.facing.getZ() == 0)
            if (outvector.getY() < 0.5) outvector.setY(0.5);

        Location teleport = block.getLocation().add(z, 0, x).setDirection(facing);
        return new TeleportLoc(teleport,outvector);
    }
    public void delete() {
        close();
        if (portal != null)
            portal.remove();
        PortalStick.getInstance().getPortals().remove(new V10Block(bottom));
        if (top != null)
            PortalStick.getInstance().getPortals().remove(new V10Block(top));
    }
    public boolean isRegionPortal() {
        return this.owner.startsWith("§region§_");
    }
    @Override
    public String getMetadataName() {
        return "portalobj";
    }
    public boolean isVertical() {
        return facing.getX() == 0 && facing.getZ() == 0;
    }

}

