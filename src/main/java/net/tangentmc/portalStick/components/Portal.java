package net.tangentmc.portalStick.components;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.tangentmc.nmsUtils.NMSUtils;
import net.tangentmc.nmsUtils.entities.HologramFactory;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.entities.NMSHologram;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.MathUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.*;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.MetadataSaver.Metadata;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Stairs;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Getter
@NoArgsConstructor
@Metadata(metadataName = "portalobj")
//TODO: Code back in angled portals
public class Portal implements MetadataSaver {
    private Vector facing;
    private String owner;
    private NMSHologram back;
    public ArmorStand portal;
    boolean finished = false;
    boolean open = false;
    Block bottom = null;
    Block top = null;
    BlockStorage bottomStorage;
    BlockStorage topStorage;
    private ArrayList<UUID> disabledFor = new ArrayList<>();
    boolean primary;
    public ArrayList<Bridge> intersects = new ArrayList<>();
    Vector entDirection = null;
    private Quaterniond rotation;
    private Quaterniond rotationOut;
    public int roundToNearest(double original, int interval, int offset) {
        return (int) (Math.round( (original-offset)/interval ) * interval + offset);
    }
    public Portal(String owner, Vector facing, Block clicked, boolean primary, Vector entDirection) {
        this.facing = facing;
        this.primary = primary;
        this.owner = owner;
        if (clicked.getType()==Material.COBBLESTONE_STAIRS||clicked.getType()==Material.SMOOTH_STAIRS) {
            placeAngledPortal(clicked);
            return;
        }
        Region region = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(clicked));
        List<String> canPlace = region.getList(RegionSetting.PORTAL_BLOCKS);
        if (!Util.checkBlock(canPlace,clicked)) return;
        if (facing.getX() == 0 && facing.getZ() == 0) {
            bottom = clicked;
            this.entDirection = entDirection.normalize().setY(0);
            BlockFace face;
            double yaw = MathUtil.getLookAtYaw(this.entDirection)/45;
            //Work out what blockfaces contain the portal
            //This portal is close to a straight portal, so round it accordingly
            if (Math.abs(yaw-Math.round(yaw))<0.2) {
                face = FaceUtil.RADIAL[(int) (Math.round(yaw)) & 0x7];
            } else {
                //This portal is not. Round it in a way that it contains the most amount of blocks possible
                face = FaceUtil.RADIAL[roundToNearest(yaw, 2, 1) & 0x7];
            }
            //Work out what blocks the portal is sitting on
            int ux = bottom.getX()+face.getModX();
            int uz = bottom.getZ()+face.getModZ();
            //Loop and check
            boolean passed = true;
            for (int x = Math.min(bottom.getX(),ux); x <= Math.max(bottom.getX(),ux); x++) {
                for (int z = Math.min(bottom.getZ(),uz); z <= Math.max(bottom.getZ(),uz); z++) {
                    if (!Util.checkBlock(canPlace,bottom.getWorld().getBlockAt(x,bottom.getY(),z))) passed = false;
                }
            }
            //This portal has an issue with placement. Bump it in all directions and check
            if (!passed) {
                Block bottom2 = bottom;
                ArrayList<BlockFace> faces = new ArrayList<>();
                faces.addAll(Arrays.asList(FaceUtil.AXIS));
                faces.add(face.getOppositeFace());
                for (BlockFace b : faces) {
                    if (passed) break;
                    //Only bump forwards or backwards at this point
                    if (b == FaceUtil.getDirection(this.entDirection,false) || b.getOppositeFace() == FaceUtil.getDirection(this.entDirection,false)) continue;;
                    passed = true;
                    bottom2 = bottom.getRelative(b);
                    ux = bottom2.getX()+face.getModX();
                    uz = bottom2.getZ()+face.getModZ();
                    for (int x = Math.min(bottom2.getX(), ux); x <= Math.max(bottom2.getX(), ux); x++) {
                        for (int z = Math.min(bottom2.getZ(), uz); z <= Math.max(bottom2.getZ(), uz); z++) {
                            if (!Util.checkBlock(canPlace, bottom2.getWorld().getBlockAt(x, bottom2.getY(), z)))
                                passed = false;
                        }

                    }
                }
                if (passed) bottom = bottom2;
            }
            //There are still problems, straighten the portal, then loop and test
            if (!passed) {
                passed = true;
                face = FaceUtil.getDirection(this.entDirection,false);
                ux = bottom.getX()+face.getModX();
                uz = bottom.getZ()+face.getModZ();
                for (int x = Math.min(bottom.getX(),ux); x <= Math.max(bottom.getX(),ux); x++) {
                    for (int z = Math.min(bottom.getZ(),uz); z <= Math.max(bottom.getZ(),uz); z++) {
                        if (!Util.checkBlock(canPlace,bottom.getWorld().getBlockAt(x,bottom.getY(),z))) passed = false;
                    }
                }
                this.entDirection = FaceUtil.faceToVector(face).normalize();
            }
            //There are still problems, bump the straight portal
            if (!passed) {
                Block bottom2 = bottom;
                for (BlockFace b : FaceUtil.AXIS) {
                    if (passed) break;
                    passed = true;
                    bottom2 = bottom.getRelative(b);
                    ux = bottom2.getX()+face.getModX();
                    uz = bottom2.getZ()+face.getModZ();
                    for (int x = Math.min(bottom2.getX(), ux); x <= Math.max(bottom2.getX(), ux); x++) {
                        for (int z = Math.min(bottom2.getZ(), uz); z <= Math.max(bottom2.getZ(), uz); z++) {
                            if (!Util.checkBlock(canPlace, bottom2.getWorld().getBlockAt(x, bottom2.getY(), z)))
                                passed = false;
                        }

                    }
                }
                if (!passed) return;
                bottom = bottom2;
            }
            PortalStick.getInstance().getPortals().put(new V10Block(bottom), this);
            finished = true;
            double yawRad = Math.toRadians(MathUtil.getLookAtYaw(entDirection)+180);
            rotation = new Quaterniond().rotationXYZ(facing.getY()>0?Math.PI:0, yawRad,0);
            rotationOut = new Quaterniond().rotationXYZ(facing.getY()<0?Math.PI:0, yawRad,0);
            System.out.println(yawRad);

            System.out.println(facing);
            return;
        }
        bottom = clicked.getRelative(BlockFace.DOWN);
        top = clicked;
        Material type = bottom.getLocation().add(facing).getBlock().getType();
        if (type.isSolid() || !type.isTransparent() || !Util.checkBlock(canPlace,bottom)) {
            bottom = clicked;
            top = clicked.getRelative(BlockFace.UP);
        }
        type = top.getLocation().add(facing).getBlock().getType();
        if (type.isSolid() || !type.isTransparent() || !Util.checkBlock(canPlace,top)) {
            return;
        }
        if (!top.getType().isSolid()) return;
        if (!bottom.getType().isSolid()) return;
        PortalStick.getInstance().getPortals().put(new V10Block(bottom), this);
        PortalStick.getInstance().getPortals().put(new V10Block(top), this);
        rotation = new Quaterniond().lookAlong(VectorUtil.convert(facing),VectorUtil.UP);
        rotationOut = new Quaterniond().lookAlong(VectorUtil.convert(new Vector(0,0,0).subtract(facing)),VectorUtil.UP);
        finished = true;
    }
    //TODO: check if facing and stair direction are in sync.
    private void placeAngledPortal(Block clicked) {
        Stairs stair = (Stairs) clicked.getState().getData();
        bottom = clicked;
        top = clicked.getRelative(BlockFace.UP).getRelative(stair.getFacing().getOppositeFace());
        if (!stair.isInverted() && stair.getItemType() == Material.SMOOTH_STAIRS) {
            bottom = clicked.getRelative(BlockFace.DOWN).getRelative(stair.getFacing());
            top = clicked;
        }
        if (stair.isInverted() && stair.getItemType() == Material.SMOOTH_STAIRS) {
            bottom = clicked;
            top = clicked.getRelative(BlockFace.UP).getRelative(stair.getFacing());
        }
        if (stair.isInverted() && stair.getItemType() == Material.COBBLESTONE_STAIRS) {
            bottom = clicked.getRelative(BlockFace.DOWN).getRelative(stair.getFacing().getOppositeFace());
            top = clicked;
        }
        if (!top.getType().isSolid()) return;
        if (!bottom.getType().isSolid()) return;
        PortalStick.getInstance().getPortals().put(new V10Block(bottom), this);
        PortalStick.getInstance().getPortals().put(new V10Block(top), this);
        this.facing = FaceUtil.faceToVector(stair.getFacing());
        this.facing.setY(stair.isInverted()?-1:1);
        BlockFace f = FaceUtil.getDirection(facing.clone().setY(0));
        if (f == BlockFace.EAST) {
            rotation = new Quaterniond();
            rotationOut = new Quaterniond().rotationY(Math.PI);
        } else if (f == BlockFace.WEST) {
            rotation = new Quaterniond().rotationY(Math.PI);
            rotationOut = new Quaterniond();
        } else if (f == BlockFace.NORTH) {
            rotation = new Quaterniond().rotationY(-Math.PI/2);
            rotationOut = new Quaterniond().rotationY(Math.PI/2);
        } else if (f == BlockFace.SOUTH) {
            rotation = new Quaterniond().rotationY(Math.PI/2);
            rotationOut = new Quaterniond().rotationY(-Math.PI/2);
        }

        finished = true;
    }

    public Portal getDestination() {
        PortalUser user = PortalStick.getInstance().getUser(owner);
        Portal p = primary ? user.getSecondary() : user.getPrimary();
        if (p == null) {
            Region r = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(bottom));
            p = primary ? r.getSecondary() : r.getPrimary();
        }
        if (p != null && p.isOpen())
            return p;
        return null;
    }

    public boolean isValid() {
        PortalUser user = PortalStick.getInstance().getUser(owner);
        return primary ? user.getPrimary() == this : user.getSecondary() == this;
    }

    public void spawnPortalStand() {
        if (portal == null) {
            Location loc = (top != null ? top : bottom).getLocation().add(0.5, top != null ? -1.5 : -0.7, 0.5).setDirection(new Vector(0, 0, 0).subtract(facing));
            if (facing.getZ() == 0 && facing.getX() == 0) {
                loc = loc.add(0,-0.23,0);
                if (facing.getY() < 0) {
                    loc = loc.add(0,-0.01,0);
                }
            }
            if ((facing.getX() == 0 && facing.getZ() == 0) || facing.getY() == 0) {
                loc = loc.add(facing);
            } else if (facing.getY() == 1) {
                loc = loc.subtract(facing.clone().multiply(0.16)).add(0,0.2,0);
            } else {
                loc = loc.add(-facing.getX()/2.2,-0.6,-facing.getZ()/2.2);
            }
            if (entDirection != null) {
                loc.setDirection(entDirection);
                loc.add(entDirection.clone().multiply(0.5));
            }
            portal = (ArmorStand) bottom.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        }
        NMSArmorStand wrapped = NMSArmorStand.wrap(portal);
        wrapped.lock();
        portal.setGravity(false);
        portal.setRemoveWhenFarAway(false);
        portal.setVisible(false);
        if (facing.getZ() == 0 && facing.getX() == 0) {
            if (facing.getY() > 0) {
                portal.setHeadPose(new EulerAngle(Math.toRadians(180), 0, 0));
            } else {
                portal.setHeadPose(new EulerAngle(0, 0, 0));
            }
        } else if (facing.getY() != 0){
            portal.setHeadPose(new EulerAngle(Math.toRadians(facing.getY()*-45), 0, 0));
        } else {
            portal.setHeadPose(new EulerAngle(Math.toRadians(90), 0, 0));
        }
        wrapped.setCollides(true);
        if (entDirection != null) {
            //Shift the portal up by the direction so that it is centered over two blocks
            Vector v = entDirection.clone().multiply(0.5);
            wrapped.setSize(v.getX(),1,v.getZ());
        }
        wrapped.setWillSave(false);
    }

    public boolean open() {
        if (!finished) return false;
        spawnPortalStand();

        if (getDestination() == null) {
            portal.setHelmet(Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE, 1, (short) (primary ? 64 : 66))));
            portal.setMetadata("portalobj2", new FixedMetadataValue(PortalStick.getInstance(), this));
            open = true;
            if (back != null) back.remove();
            back = null;
            return true;
        }

        portal.setHelmet(Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE, 1, (short) (primary ? 63 : 65))));
        //Used for checking if its a portal (funnel and removing)
        portal.setMetadata("portalobj2", new FixedMetadataValue(PortalStick.getInstance(), this));
        //Dont recreate these if the portal has been opened already, and is just being updated.
        if (bottomStorage == null) {
            bottomStorage = new BlockStorage(bottom);
            if (top != null) {
                topStorage = new BlockStorage(top);
            }
        }
        //Back is null if we go from disconnected -> connected
        if (back == null) {
            if (top != null) {
                back = new HologramFactory().withLocation(top.getLocation().add(0.5, 0.5, 0.5)).withHead(new ItemStack(Material.AIR), 1).withHead(new ItemStack(Material.AIR), 1).build();
            } else if (entDirection == null){
                back = new HologramFactory().withLocation(bottom.getLocation().add(0.5, 0.5, 0.5)).withHead(new ItemStack(Material.AIR), 1).build();
            }
            if (back != null)
                back.getEntity().setMetadata(getMetadataName(), new FixedMetadataValue(PortalStick.getInstance(), this));
        }

        open = true;
        return true;
    }
    public void openFor(Player pl) {
        if (getDestination() == null) return;
        if (this.back.getLines().size()==0) return;
        setTemporary(pl, 0, true);
        if (top != null)
            setTemporary(pl, 1, true);
        checkPortal(pl);
    }

    private void checkPortal(final Player pl) {
        Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), () -> {
            if (portal.getNearbyEntities(0.001, 0.1, 0.001).contains(pl)) {
                checkPortal(pl);
            } else {
                if (this.back.getLines().size()!=0) {
                    setTemporary(pl, 0, false);
                    if (top != null)
                        setTemporary(pl, 1, false);
                }
            }
        }, 5L);
    }
    private void setTemporary(Player pl, int id, boolean toggleEntity) {
        ArmorStand as = (ArmorStand) back.getLines().get(id);
        ItemStack head = id==0?bottomStorage.toStack():topStorage.toStack();
        Block b = id==0?bottom:top;
        pl.sendBlockChange(b.getLocation(),toggleEntity?Material.AIR:head.getType(),head.getData().getData());
        PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        pc.getItemSlots().write(0, EnumWrappers.ItemSlot.HEAD);
        pc.getItemModifier().write(0, toggleEntity ? head : new ItemStack(Material.AIR));
        pc.getIntegers().write(0, as.getEntityId());
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(pl, pc.deepClone());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }
    public void closePlugin() {
        if (topStorage != null) {
            topStorage.set();
        }
        if (bottomStorage != null)
            bottomStorage.set();
        if (this.back != null) {
            back.remove();
        }
        portal.remove();
    }
    public void close() {
        if (!this.open) return;
        open = false;
        if (PortalStick.getInstance().isEnabled()) {
            //Update bridges a tick later so that all portals are placed.
            Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), () -> {
                for (Bridge b : PortalStick.getInstance().getBridgeManager().getBridges()) {
                    if (b.intersectsWith(new V10Block(bottom.getLocation().add(facing).getBlock())) || (top != null && b.intersectsWith(new V10Block(top.getLocation().add(facing).getBlock())))) {
                        b.update();
                    }
                    if (getDestination() != null) {
                        if (b.intersectsWith(new V10Block(getDestination().bottom.getLocation().add(getDestination().facing).getBlock()))
                                || (getDestination().top != null && b.intersectsWith(new V10Block(getDestination().top.getLocation().add(getDestination().facing))))) {
                            b.update();
                        }
                    }
                }
            }, 2l);
        }
        if (this.back != null) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                setTemporary(pl, 0, false);
                if (top != null)
                    setTemporary(pl, 1, false);
            }
            back.remove();
        }
        portal.remove();
        bottomStorage = null;
        portal = null;
        if (getDestination() != null) {
            getDestination().open();
        }
        open();
    }

    Random r = new Random();

    public void teleportEntity(Entity entity, Vector motion) {
        if (this.disabledFor.contains(entity.getUniqueId())) return;
        if (getDestination() != null && getDestination().disabledFor.contains(entity.getUniqueId())) {
            return;
        }
        if (facing.getX() != 0 || facing.getZ() != 0) {
            if (entity.getLocation().getBlockY() > getTop().getY()) return;
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
            TeleportLoc loc = teleportEntity(eloc, motion, entity);
            if (loc == null) return;
            getDestination().disabledFor.add(entity.getUniqueId());
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(PortalStick.getInstance(), () -> getDestination().disabledFor.remove(entity.getUniqueId()), 10L);
            NMSUtils.getInstance().getUtil().teleportFast(entity, b2.setDirection(loc.destination.getDirection()), loc.getVelocity());
            return;
        }
        TeleportLoc loc = teleportEntity(eloc, motion, entity);
        if (loc == null) return;
//        if (!(entity instanceof Player) && !(entity instanceof Laser)) {
//            NMSUtils.getInstance().getUtil().teleportFast(entity, loc.destination, loc.getVelocity());
//        } else {
//            entity.teleport(loc.destination);
//        }

        entity.teleport(loc.destination);
        //Adjust velocity for gel.
        if (!FaceUtil.isVertical(FaceUtil.getDirection(loc.velocity)) && Util.checkInstance(GelTube.class, entity)) {
            loc.velocity = loc.velocity.multiply(r.nextInt(10)+5 / 10d);
        }
        if (loc.getVelocity().getY() > 0.5&& Util.checkInstance(GelTube.class, entity)) {
            loc.velocity = loc.velocity.setY(1);
        }
        entity.setVelocity(loc.velocity);
        Util.playSound(primary ? Sound.PORTAL_EXIT_BLUE : Sound.PORTAL_EXIT_ORANGE, new V10Block(loc.destination));
        getDestination().disabledFor.add(entity.getUniqueId());
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(PortalStick.getInstance(), () -> getDestination().disabledFor.remove(entity.getUniqueId()), 3L);

    }

    @AllArgsConstructor
    @Getter
    @ToString
    public class TeleportLoc {
        Location destination;
        Vector velocity;
    }

    public TeleportLoc teleportEntity(Location loc, Vector motion, Entity en) {
        Portal destination = getDestination();
        if (destination == null) return null;
        Vector fromCenter = loc.toVector().subtract(portal.getLocation().add(0,1,0).toVector());
        Quaterniond q = getRotationInv().mul(destination.getRotation());

        Vector outorient = VectorUtil.rotate(q, fromCenter);
        Vector v = loc.getDirection();
        Vector facing = VectorUtil.rotate(q, v);
        Vector outvector = VectorUtil.rotate(q, motion);
        Location teleport = getDestination().portal.getLocation();
        if (destination.getEntDirection() == null) {
            teleport.add(0,0.5,0);
        }
        //If we have an inverted angled portal, we need to calculate a new location.
        //also, correct rotation of portal.
        if ((destination.facing.getX() != 0 || destination.facing.getZ() != 0) && destination.facing.getY() != 0) {
            if (destination.facing.getY() == -1) {
                if (!(en instanceof Player) && !(en instanceof Monster) && !(en instanceof Villager)) {
                    teleport.add(0,-1,0);
                }
            } else if (destination.facing.getY() == 1) {
                if (!(en instanceof Player) && !(en instanceof Monster) && !(en instanceof Villager)) {
                    teleport.add(0,1,0);
                }
            }
        }
        teleport.setDirection(facing);
        //teleport.add(outorient);

        if ((destination.facing.getX() != 0 || destination.facing.getZ() != 0)) {
            if (destination.facing.getY() == -1) {
                teleport= teleport.add(0,-0.5,0);
            } else if (destination.facing.getY() == 1) {
                teleport= teleport.add(0,0.5,0);
            }
        }
        //If the destination is on the floor
        if (destination.facing.getX() == 0 && destination.facing.getZ() == 0) {
            if (outvector.getY() < 0.5 && outvector.getY() > 0) outvector.setY(0.5);
            teleport = teleport.add(0.5, Math.signum(outvector.getY()),0.5);
            if (outvector.getY()<0) teleport = teleport.add(0, -1,0);
            if (outvector.getY()>0) teleport = teleport.add(0, 1,0);
        }
        if (outvector.getY() > 1) outvector.setY(outvector.getY()*1.2);
        return new TeleportLoc(teleport, outvector);
    }

    public void delete() {
        if (topStorage != null) {
            topStorage.set();
        }
        if (bottomStorage != null)
            bottomStorage.set();
        if (this.back != null) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                setTemporary(pl, 0, false);
                if (top != null)
                    setTemporary(pl, 1, false);
            }
            back.remove();
        }
        if (portal != null)
            portal.remove();
        PortalStick.getInstance().getPortals().remove(new V10Block(bottom));
        if (top != null)
            PortalStick.getInstance().getPortals().remove(new V10Block(top));
    }

    public boolean isRegionPortal() {
        return this.owner.startsWith("§region§_");
    }
    public Quaterniond getRotation() {
        return new Quaterniond(rotation);
    }
    public Quaterniond getRotationInv() {
        return new Quaterniond(rotationOut);
    }
    @Override
    public String getMetadataName() {
        return "portalobj";
    }
    public boolean isAngled() {
        return (facing.getX() != 0 || facing.getZ() != 0) && facing.getY() == 1;
    }
    public boolean isVertical() {
        return facing.getX() == 0 && facing.getZ() == 0;
    }
}

