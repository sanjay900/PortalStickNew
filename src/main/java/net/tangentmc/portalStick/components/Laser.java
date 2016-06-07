package net.tangentmc.portalStick.components;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap;
import net.tangentmc.nmsUtils.NMSUtils;
import net.tangentmc.nmsUtils.entities.NMSLaser;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.MetadataSaver.Metadata;
import net.tangentmc.portalStick.components.Portal.TeleportLoc;
import net.tangentmc.portalStick.utils.BlockStorage;
import net.tangentmc.portalStick.utils.Util;
@NoArgsConstructor
@Metadata(metadataName = "laseren")
public class Laser implements MetadataSaver{
	NMSLaser laser;
	Map<BlockStorage, BlockStorage> furnaces = ExpiringMap.builder()
			//Expire in 2 ticks (Will reset changed blocks unless they are repeatedly hit)
			.expiration(100, TimeUnit.MILLISECONDS)
			.expirationListener(new ExpirationListener<BlockStorage, BlockStorage>() { 
				public void expired(BlockStorage block, BlockStorage redstone) { 
					block.set();
					redstone.set();
				}
			})
			.build();
	@Getter
	Location init;
	public Laser(Location init) {
		Vector dir = init.getDirection();
		this.init = init;
		init = init.getBlock().getLocation().add(0.5, 0, 0.5).setDirection(dir);
		laser = NMSUtils.getInstance().getUtil().spawnLaser(init);
		((Entity)laser).setMetadata("laseren", new FixedMetadataValue(PortalStick.getInstance(),this));
	}
	public boolean blockCollide(Block blk) {
		if (blk.getType() == Material.STAINED_GLASS_PANE && (blk.getData() == (byte)9|| blk.getData() == (byte)4)) {
			
			
			BlockFace dir2 = Util.getGlassPaneDirection(blk);
			boolean isVertical = dir2 == null;
			if (isVertical) dir2 = BlockFace.UP;
			BlockStorage storage = new BlockStorage(blk);
			BlockStorage redstone = new BlockStorage(blk.getRelative(dir2.getOppositeFace()));
			blk.getRelative(dir2.getOppositeFace()).setType(Material.REDSTONE_BLOCK);
			this.furnaces.put(storage, redstone);
			blk.setData((byte)4);
			if (!isVertical) {
				return false;
			} 
			return true;
		}
		if (blk.getType().isSolid()) return false;
		return true;
	}

	public String getStringLocation()
	{
		Location loc = laser.getSource();
		return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	public void remove() {
		laser.remove();
	}
	@Override
	public String getMetadataName() {
		return "laseren";
	}
}
