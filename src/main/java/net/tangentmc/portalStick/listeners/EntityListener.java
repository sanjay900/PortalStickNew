package net.tangentmc.portalStick.listeners;

import net.tangentmc.nmsUtils.NMSUtil;
import net.tangentmc.nmsUtils.NMSUtils;
import net.tangentmc.portalStick.components.*;
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

import net.tangentmc.nmsUtils.entities.NMSEntity;
import net.tangentmc.nmsUtils.entities.NMSHologram;
import net.tangentmc.nmsUtils.entities.NMSLaser;
import net.tangentmc.nmsUtils.events.EntityCollideWithBlockEvent;
import net.tangentmc.nmsUtils.events.EntityCollideWithEntityEvent;
import net.tangentmc.nmsUtils.events.EntityMoveEvent;
import net.tangentmc.nmsUtils.events.LaserCollideWithEntityEvent;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Cube.CubeType;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.Util;

public class EntityListener implements Listener {
	public EntityListener() {
		Bukkit.getPluginManager().registerEvents(this, PortalStick.getInstance());
	}

	@EventHandler
	public void entityCollide(EntityCollideWithEntityEvent evt) {
		boolean willCollide = testCollision(evt.getTarget(),evt.getCollider());
		willCollide |= testCollision(evt.getCollider(),evt.getTarget());
		evt.setWillCollide(willCollide);
	}

