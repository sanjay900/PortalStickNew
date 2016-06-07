package net.tangentmc.portalStick.managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.GelTube;

public class GelManager {
	@Getter
	List<GelTube> tubes = new ArrayList<>();
	public void createTube(V10Block from, BlockFace facing, ItemStack stack) {
		if (stack.getType() == Material.WOOL) {
			if (stack.getData().getData() == (byte)1) {
				stack = new ItemStack(Material.SAND,1,(short) 1);
			}
			if (stack.getData().getData() == (byte)3) {
				stack = new ItemStack(Material.LAPIS_ORE);
			}
		}
		GelTube tube = new GelTube(from.getHandle().getBlock(),facing, stack);
		tubes.add(tube);
		
	}
	public void disableAll() {
		tubes.forEach(GelTube::stop);
	}
	public void loadGel(String blockloc) {
		String[] locarr = blockloc.split(",");
		String world = locarr[0];
		if (Bukkit.getWorld(world)==null)  {
			return;
		}
		V10Block blk = new V10Block(world, (int)Double.parseDouble(locarr[1]), (int)Double.parseDouble(locarr[2]), (int)Double.parseDouble(locarr[3]));
		Block b = blk.getHandle().getBlock();
		if (b.getType() == Material.DISPENSER) {
			org.bukkit.material.Dispenser disp = (org.bukkit.material.Dispenser) b.getState().getData();
			InventoryHolder ih = (InventoryHolder) b.getState();
			ItemStack is = ih.getInventory().getItem(4);
			if(is != null) {
				createTube(blk,disp.getFacing(),is);
				return;
			}
		} 
		PortalStick.getInstance().getConfiguration().deleteGel(blockloc);
	}
}
