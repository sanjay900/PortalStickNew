package net.tangentmc.portalStick.components;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import net.tangentmc.portalStick.utils.MetadataSaver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import lombok.NoArgsConstructor;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.MetadataSaver.Metadata;
import net.tangentmc.portalStick.utils.BlockStorage;
import net.tangentmc.portalStick.utils.Util;
@NoArgsConstructor
@Metadata(metadataName = "gelTubeobj")
public class GelTube extends BukkitRunnable implements MetadataSaver {
	Block dispBlock;
	BlockFace direction;
	ItemStack stack;
	boolean run = true;
	private static final Material[] gelBlacklist = new Material[] {
			Material.ANVIL,
			Material.STAINED_GLASS_PANE,
			Material.STAINED_GLASS,
			Material.GLASS,
			Material.THIN_GLASS,
			Material.CHEST,
			Material.FENCE,
			Material.FENCE_GATE,
			Material.NETHER_FENCE,
			Material.IRON_FENCE,
			Material.GLASS,
			Material.THIN_GLASS,
			Material.BED_BLOCK,
			Material.TRAP_DOOR,
			Material.IRON_DOOR_BLOCK,
			Material.WOODEN_DOOR,
			Material.STONE_PLATE,
			Material.WOOD_PLATE,
			Material.DISPENSER,
			Material.WORKBENCH,
			Material.FURNACE,
			Material.PISTON_BASE,
			Material.PISTON_EXTENSION,
			Material.PISTON_MOVING_PIECE,
			Material.PISTON_STICKY_BASE,
			Material.BEACON,
			Material.GLOWSTONE,
			Material.REDSTONE_LAMP_OFF,
			Material.REDSTONE_LAMP_ON,
			Material.BEDROCK,
			Material.BURNING_FURNACE,
			Material.COMMAND,
			Material.DRAGON_EGG,
			Material.ENDER_CHEST,
			Material.JACK_O_LANTERN,
			Material.JUKEBOX,
			Material.CAKE_BLOCK,
			Material.ENCHANTMENT_TABLE,
			Material.BREWING_STAND,
			Material.WALL_SIGN,
			Material.SIGN_POST
	};
	public GelTube(Block dispBlock, BlockFace direction, ItemStack stack) {
		this.direction = direction;
		this.dispBlock = dispBlock;
		this.stack = stack;
		this.runTaskTimer(PortalStick.getInstance(), 1l, 5l);
	}
	public String getStringLocation()
	{
		Location loc = dispBlock.getLocation();
		return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
	}
	Random r = new Random();
	@Override
	public void run() {
		if (!dispBlock.isBlockPowered() && !dispBlock.isBlockIndirectlyPowered()) {
			this.stop();
			run = false;
			return;
		}
		ArmorStand as = (ArmorStand) dispBlock.getWorld().spawnEntity(dispBlock.getLocation().add(FaceUtil.faceToVector(direction).add(new Vector(0.5,0,0.5))), EntityType.ARMOR_STAND);
		NMSArmorStand.wrap(as).setWillSave(false);
		if (direction == BlockFace.DOWN)
			as.setVelocity(new Vector(0,-0.00000000001,0));
		else
			as.setVelocity(FaceUtil.faceToVector(direction).multiply(r.nextInt(20)/10d));

		as.setVisible(false);
		as.setArms(false);
		as.setBasePlate(false);
        as.setRemoveWhenFarAway(false);
		as.setHelmet(stack);
		as.setMetadata(getMetadataName(), new FixedMetadataValue(PortalStick.getInstance(),this));
		as.setMetadata("sizeY", new FixedMetadataValue(PortalStick.getInstance(),0));
	}
	public void stop() {
		this.cancel();
		changedBlocks.values().forEach(BlockStorage::set);
        dispBlock.getWorld().getEntities().stream().filter(en -> Util.getInstance(this.getClass(), en) == this).forEach(Entity::remove);
	}
	@Override
	public String getMetadataName() {
		return "gelTubeobj";
	}
	HashMap<V10Block,BlockStorage> changedBlocks = new HashMap<>();
	public void groundCollide(Block ground) {
		if (!run) return;
		if (Util.retrieveMetadata(ground, 1, Laser.class) != null) return;
		//We dont want to change the dispenser itself (in the case of dispensers facing upwards)
		if (new V10Block(ground).equals(new V10Block(this.dispBlock))) return;
		if (changedBlocks.containsKey(new V10Block(ground))) return;
		if (Arrays.asList(gelBlacklist).contains(ground.getType())) return;
		changedBlocks.put(new V10Block(ground),new BlockStorage(ground));
		ground.setType(Material.WOOL);
		ground.setData(stack.getType()==Material.SAND?(byte)1:(byte)3);
	}
}
