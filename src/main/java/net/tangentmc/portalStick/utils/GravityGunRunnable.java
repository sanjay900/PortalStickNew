package net.tangentmc.portalStick.utils;

import org.bukkit.scheduler.BukkitRunnable;

import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.PortalUser;

public class GravityGunRunnable extends BukkitRunnable {
	public GravityGunRunnable() {
		this.runTaskTimer(PortalStick.getInstance(), 1l, 1l);
	}
	@Override
	public void run() {
		for (PortalUser u: PortalStick.getInstance().getUsers().values()) {
			u.testHeld();
		}
	}

}
