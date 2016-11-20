package net.tangentmc.portalStick.managers;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;

import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Grill;
import org.bukkit.block.Block;

public class GrillManager {
	public final Set<Grill> grills = new HashSet<>();
	private final PortalStick plugin = PortalStick.getInstance();
	public void loadGrill(String blockloc) {
		String[] locarr = blockloc.split(",");
		String world = locarr[0];
		if (Bukkit.getWorld(world)==null)  {
			return;
		}
		Grill grill = new Grill(new V10Block(world, (int)Double.parseDouble(locarr[1]), (int)Double.parseDouble(locarr[2]), (int)Double.parseDouble(locarr[3])));
		if (!grill.isComplete())
			plugin.getConfiguration().deleteGrill(blockloc);
	}

	public void blockBreak(Block block) {
		V10Block vblock = new V10Block(block);
		for (Grill g: grills.toArray(new Grill[0])) {
			if (g.getBorder().contains(vblock)) {
				g.remove();
			}
		}
	}
}
