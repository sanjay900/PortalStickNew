package net.tangentmc.portalStick.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Directional;

import lombok.Getter;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Bridge;
import net.tangentmc.portalStick.components.Funnel;
@Getter
public class FunnelBridgeManager {

	private ArrayList<Bridge> bridges = new ArrayList<>();

	public void loadBridge(String blockloc) {
		String[] locarr = blockloc.split(",");
		String type = locarr[0];
		locarr = Arrays.copyOfRange(locarr, 1,5);
		String world = locarr[0];
		if (Bukkit.getWorld(world)==null)  {
			return;
		}
		V10Block blk = new V10Block(world, (int)Double.parseDouble(locarr[1]), (int)Double.parseDouble(locarr[2]), (int)Double.parseDouble(locarr[3]));
		Bridge b = type.equals("Bridge")?new Bridge(blk.getHandle().getBlock()):new Funnel(blk.getHandle().getBlock());
		if (!b.isComplete()) {
			PortalStick.getInstance().getConfiguration().deleteBridge(blockloc);
			return;
		}
		bridges.add(b);
	}

	public void blockUpdate(Block block) {
		//Wait a tick so that the block is modified before iterating.
		Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), ()->{
			Iterator<Bridge> it = bridges.iterator();
			while (it.hasNext()) {
				Bridge b = it.next();
				if (new V10Block(block).equals(new V10Block(b.getInit()))) {
					b.remove();
					it.remove();
					PortalStick.getInstance().getConfiguration().saveAll();
				}
				if (b.intersectsWith(new V10Block(block))||b.isNearby(new V10Block(block))) {
					b.update();
				}
			}
		},1l);
	}

	public Bridge createBridge(Block block) {
		Bridge bridge;
		if (block.getData() == (byte)14) 
			bridge = new Bridge(block);
		else 
			bridge = new Funnel(block);
		if (bridge.isComplete())
			bridges.add(bridge);
		PortalStick.getInstance().getConfiguration().saveAll();
		return bridge;
	}

	public void powerBlock(Block block, boolean powered) {
		if (block.getType() == Material.LEVER) {
			if (block.getState().getData() instanceof Directional) {
				block = block.getRelative(((Directional)block.getState().getData()).getFacing().getOppositeFace());
			}
		}
		Iterator<Bridge> it = bridges.iterator();
		while (it.hasNext()) {
			Bridge b = it.next();
			BlockFace bf = FaceUtil.getDirection(b.getStart().getDirection());
			if (new V10Block(block.getRelative(FaceUtil.rotate(bf, 2))).equals(new V10Block(b.getInit().getBlock()))) {
				b.setReversed(powered);
				b.update();
			}
			if (new V10Block(block.getRelative(FaceUtil.rotate(bf, -2))).equals(new V10Block(b.getInit().getBlock()))) {
				b.toggleState(powered);
				b.update();
			}
		}
	}

	public void disableAll() {
		bridges.forEach(Bridge::remove);
	}

}
