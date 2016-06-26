package net.tangentmc.portalStick.managers;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;

import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Grill;

public class GrillManager {
	//TODO: Grills arent removed on block break or load with incomplete frame or reload
	public final Set<Grill> grills = new HashSet<Grill>();
	private final PortalStick plugin = PortalStick.getInstance(); 

	public void loadGrill(String blockloc) {
		String[] locarr = blockloc.split(",");
		String world = locarr[0];
		if (Bukkit.getWorld(world)==null)  {
			return;
		}
		Grill grill = new Grill(new V10Block(world, (int)Double.parseDouble(locarr[1]), (int)Double.parseDouble(locarr[2]), (int)Double.parseDouble(locarr[3])).getHandle().getBlock());
		if (!grill.isComplete())
			plugin.getConfiguration().deleteGrill(blockloc);
	}

}
