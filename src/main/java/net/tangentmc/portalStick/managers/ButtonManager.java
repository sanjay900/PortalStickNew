package net.tangentmc.portalStick.managers;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Lever;

import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.Util;

public class ButtonManager {
	private PortalStick plugin = PortalStick.getInstance();
    public final HashMap<UUID, V10Block> buttonsToEntity = new HashMap<UUID, V10Block>();

	BlockFace[] blockfaces = new BlockFace[] { BlockFace.WEST,
			BlockFace.NORTH_WEST, BlockFace.NORTH, BlockFace.NORTH_EAST,
			BlockFace.EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST,
			BlockFace.SOUTH_EAST };
	BlockFace[] blockfacesn = new BlockFace[] { BlockFace.WEST,
			BlockFace.NORTH, 
			BlockFace.EAST, BlockFace.SOUTH};
	public void disableAll() {
		buttonsToEntity.values().forEach(b -> changeBtn(b, false));
	}
	@SuppressWarnings("deprecation")
	public Block chkBtn (Location l) {
		Block blockUnder = l.getBlock().getRelative(BlockFace.DOWN);
		Block middle = null;
		if ((blockUnder.getType()==Material.WOOL) && (blockUnder.getData() == (byte) 14|| blockUnder.getData() == (byte) 5)) {
			middle = blockUnder;
		} else {
			for (BlockFace f : blockfaces) {
				if (blockUnder.getRelative(f).getType() == Material.WOOL
						&& (blockUnder.getRelative(f).getData() == (byte) 14||blockUnder.getRelative(f).getData() == (byte) 5)) {
					middle = blockUnder.getRelative(f);
					Location comp = l.clone();
					comp.setY(middle.getY());
					//if (comp.distanceSquared(middle.getLocation().add(0.5,0,0.5)) < 0.6) {
					//	break;
					//} else {
						//middle = null;
					//}
				}
			}
		}
		return  middle;
	}
	@SuppressWarnings("deprecation")
	public void changeBtn(Block middle, boolean on) {
		Block under = middle.getRelative(BlockFace.DOWN);
		byte data;
		if(on) {
			Util.playSound(Sound.BUTTON_DOWN, new V10Block(middle));
			data = 5;
		} else {
			data = 14;
			Util.playSound(Sound.BUTTON_UP, new V10Block(middle));
		}
		middle.setTypeIdAndData(Material.WOOL.getId(), data, false);
		for (BlockFace f : blockfacesn) {
			Block block = under.getRelative(f,2);
			Lever lever = null;
			BlockState state;
			if (!block.getRelative(BlockFace.UP).getType().isSolid()) {
				block = under.getRelative(f,1);
				state = block.getState(); 
				state.getData().setData((byte)0);
				state.update();
				block.setType(Material.LEVER);
				lever = (Lever) state.getData();
				lever.setFacingDirection(f);
				Block supportBlock = block.getRelative(f.getOppositeFace());
				supportBlock.setType(Material.EMERALD_BLOCK);
			} else {
				state = block.getState(); 
				state.getData().setData((byte)0);
				state.update();			
				block.setType(Material.LEVER); 
				state = block.getState(); 
				state.getData().setData((byte)0);
				state.update();
				lever = (Lever) state.getData();
				lever.setPowered(on);
				state.setData(lever);
				state.update();
			}
			Block redstone = block.getRelative(lever.getAttachedFace(),2);
			if (redstone.getType().name().contains("REDSTONE"))
				redstone.setData((byte) (redstone.getData()>4?redstone.getData()-4:redstone.getData()+4));
			lever.setPowered(on);
			state.setData(lever);
			state.update();

		}
	}


	public void changeBtn(V10Block middle, boolean on) {
		changeBtn(middle.getHandle().getBlock(), on);
	}
}
