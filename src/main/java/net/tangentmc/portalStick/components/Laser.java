package net.tangentmc.portalStick.components;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap;
import net.tangentmc.nmsUtils.utils.LocationIterator;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.BlockStorage;
import net.tangentmc.portalStick.utils.MetadataSaver;
import net.tangentmc.portalStick.utils.MetadataSaver.Metadata;
import net.tangentmc.portalStick.utils.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor
@Metadata(metadataName = "laseren")
public class Laser implements MetadataSaver {
    ExpiringMap<BlockStorage, BlockStorage> recievers = ExpiringMap.builder()
            //Expire in 2 ticks (Will reset changed blocks unless they are repeatedly hit)
            .expiration(100, TimeUnit.MILLISECONDS)
            .expirationListener(new ExpirationListener<BlockStorage, BlockStorage>() {
                public void expired(BlockStorage block, BlockStorage redstone) {
                    block.set();
                    redstone.set();
                }
            })
            .build();
    @Getter
    Location init;
    BukkitTask loop;
    Laser child;
    Portal sourcePortal;
    Cube sourceCube;
    Location current = null;
    public void setInit(Location init) {
        this.init = init;
        if (loop != null)
            loop.cancel();
        loop = Bukkit.getScheduler().runTaskTimer(PortalStick.getInstance(), () -> {
            Vector v = this.init.toVector();
            if (sourceCube == null) v.subtract(this.init.getDirection());
            LocationIterator it = new LocationIterator(this.init.getWorld(),v,this.init.getDirection().multiply(STEP_SIZE), 200);
            World world = init.getWorld();
            for (double i = 0; sourceCube == null && i < 1; i+=STEP_SIZE) {
                world.spawnParticle(Particle.REDSTONE, it.next(), 2, 0, 0, 0, 0);
            }
            while ((current = it.next()).getChunk().isLoaded() && it.hasNext()) {
                if (blockCollide(current.getBlock())) break;
                for (Entity en : this.getNearbyEntities(STEP_SIZE, STEP_SIZE, STEP_SIZE)) {
                    if (sourcePortal != null && en.getUniqueId().equals(sourcePortal.portal.getUniqueId())) continue;
                    if (sourceCube != null && en.getUniqueId().equals(sourceCube.as.getUniqueId())) continue;
                    if (en.hasMetadata("portalobj2")) {
                        Portal pl = (Portal) en.getMetadata("portalobj2").get(0).value();
                        if (pl.getDestination() == null) continue;
                        Portal.TeleportLoc tloc = pl.teleportEntity(current,init.getDirection(),null);

                        Location loc = tloc.getDestination().setDirection(tloc.getVelocity());
                        if (child != null) {
                            child.setInit(loc);
                            child.sourcePortal = pl.getDestination();
                            child.sourceCube = null;
                        } else {
                            child = new Laser(loc, pl.getDestination());
                        }
                        world.spawnParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0);
                        world.spawnParticle(Particle.REDSTONE, current, 2, 0, 0, 0, 0);
                        //Iterate 1 more block
                        for (double i = 0; i < 1; i+=STEP_SIZE) {
                            world.spawnParticle(Particle.REDSTONE, it.next(), 2, 0, 0, 0, 0);
                        }
                        return;
                    }
                    Cube cube = Util.getInstance(Cube.class,en);
                    if (cube != null && cube.getType() == Cube.CubeType.LASER) {
                        Location loc = en.getLocation().add(0,0.5,0);
                        loc.setDirection(loc.getDirection().setY(0));
                        if (child != null) {
                            child.setInit(loc);
                            child.sourceCube = cube;
                            child.sourcePortal = null;
                        } else {
                            child = new Laser(loc, cube);
                        }
                        world.spawnParticle(Particle.REDSTONE, current, 2, 0, 0, 0, 0);
                        return;
                    }
                }
                world.spawnParticle(Particle.REDSTONE, current, 2, 0, 0, 0, 0);
            }
            if (child != null) {
                child.remove();
                child = null;
            }
        }, 1L, 1L);
    }
    Block source;
    public Laser(Location init, Block source) {
        setInit(init);
        this.source = source;
    }

    public Laser(Location init, Portal pl) {
        setInit(init);
        this.sourcePortal = pl;
    }

    public Laser(Location init, Cube cube) {
        setInit(init);
        this.sourceCube = cube;
    }

    public boolean blockCollide(Block blk) {
        if (blk.getType() == Material.STAINED_GLASS_PANE && (blk.getData() == (byte) 9 || blk.getData() == (byte) 4)) {


            BlockFace dir2 = Util.getGlassPaneDirection(blk);
            boolean isVertical = dir2 == null;
            if (isVertical) dir2 = BlockFace.UP;
            BlockStorage storage = new BlockStorage(blk);
            //Incase we override a previous panel, make sure to revert to off
            storage.setData((byte)9);
            BlockStorage redstone = new BlockStorage(blk.getRelative(dir2.getOppositeFace()));
            redstone.setId(9);
            blk.getRelative(dir2.getOppositeFace()).setType(Material.REDSTONE_BLOCK);
            if (recievers.containsKey(storage)) recievers.resetExpiration(storage);
            else this.recievers.put(storage, redstone);
            blk.setData((byte) 4);
            if (!isVertical) {
                return true;
            }
            return false;
        }
        return blk.getType().isSolid();
    }

    public String getStringLocation() {
        Location loc = source==null?init:source.getLocation();
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public List<Entity> getNearbyEntities(double v, double v1, double v2) {
        return (List<Entity>) current.getWorld().getNearbyEntities(current, v, v1, v2);
    }

    public void remove() {
        loop.cancel();
        recievers.clear();
    }
    static final double STEP_SIZE = 0.2;
}
