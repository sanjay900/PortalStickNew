package net.tangentmc.portalStick.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import lombok.NoArgsConstructor;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.MetadataSaver.Metadata;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.Util;
@NoArgsConstructor
@Metadata(metadataName = "wireobj")
public class Wire implements MetadataSaver{
	public ArmorStand stand;
	public V10Block loc;
	public boolean powered = false;
	public BlockFace facing;
	public WireType type;
	public PoweredReason reason = PoweredReason.REDSTONE;
	PortalStick plugin;
	public HashSet<Wire> source = new HashSet<>();
	public HashSet<Wire> signsource = new HashSet<>();
	public ArmorStand[] stands = new ArmorStand[3];
	public Wire(Block block, BlockFace clicked,WireType type,PortalStick stick) {
		this.plugin = stick;
		this.type = type;
		facing = clicked;
		this.loc = new V10Block(block.getRelative(clicked));
		this.stand = (ArmorStand) block.getWorld().spawnEntity(rotate(loc.getHandle().getBlock().getLocation()).add(0.5, -0.95, 0.5), EntityType.ARMOR_STAND);
		NMSArmorStand.wrap(stand).lock();
		stand.setGravity(false);
		stand.setHelmet(getItemStack());
		stand.setRemoveWhenFarAway(false);
		stand.setVisible(false);
		stand.setCustomName("wire");
		stand.setCustomNameVisible(false);
		stand.setMetadata(this.getMetadataName(), new FixedMetadataValue(PortalStick.getInstance(),this));

	}
	public void createExtraWire(int amount) {
		removeExtraWire();
		stands = new ArmorStand[amount];
		for (int i = 0; i < amount; i ++){
			stands[i] = (ArmorStand) loc.getHandle().getWorld().spawnEntity(rotate(loc.getHandle().getBlock().getLocation()).add(0.5, -0.95, 0.5), EntityType.ARMOR_STAND);
			NMSArmorStand nas = NMSArmorStand.wrap(stands[i]);
            nas.lock();
			nas.setWillSave(false);
			stands[i].setGravity(false);
			stands[i].setHelmet(getItemStack());
			stands[i].setRemoveWhenFarAway(false);
			stands[i].setVisible(false);
			stands[i].setCustomNameVisible(false);
			stands[i].setMetadata(this.getMetadataName(), new FixedMetadataValue(PortalStick.getInstance(),this));
		}
	}
	public void removeExtraWire() {
		for (int i = 0; i < stands.length; i ++) {
			if (stands[i] != null)
				stands[i].remove();
			stands[i]=null;
		}
	}
	public Wire(ArmorStand en,PortalStick stick) {
		this.plugin = stick;
		stand = en;
		NMSArmorStand.wrap(stand).lock();
		stand.setMetadata(getMetadataName(), new FixedMetadataValue(PortalStick.getInstance(),this));
		this.loc = new V10Block(en.getLocation().getBlock().getRelative(BlockFace.UP));
		type = WireType.getType(stand.getHelmet().getData());
		if (stand.getHeadPose().getX()==0) {
			facing = BlockFace.DOWN;
		} else
			facing = FaceUtil.getDirection(en.getLocation().getDirection()).getOppositeFace();
		if (type == null) return;
		powered = type.getState(stand.getHelmet().getData());
		if (en.getCustomName().contains("wiret")) {
			String time = en.getCustomName().replace("wiret", "");
			if (!time.isEmpty()) {
				this.timerSeconds = Integer.parseInt(time);
			}
		}
		if (type == WireType.timer) {
			this.source.add(this);
			initTimer();
		}

	}