	@EventHandler
	public void entityMoveEvent(PlayerMoveEvent evt) {
		if (evt.getTo().distance(evt.getFrom())==0) return;
		Player collider = evt.getPlayer();
		if (collider.getNearbyEntities(1, 0.7, 1).stream().anyMatch(en -> Util.checkInstance(Funnel.class, en))) return;
		if (collider.hasMetadata("inFunnel")) {
			collider.setFlying(false);
			if (collider.getGameMode() == GameMode.SURVIVAL || collider.getGameMode() == GameMode.ADVENTURE)
				collider.setAllowFlight(false);
			collider.removeMetadata("inFunnel", PortalStick.getInstance());
		}
	}
	@EventHandler
	public void entityMoveEvent(EntityMoveEvent evt) {
		if (evt.getTo().distance(evt.getFrom())==0) return;
		Entity collider = evt.getEntity();
		if (collider.isInsideVehicle()) return;
		if (collider.getNearbyEntities(0.1, 0.1, 0.1).stream().anyMatch(en -> Util.checkInstance(Funnel.class, en))) return;
		if (collider.hasMetadata("inFunnel")) {
			collider.removeMetadata("inFunnel", PortalStick.getInstance());
			((NMSEntity)collider).setFrozen(false);
		}
	}
	private boolean testCollision(Entity target, Entity collider) {
		Funnel f = Util.getInstance(Funnel.class, target);
		if (collider.isInsideVehicle()) collider = collider.getVehicle();
		if (f != null && !Util.checkInstance(Funnel.class, collider) && !Util.checkInstance(Portal.class, collider) && !collider.hasMetadata("portalobj2")) {
			if (collider instanceof ArmorStand) {
				((ArmorStand)collider).setGravity(true);
			} else if (collider instanceof Player) {
				((Player) collider).setAllowFlight(true);
				((Player) collider).setFlying(true);
			} else {
				NMSUtils.getInstance().getUtil().getNMSEntity(collider).setFrozen(true);
			}
			Vector dir = f.isReversed()?new Vector(0,0,0).subtract(target.getLocation().getDirection()):target.getLocation().getDirection();
			if (collider.getLocation().getY() != target.getLocation().getY()) {
				double y = target.getLocation().getY()-(collider instanceof ArmorStand?0.3:0)-collider.getLocation().getY();
				
				collider.setVelocity(dir.clone().add(new Vector(0,y,0)).multiply(0.1));
			} else {
				collider.setVelocity(dir.clone().multiply(0.1));
			}
			
			
			if (collider instanceof LivingEntity && !(collider instanceof Player) && !(collider instanceof ArmorStand)) {
				if (!collider.getLocation().add(dir.clone().multiply(0.5)).getBlock().getType().isSolid())
				collider.teleport(collider.getLocation().add(dir.clone().multiply(0.05)));
			}
			collider.setMetadata("inFunnel", new FixedMetadataValue(PortalStick.getInstance(),f));
		} 
		boolean isholo = collider instanceof NMSHologram || (collider.isInsideVehicle() && collider.getVehicle() instanceof NMSHologram);
		boolean islaser = collider instanceof NMSLaser || (collider.isInsideVehicle() && collider.getVehicle() instanceof NMSLaser);
		Portal portal = Util.getInstance(Portal.class, target);
		if (portal != null &&!Util.checkInstance(Funnel.class, collider) && !collider.hasMetadata("portalobj") && !collider.hasMetadata("portalobj2") && !islaser && !Util.checkInstance(AutomatedPortal.class, collider)) {
			portal.teleportEntity(collider,target.getLocation().toVector().subtract(collider.getLocation().toVector()));
			return false;
		}
		//Boats are special.
		//TODO come up with a better solution to this, it will do though.
		if (target.hasMetadata("portalobj2") && collider instanceof Boat) {
			portal = (Portal) target.getMetadata("portalobj2").get(0).value();
			portal.teleportEntity(collider,target.getLocation().getDirection());
			return false;
		}
		if (target.hasMetadata("portalobj2") && !collider.hasMetadata("portalobj")) {
			portal = (Portal) target.getMetadata("portalobj2").get(0).value();
			if (collider instanceof Player) {
				portal.openFor(collider);
			} else {
				BlockIterator it = new BlockIterator(collider.getWorld(),collider.getLocation().toVector(),collider.getVelocity(),0,5);
                while (it.hasNext()) {
                    Location l = it.next().getLocation();
                    if (portal.getBottom().getLocation().distance(l) < 1 || (portal.getTop() != null && portal.getTop().getLocation().distance(l) < 1)) {
                        portal.teleportEntity(collider, target.getLocation().getDirection());
                        return true;
                    }
                }
			}
		}
		if (target.hasMetadata("cuben")) {
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

	@EventHandler
	public void onLaserCollide(LaserCollideWithEntityEvent event) {
		if (event.getCollider().hasMetadata("cuben")) {
			Cube cube = (Cube) event.getCollider().getMetadata("cuben").get(0).value();
			if (cube.getType() == CubeType.LASER)
				event.setNewLoc(event.getCollider().getLocation());
		}

		Portal p = Util.getInstance(Portal.class, event.getCollider());
		if (p != null && p.getDestination() != null) {
			Location b = p.getDestination().getBottom().getLocation();
			if (p.getTop() != null && event.getCollider().getLocation().getBlockY() == p.getBottom().getY()) {
				b = p.getDestination().getTop().getLocation();
			}
			b = b.getBlock().getLocation().add(0.5,p.getDestination().isVertical()?p.getDestination().getFacing().getY()*1.6:0,0.5).setDirection(p.getDestination().getFacing());
			event.setNewLoc(b);
			return;
		}

		if (event.getCollider() instanceof LivingEntity && !(event.getCollider() instanceof NMSLaser)) {
			((LivingEntity)event.getCollider()).damage(2);
			return;
		}

	}
	@SuppressWarnings("deprecation")
	@EventHandler
	public void gel(EntityCollideWithBlockEvent evt) {
		Entity en = evt.getEntity();

		GelTube tube = Util.getInstance(GelTube.class, en);
		if (tube != null) {
			tube.groundCollide(evt.getBlock());
			en.remove();
			return;
		}

		if (evt.getBlock().getType() == Material.PISTON_BASE || evt.getBlock().getType() == Material.PISTON_STICKY_BASE) {
			if (Util.checkPiston(evt.getBlock().getLocation(), evt.getEntity()))
				return;
		}
		if (evt.getBlock().getType() == Material.WOOL) {
			if (evt.getBlock().getData() == (byte)3) {
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
			if (evt.getBlock().getData() == (byte)1) {
				//A lot of entities have code to slow them down every tick, making 1.5 useless at speeding them up. 
				double mul = 5;
				if (evt.getEntity() instanceof Player) {
					mul = 1.5;
				}
				Vector vel = evt.getVelocity();
				vel.setX(vel.getX()*mul);
				vel.setZ(vel.getZ()*mul);
				evt.getEntity().setVelocity(vel);
			}
		}
	}
}
