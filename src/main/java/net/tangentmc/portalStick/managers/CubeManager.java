package net.tangentmc.portalStick.managers;

import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.components.Cube;
import net.tangentmc.portalStick.utils.Util;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;

public class CubeManager {
    public static void spawnCube(Block hatchMiddle, boolean powered, Cube.CubeType type, Block sign, boolean first) {
        V10Block loc = new V10Block(hatchMiddle);
        loc.getHandle().getWorld().getEntities().stream()
                .filter(en -> Util.checkInstance(Cube.class, en))
                .filter(en -> Util.getInstance(Cube.class, en).getSpawner().equals(loc))
                .forEach(Entity::remove);

        if (powered) {
            Sign s = (Sign) sign.getState();
            if ((!s.getLine(2).equals("norespawn")||first)) {
                new Cube(type,hatchMiddle.getLocation().add(0.5,0,0.5),loc);
            }
        }
    }
}
