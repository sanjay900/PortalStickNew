package net.tangentmc.portalStick.managers;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;

import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Laser;
import net.tangentmc.portalStick.utils.Util;

public class LaserManager {
	private final PortalStick plugin = PortalStick.getInstance();
	public Set<Laser> lasers = new HashSet<>();
	public void removeLaser(Laser s) {
		lasers.remove(s);
		s.remove();
		plugin.getConfiguration().saveAll();
	}
	public void saveLaser(Laser laser, V10Block block) {
		lasers.add(laser);
	}
	public void loadLaser(String blockloc) {
		String[] locarr = blockloc.split(",");
		String world = locarr[0];
		if (Bukkit.getWorld(world)==null)  {
			return;
		}
		V10Block blk = new V10Block(world, (int)Double.parseDouble(locarr[1]), (int)Double.parseDouble(locarr[2]), (int)Double.parseDouble(locarr[3]));
		if (!createLaser(blk)) {
			plugin.getConfiguration().deleteLaser(blockloc);
		}
	}
	public boolean createLaser(V10Block clickedBlock) {
		
		BlockFace b = Util.getGlassPaneDirection(clickedBlock.getHandle().getBlock());
		if (b == null) return false;
		Laser s = new Laser(clickedBlock.getHandle().setDirection(FaceUtil.faceToVector(b)));
		saveLaser(s, clickedBlock);
		return true;
	}
}
