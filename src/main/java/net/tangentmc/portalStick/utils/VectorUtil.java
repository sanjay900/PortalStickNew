
package net.tangentmc.portalStick.utils;


import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.Arrays;
import java.util.Collections;

public class VectorUtil {
    public static Vector rotate(Quaterniond q, Vector origin) {
        Vector3d v0 = new Vector3d(origin.getX(), origin.getY(), origin.getZ());
        Vector3d tv = q.transform(v0);
        return new Vector(tv.x, tv.y, tv.z);
    }
    /**
     * Calculate a quaternion between two vectors
     * Corrections based on the below link:
     * https://bitbucket.org/sinbad/ogre/src/9db75e3ba05c/OgreMain/include/OgreVector3.h?fileviewer=file-view-default#cl-651
     * @param origin
     * @param dest
     * @return
     */
    public static Quaterniond getRotationTo(Vector origin, Vector dest) {
        Vector3d v0 = new Vector3d(origin.getX(), origin.getY(), origin.getZ());
        Vector3d v1 = new Vector3d(dest.getX(), dest.getY(), dest.getZ());
        return new Quaterniond().rotateTo(v0,v1);
    }

    public static Player getTargetPlayer(final Player player) {
        return getTarget(player, player.getWorld().getPlayers());
    }

    public static boolean isLookingAt(final Player player, final Entity e) {
        return getTarget(player, Collections.singletonList(e)) != null;
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
