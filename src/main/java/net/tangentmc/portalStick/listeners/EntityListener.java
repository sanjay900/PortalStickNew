package net.tangentmc.portalStick.listeners;

import net.tangentmc.nmsUtils.entities.NMSHologram;
import net.tangentmc.nmsUtils.events.EntityCollideWithBlockEvent;
import net.tangentmc.nmsUtils.events.EntityCollideWithEntityEvent;
import net.tangentmc.nmsUtils.events.EntityMoveEvent;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.*;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.GelType;
import net.tangentmc.portalStick.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class EntityListener implements Listener {
    public EntityListener() {
        Bukkit.getPluginManager().registerEvents(this, PortalStick.getInstance());
    }

    @EventHandler
    public void entityCollide(EntityCollideWithEntityEvent evt) {
        boolean willCollide = testCollision(evt.getTarget(), evt.getCollider(), evt.getVelocity());
        willCollide |= testCollision(evt.getCollider(), evt.getTarget(), evt.getVelocity());
        evt.setWillCollide(willCollide);
    }

    @EventHandler
    public void entityMoveEvent(PlayerMoveEvent evt) {
        if (evt.getTo().distance(evt.getFrom()) == 0) return;
        Player collider = evt.getPlayer();
        if (collider.getNearbyEntities(1, 0.7, 1).stream().anyMatch(en -> Util.checkInstance(Funnel.FunnelEn.class, en))) return;
        if (collider.hasMetadata("inFunnel")) {
            collider.setFlying(false);
            if (collider.getGameMode() == GameMode.SURVIVAL || collider.getGameMode() == GameMode.ADVENTURE)
                collider.setAllowFlight(false);
            collider.removeMetadata("inFunnel", PortalStick.getInstance());
        }
    }

    @EventHandler
    public void entityMoveEvent(EntityMoveEvent evt) {
        if (evt.getTo().distance(evt.getFrom()) == 0) return;
        Entity collider = evt.getEntity();
        if (collider.isInsideVehicle()) return;
        if (collider.getNearbyEntities(0.1, 0.1, 0.1).stream().anyMatch(en -> Util.checkInstance(Funnel.FunnelEn.class, en)))
            return;
        if (collider.hasMetadata("inFunnel")) {
            collider.removeMetadata("inFunnel", PortalStick.getInstance());
            collider.setGravity(false);
        }
    }
    private boolean testCollision(Entity target, Entity collider, Vector motion) {
        Funnel.FunnelEn f = Util.getInstance(Funnel.FunnelEn.class, target);
        if (collider.isInsideVehicle()) collider = collider.getVehicle();
        if (f != null && !Util.checkInstance(Funnel.FunnelEn.class, collider) && !Util.checkInstance(Portal.PortalEntity.class, collider) && !Util.checkInstance(Portal.PortalFrame.class, collider)) {

            if (collider instanceof Player) {
                ((Player) collider).setAllowFlight(true);
                ((Player) collider).setFlying(true);
            } else {
                collider.setGravity(true);
            }
            Vector dir = f.getFunnel().isReversed() ? new Vector(0, 0, 0).subtract(target.getLocation().getDirection()) : target.getLocation().getDirection();
            if (collider.getLocation().getY() != target.getLocation().getY()) {
                double y = target.getLocation().getY() - (collider instanceof ArmorStand ? 0.3 : 0) - collider.getLocation().getY();

                collider.setVelocity(dir.clone().add(new Vector(0, y, 0)).multiply(0.1));
            } else {
                collider.setVelocity(dir.clone().multiply(0.1));
            }


            if (collider instanceof LivingEntity && !(collider instanceof Player) && !(collider instanceof ArmorStand)) {
                if (!collider.getLocation().add(dir.clone().multiply(0.5)).getBlock().getType().isSolid())
                    collider.teleport(collider.getLocation().add(dir.clone().multiply(0.05)));
            }
            collider.setMetadata("inFunnel", new FixedMetadataValue(PortalStick.getInstance(), f));
        }
        boolean isholo = collider instanceof NMSHologram || (collider.isInsideVehicle() && collider.getVehicle() instanceof NMSHologram);
        Portal portal = null;
        Portal.PortalFrame frame = Util.getInstance(Portal.PortalFrame.class,target);
        Portal.PortalEntity pen = Util.getInstance(Portal.PortalEntity.class,target);
        if (Util.checkInstance(Funnel.FunnelEn.class, collider) || Util.checkInstance(Portal.PortalFrame.class, collider) ||
                Util.checkInstance(Portal.PortalEntity.class, collider) || Util.checkInstance(AutomatedPortal.class, collider)) return false;
        if (frame != null) portal = frame.getPortal();
        if (portal != null) {
            portal.teleportEntity(collider, motion);
            return false;
        }
        if (pen != null) portal = pen.getPortal();
        if (portal != null && collider instanceof Boat) {
            portal.teleportEntity(collider, target.getLocation().getDirection());
            return false;
        }
        if (portal != null) {
            if (collider instanceof Player && portal.getEntDirection() == null) {
                portal.openFor((Player) collider);
            } else {
                if (portal.getEntDirection() != null){
                    portal.teleportEntity(collider, motion);
                }
                if (collider.getVelocity().lengthSquared()>0) {
                    BlockIterator it = new BlockIterator(collider.getWorld(), collider.getLocation().toVector(), collider.getVelocity(), 0, 5);
                    while (it.hasNext()) {
                        Location l = it.next().getLocation();
                        if (portal.getBottom().getLocation().distance(l) < 1 || (portal.getTop() != null && portal.getTop().getLocation().distance(l) < 1)) {
                            portal.teleportEntity(collider, motion);
                            return true;
                        }
                        if (portal.getEntDirection() != null && (portal.getPortal().getLocation().distance(l)<0.4 ||
                                portal.getPortal().getLocation()
                                        .add(portal.getEntDirection().clone().multiply(0.5)).distance(l) <0.4||
                                portal.getPortal().getLocation()
                                        .subtract(portal.getEntDirection().clone().multiply(0.5)).distance(l)<0.4)) {
                            portal.teleportEntity(collider, motion);
                            return true;
                        }
                    }
                }
            }
        }
        if (target.hasMetadata("cuben")) {
            GelTube.Gel tube = Util.getInstance(GelTube.Gel.class,collider);
            if (tube != null) {
                Util.getInstance(Cube.class,target).setGelType(tube.getTube().getType());
                return false;
            }
            return true;
        }
        if (collider instanceof Laser && target instanceof LivingEntity) {
            ((LivingEntity) target).damage(2);
        }
        //Grills should NOT emacipate themselves
        if (collider.getType() != EntityType.FALLING_BLOCK && collider.getType() != EntityType.COMPLEX_PART)
            if (target.hasMetadata("grillen") && !isholo) {
                Grill grill = (Grill) target.getMetadata("grillen").get(0).value();
                grill.emacipate(collider);
            }
        return false;
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void gel(EntityCollideWithBlockEvent evt) {
        Entity en = evt.getEntity();
        Cube cube = Util.getInstance(Cube.class,en);

        GelTube.Gel tube = Util.getInstance(GelTube.Gel.class, en);
        if (tube != null) {
            Portal portal;
            Portal.PortalEntity pen = Util.retrieveMetadata(en.getLocation().getBlock(),1, Portal.PortalEntity.class);
            if (pen != null) {
                portal = pen.getPortal();
                if (portal.getDestination() == null) {
                    en.remove();
                    return;
                }
                portal.teleportEntity(en, en.getVelocity());
                return;
            }

            tube.getTube().groundCollide(evt.getBlock());
            en.remove();
            return;
        }

        if (evt.getBlock().getType() == Material.PISTON_BASE || evt.getBlock().getType() == Material.PISTON_STICKY_BASE) {
            if (Util.checkPiston(evt.getBlock().getLocation(), evt.getEntity()))
                return;
        }
        if ((cube != null && cube.getGelType() == GelType.JUMP) || (evt.getBlock().getType() == Material.WOOL && evt.getBlock().getData() == (byte) 3)) {
            Vector velocity = evt.getVelocity();
            if (evt.getFace().getModX() != 0) {
                velocity.setX(-evt.getFace().getModX());
            }
            if (evt.getFace().getModY() != 0) {
                velocity.setY(-evt.getFace().getModY());
            }
            if (evt.getFace().getModZ() != 0) {
                velocity.setZ(-evt.getFace().getModZ());
            }
            evt.getEntity().setVelocity(velocity);
            Util.playSound(Sound.GEL_BLUE_BOUNCE, new V10Block(evt.getBlock()));
        }
        if ((cube != null && cube.getGelType() == GelType.JUMP) || (evt.getBlock().getType() == Material.WOOL && evt.getBlock().getData() == (byte) 1)) {
            //A lot of entities have code to slow them down every tick, making 1.5 useless at speeding them up.
            double mul = 5;
            if (evt.getEntity() instanceof Player) {
                mul = 1.5;
            }
            Vector vel = evt.getVelocity();
            vel.setX(vel.getX() * mul);
            vel.setZ(vel.getZ() * mul);
            evt.getEntity().setVelocity(vel);
        }
    }
}
