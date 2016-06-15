package net.tangentmc.portalStick.utils;

import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import net.tangentmc.nmsUtils.utils.V10Block;
@Setter
public class BlockStorage {

    private int id;
    private V10Block location;
    private byte data;
    
    @SuppressWarnings("deprecation")
	public BlockStorage(Block block) {
        this.id = block.getTypeId(); 
        this.location = new V10Block(block);
        this.data = block.getData();
    }
    
    public int getID() {
        return id;
    }
    
    public byte getData() {
        return data;
    }
    
    public void set() {
        Location loc = location.getHandle();
        if(loc != null) {
            Block block = loc.getBlock();
            block.setTypeIdAndData(id, data, true);
        }
    }
    
    public V10Block getLocation() {
        return location;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result
                + ((location == null) ? 0 : location.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if(obj != null && obj instanceof BlockStorage) {
            BlockStorage other = (BlockStorage)obj;
            return id == other.id && location.equals(other.location);
        }
        return false;
    }

	public ItemStack toStack() {
		return new ItemStack(id,1,data);
	}
}
