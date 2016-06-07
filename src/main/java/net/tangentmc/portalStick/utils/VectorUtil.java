
package net.tangentmc.portalStick.utils;

import java.util.Arrays;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.tangentmc.portalStick.util.math.Quaternion;
import net.tangentmc.portalStick.util.math.Vector3;

public class VectorUtil {
	public static Vector rotate(Quaternion q, Vector origin) {
		Vector3 v0 = new Vector3(origin);
		Vector3 tv = q.transform(v0);
		return tv.getVector();
	}
	public static Quaternion getRotationTo(Vector origin, Vector dest)
	{
		Quaternion q = new Quaternion();
		Vector3 v0 = new Vector3(origin);
		Vector3 v1 = new Vector3(dest);
		return q.setFromCross(v0.nor(), v1.nor());
	}
	public static Player getTargetPlayer(final Player player) {
        return getTarget(player, player.getWorld().getPlayers());
    }
	public static boolean isLookingAt(final Player player, final Entity e) {
        return getTarget(player, Arrays.asList(e))!=null;
    }
 
    public static Entity getTargetEntity(final Entity entity) {
        return getTarget(entity, entity.getWorld().getEntities());
    }
 
    public static <T extends Entity> T getTarget(final Entity entity,
            final Iterable<T> entities) {
        if (entity == null)
            return null;
        T target = null;
        final double threshold = 1;
        for (final T other : entities) {
            final Vector n = other.getLocation().toVector()
                    .subtract(entity.getLocation().toVector());
            if (entity.getLocation().getDirection().normalize().crossProduct(n)
                    .lengthSquared() < threshold
                    && n.normalize().dot(
                            entity.getLocation().getDirection().normalize()) >= 0) {
                if (target == null
                        || target.getLocation().distanceSquared(
                                entity.getLocation()) > other.getLocation()
                                .distanceSquared(entity.getLocation()))
                    target = other;
            }
        }
        return target;
    }
}