	public void update() {
		boolean pow = hasPower();
		if (pow && reason != PoweredReason.REDSTONE) {
			this.setPowered(true, PoweredReason.REDSTONE);
		}
		if (reason == PoweredReason.REDSTONE && !pow) {
			this.setPowered(false, PoweredReason.REDSTONE);
		}
	}
	public boolean isWire() {
		return type == WireType.CENTER||type == WireType.NORMAL;
	}
	public void updateNearby() {
		for (Wire w: getRelated().keySet()) {
			w.orient();
		}
		this.getRelated().keySet().stream().forEach(s -> source.addAll(s.source));
	}
	public Block getSupport() {
		return loc.getHandle().getBlock().getRelative(facing.getOppositeFace());
	}
	public HashMap<Wire,BlockFace> getRelated() {
		return plugin.getWireManager().getNearbyWire(this);
	}
	public Location rotate(Location loc) {
		loc.setDirection(FaceUtil.faceToVector(facing.getOppositeFace()));
		return loc;
	}
	public void setPowered(boolean powered, PoweredReason reason) {
		if (powered) {
			this.reason = reason;
			source.add(this);
		}
		else	{
			this.reason = null;
			this.source.remove(this);
		}
		this.powered = powered;
		setPowered(this);
	}
	public boolean hasPoweredSource() {
		this.source.removeAll(this.source.stream().filter(s -> s.reason == null && !s.hasPower()).collect(Collectors.toList()));
		this.source.removeAll(this.source.stream().filter(s -> s.reason == PoweredReason.REDSTONE && !s.hasPower()).collect(Collectors.toList()));
		this.source.removeAll(this.source.stream().filter(s -> !s.powered).collect(Collectors.toList()));
		return !this.source.isEmpty();
	}
	public boolean hasPower() {
		boolean pow = false;
		Block blk = getSupport();
		pow = (blk.isBlockPowered() || blk.isBlockIndirectlyPowered());
		blk=loc.getHandle().getBlock();
		if (!pow || facing != BlockFace.DOWN) {
			for (int i =-2;i<=2;i+=2) {
				if (pow) continue;
				Block blk2 = blk.getRelative(FaceUtil.rotate(facing, i));
				pow = blk2.isBlockPowered() || blk2.isBlockIndirectlyPowered();
			}

		}
		return pow;
	}
	//TODO: work out why grill + redstone makes the button no longer controll the timer correctly
	//TODO: make wireless connections store, and break when changed.
	public void setPowered(Wire origin) {
		if (origin != this)
			source.addAll(origin.source);
		this.powered = hasPoweredSource();
		if (!powered){
			for (Wire w: source) {
				w.setPowered(this);
			}
		} else if(this.hasPower() || reason == PoweredReason.GRILL){
			source.stream().filter(w -> w.type == WireType.timer).forEach(w -> {
				w.source.add(this);
				w.decreaseTimer();
			});
		}
		if (this.type == WireType.timer && powered) {
			this.reason = PoweredReason.WIRE;
			this.source.add(this);
			for (Wire w: this.source) {
				if (w != this) {
					w.source.add(this);
				}
			}
			startTimer();
		} 
		Block test = getSupport().getRelative(facing.getOppositeFace(),2);
		if (test.getType()==Material.REDSTONE_BLOCK||test.getType()==Material.EMERALD_BLOCK){
			test.setType(powered?Material.REDSTONE_BLOCK:Material.EMERALD_BLOCK);
		}



		stand.setHelmet(getItemStack());
		if (type != WireType.timer) {
			for (ArmorStand s : stands) {
				if (s!=null) s.setHelmet(getItemStack());
			}
		}
		if (getRelated() != null) {
			for (Wire w: getRelated().keySet()) {
				if (origin != w && powered != w.powered)
					w.setPowered(this);
			}
		}
		updateInRegion();
	}
	private void updateInRegion() {
		if (isIndicatorWire()) return;
		for (Wire w: plugin.getWireManager().getInRegion(loc)) {
			if (this != w && powered != w.powered&&w.type == this.type) {
				w.setPowered(this);
				w.signsource.add(this);
			}
		}
	}
	public void cycle() {
		if (!isWire()) {
			if (this.powered && type != WireType.timer) {
				for (Wire w: plugin.getWireManager().getInRegion(loc)) {
					if (this != w && w.signsource.contains(this)&&w.type != this.type) {
						w.source.remove(this);
						w.signsource.remove(this);
					}
				}
				this.powered = true;
				this.updateInRegion();
			}
			if (type == WireType.timer) {
				stopTimer();
				this.removeExtraWire();
				this.stands = new ArmorStand[3];
				type = WireType.INDICATOR;
			}
			if (Arrays.asList(WireType.values()).indexOf(type)!=WireType.values().length-1) {
				type = WireType.values()[Arrays.asList(WireType.values()).indexOf(type)+1];
			} 
			if (type == WireType.timer) {
				initTimer();
			}
			stand.setHelmet(getItemStack());
		}
	}
	public boolean isIndicatorWire() {
		return (type == WireType.CENTER||type == WireType.NORMAL||type == WireType.INDICATOR||type == WireType.timer);
	}
	public void remove() {
		for (ArmorStand as: stands) {
			if (as != null) as.remove();
		}
		stand.remove();
		plugin.getWireManager().wiresupport.get(new V10Block(this.getSupport())).remove(this);
		plugin.getWireManager().wireloc.get(loc).remove(this);
		for (Wire w: plugin.getWireManager().getNearbyWire(this.loc)) {
			w.orient();
		}
	}
	private ItemStack getItemStack() {
		return new ItemStack(type.getWireType(powered).getItemType(),1,type.getWireType(powered).getData());
	}
	public enum PoweredReason {
		REDSTONE, WIRE, GRILL;
	}
	public enum WireType {
		NORMAL(Material.EMERALD_BLOCK,Material.REDSTONE_BLOCK),
		CENTER(Material.EMERALD_ORE,Material.REDSTONE_ORE),
		INDICATOR(Material.DIAMOND_BLOCK,Material.GOLD_BLOCK),
		dots1(8),dots2(10),dots3(6),dots4(1),
		shape1(2),shape2(5),shape3(7),shape4(12),shape5(4),
		timer(14,13);
		MaterialData on;
		MaterialData off;
		WireType(Material off, Material on) {
			this.on = new MaterialData(on);
			this.off = new MaterialData(off);
		}
		WireType(int off, int on) {
			this.on = new MaterialData(Material.STAINED_CLAY,(byte)on);
			this.off = new MaterialData(Material.STAINED_CLAY,(byte)off);
		}
		WireType(int b) {
			this.on = new MaterialData(Material.STAINED_CLAY,(byte)b);
			this.off = on;
		}
		public MaterialData getWireType(boolean powered) {
			return powered?on:off;
		}
		public boolean getState(MaterialData mt) {
			return mt.getItemType() == on.getItemType() && on.getData() == mt.getData();
		}
		public static WireType getType(MaterialData mt) {
			for (WireType t:WireType.values()) {
				if ((mt.getItemType() == t.on.getItemType() && t.on.getData() == mt.getData()) || (mt.getItemType() == t.off.getItemType() && t.off.getData() == mt.getData()) ) {
					return t;
				}
			}
			return null;
		}
	}
	public BukkitTask timer;
	public BukkitTask soundTimer;
	public int timerState;
	public int timerSeconds = 8;
	public void initTimer() {
		timerState = 8;
		if (stands.length != 8) {
			removeExtraWire();
			stands = new ArmorStand[8];
		}
		stopTimer();
		double up = Math.toRadians(90);
		for (int i = 0; i < 8; i ++){
			if (stands[i] == null) {
				stands[i] = (ArmorStand) loc.getHandle().getWorld().spawnEntity(rotate(loc.getHandle().getBlock().getLocation()).add(0.5, -0.95, 0.5).add(FaceUtil.faceToVector(facing, 0.04)), EntityType.ARMOR_STAND);
				NMSArmorStand.wrap(stands[i]).lock();
				stands[i].setGravity(false);
				stands[i].setHelmet(new ItemStack(Material.PRISMARINE,0,(short)(1-i%2)));
				stands[i].setRemoveWhenFarAway(false);
				stands[i].setVisible(false);
				stands[i].setCustomName("wire2");
				stands[i].setCustomNameVisible(false);
				stands[i].setHeadPose(new EulerAngle(up, 0, Math.toRadians(90+(Math.round(i/2)*90))));
			} else {
				stands[i].setHelmet(new ItemStack(Material.PRISMARINE,0,(short)(1-i%2)));
			}
		}
	}
	public void startTimer() {
		this.reason = PoweredReason.WIRE;
		initTimer();
		timer = Bukkit.getScheduler().runTaskTimer(plugin, this::decreaseTimer,(long) ((timerSeconds/8d)*20),(long) ((timerSeconds/8d)*20));
		soundTimer = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			Util.playSound(Sound.TICK_TOCK, new V10Block(stand.getLocation()));
		}, 1l, 20l);

	}
	public void decreaseTimer() {
		this.hasPoweredSource();
		if (source.size() > 1 || hasPower()) {
			for (int i = 0; i < 8; i ++){
				timerState = 8;
				stands[i].setHelmet(new ItemStack(Material.PRISMARINE,0,(short)(1-i%2)));
			}
			return;
		}
		if (timerState == 0) {
			initTimer();
			setPowered(false, PoweredReason.WIRE);
			return;
		}
		stands[timerState-1].setHelmet(null);
		timerState--;
	}
	private void stopTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (soundTimer != null) {
			soundTimer.cancel();
			soundTimer = null;
		}

	}
	public int addTimer() {
		this.stand.setCustomName("wiret"+(++timerSeconds));
		return timerSeconds;
	}
	public int removeTimer() {
		if (timerSeconds > 1) {
			this.stand.setCustomName("wiret"+(--timerSeconds));
			return timerSeconds;
		}
		return timerSeconds;
	}
	public void orient() {
		if (type == WireType.CENTER)
			type = WireType.NORMAL;
		if (type != WireType.timer)
			removeExtraWire();
		double up = Math.toRadians(90);
		if (!isWire()) {
			stand.setHeadPose(new EulerAngle(up, 0, 0));
			return;
		}
		if (facing==BlockFace.DOWN) {
			up = 0;
		} else if (facing==BlockFace.UP) {
			up = Math.toRadians(180);
		}  
		stand.setHeadPose(new EulerAngle(up, 0, 0));
		if (!isWire()) return;
		ArrayList<BlockFace> directions = new ArrayList<BlockFace>(getRelated().values());
		int damount = directions.size();
		boolean down = facing == BlockFace.DOWN;
		if (damount == 0) {
			return;
		}
		if (damount == 2) {
			boolean vert = false;
			boolean first = true;
			boolean same = false;
			for (Entry<Wire, BlockFace> w: getRelated().entrySet()) {
				boolean testing = w.getValue() == BlockFace.SOUTH || w.getValue() == BlockFace.NORTH;
				boolean testing2 = w.getKey().facing == BlockFace.SOUTH || w.getKey().facing == BlockFace.NORTH;
				if (!down) {
					testing2 = w.getKey().facing==BlockFace.DOWN;
					testing = FaceUtil.isVertical(w.getValue());
				}
				if (first)
					vert = testing||(w.getValue()==BlockFace.SELF&&testing2);
				else {
					same =  vert == (testing||(w.getValue()==BlockFace.SELF&&testing2));
				}

				first = false;
			}
			if (same) {
				if (vert) {
					stand.setHeadPose(new EulerAngle(up, down?Math.toRadians(90):0, down?0:Math.toRadians(90)));
				}
				return;
			} 
		} else if (damount == 1) {
			for (Entry<Wire, BlockFace> w: getRelated().entrySet()) {
				boolean testing = w.getValue() == BlockFace.SOUTH || w.getValue() == BlockFace.NORTH;
				boolean testing2 = w.getKey().facing == BlockFace.SOUTH || w.getKey().facing == BlockFace.NORTH;
				if (!down) {
					testing2 = w.getKey().facing==BlockFace.DOWN;
					testing = FaceUtil.isVertical(w.getValue());
				}
				if (testing||(w.getValue()==BlockFace.SELF&&testing2)) {
					stand.setHeadPose(new EulerAngle(up, down?Math.toRadians(90):0, down?0:Math.toRadians(90)));
				} 
			}
			return;
		} else if (damount == 4) {
			type = WireType.CENTER;
			stand.setHeadPose(new EulerAngle(up, down?Math.toRadians(270):0, down?0:Math.toRadians(270)));
			createExtraWire(3);
			for (int i = 0; i < 3; i++)
				stands[i].setHeadPose(new EulerAngle(up, down?Math.toRadians(i*90):0, down?0:Math.toRadians(i*90)));
			return;
		}
		type = WireType.CENTER;
		createExtraWire(directions.size()-1);
		int counter = -1;
		ArmorStand st = stand;
		for (BlockFace b: directions) {		
			if (counter < directions.size() && counter != -1) {
				st = stands[counter];
			}
			if (facing != BlockFace.DOWN) {
				int z = 0;
				switch (b) {
				case DOWN:
					z = 270;
					break;
				case UP:
					z = 90;
					break;
				case NORTH:
				case SOUTH:
				case EAST:
				case WEST:
					if (FaceUtil.rotate(b, 2) == facing)
						z=0;
					else
						z=180;
					break;
				default:
					break;

				}
				st.setHeadPose(new EulerAngle(up, 0, Math.toRadians(z)));
			} else {

				if (b == BlockFace.WEST) {
					st.setHeadPose(new EulerAngle(up, Math.toRadians(0), 0));
				}else if (b == BlockFace.EAST) {
					st.setHeadPose(new EulerAngle(up, Math.toRadians(180), 0));
				} else if (b == BlockFace.SOUTH) {
					st.setHeadPose(new EulerAngle(up, Math.toRadians(270),0));
				} else if (b == BlockFace.NORTH) {
					st.setHeadPose(new EulerAngle(up, Math.toRadians(90),0));
				} 
			}
			counter++;
		}

	}
	@Override
	public String getMetadataName() {
		return "wireobj";
	}
}
