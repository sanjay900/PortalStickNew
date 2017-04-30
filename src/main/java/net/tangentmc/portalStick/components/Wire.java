package net.tangentmc.portalStick.components;

import net.tangentmc.nmsUtils.NMSUtils;
import net.tangentmc.nmsUtils.entities.NMSArmorStand;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.MetadataSaver;
import net.tangentmc.nmsUtils.utils.MetadataSaver.Metadata;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.Util;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
@Metadata(metadataName = "wireobj")
public class Wire extends MetadataSaver {
    public ArmorStand stand;
    public V10Block loc;
    public boolean powered = false;
    public BlockFace facing;
    public WireType type;
    public PoweredReason reason = PoweredReason.REDSTONE;
    PortalStick plugin;
    public HashSet<Wire> source = new HashSet<>();
    public HashSet<Wire> signsource = new HashSet<>();
    public Wire(Block block, BlockFace clicked,WireType type,PortalStick stick, Vector direction) {
        this.plugin = stick;
        this.type = type;
        facing = clicked;
        this.loc = new V10Block(block.getRelative(clicked));
        this.stand = (ArmorStand) block.getWorld().spawnEntity(rotate(loc.getHandle().getBlock().getLocation(),direction).add(0.5, -0.94, 0.5), EntityType.ARMOR_STAND);
        NMSArmorStand nas = NMSArmorStand.wrap(stand);
        nas.lock();
        stand.setGravity(false);
        stand.setHelmet(getItemStack());
        stand.setRemoveWhenFarAway(false);
        stand.setVisible(false);
        stand.setHeadPose(new EulerAngle(facing==BlockFace.DOWN?0:facing==BlockFace.UP?Math.toRadians(180):Math.toRadians(90),0,0));
        stand.setAI(false);
        stand.setSilent(true);

        updateNearby();
        powered = this.hasPoweredSource();
        orient();
        if (type == WireType.TIMER) {
            initTimer();
        }
        initMetadata(stand);
    }
    public Location rotate(Location loc, Vector entityDirection) {
        if (FaceUtil.isVertical(facing) && isSign()) {
            loc.setDirection(FaceUtil.faceToVector(FaceUtil.getDirection(entityDirection.setY(0),false)));
        } else {
            loc.setDirection(FaceUtil.faceToVector(facing.getOppositeFace()));
        }
        return loc;
    }
    public Wire(Entity en) {
        this.plugin = PortalStick.getInstance();
        stand = (ArmorStand) en;
        NMSArmorStand.wrap(stand).lock();
        this.loc = new V10Block(en.getLocation().getBlock().getRelative(BlockFace.UP));
        type = WireType.getType(stand.getHelmet());
        if (stand.getHeadPose().getX()==0) {
            facing = BlockFace.DOWN;
        } else if (stand.getHeadPose().getX()==Math.toRadians(180)) {
            facing = BlockFace.UP;
        } else {
            facing = FaceUtil.getDirection(en.getLocation().getDirection()).getOppositeFace();
        }
        if (type == null) return;
        powered = type.getState(stand.getHelmet());
        if (type == WireType.TIMER) {
            this.source.add(this);
            initTimer();
        }
        initMetadata(stand);
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
    public void updateNearby() {
        getRelated().forEach(s -> {
            s.orient();
            source.addAll(s.source);
        });
    }
    public Block getSupport() {
        return loc.getHandle().getBlock().getRelative(facing.getOppositeFace());
    }
    public Set<Wire> getRelated() {
        return plugin.getWireManager().getNearbyWire(this);
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
        Block blk = getSupport();
        boolean pow = blk.isBlockPowered();
        blk = loc.getHandle().getBlock();
        pow = pow || blk.isBlockPowered() || blk.isBlockIndirectlyPowered();
        if (pow) {
            if (loc.getHandle().getBlock().getRelative(BlockFace.DOWN,2).getType() == Material.LEVER) {
                return false;
            }
            for (BlockFace b: FaceUtil.BLOCK_SIDES) {
                if (loc.getHandle().getBlock().isBlockFacePowered(b)||loc.getHandle().getBlock().isBlockFaceIndirectlyPowered(b)) {
                    if (loc.getHandle().getBlock().getRelative(b.getOppositeFace()).getRelative(BlockFace.DOWN,2).getType() == Material.LEVER) {
                        return false;
                    }
                }
            }
        }
        return pow;
    }
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
            source.stream().filter(w -> w.type == WireType.TIMER).forEach(w -> {
                w.source.add(this);
                w.decreaseTimer();
            });
        }
        if (this.type == WireType.TIMER && powered) {
            this.reason = PoweredReason.WIRE;
            this.source.add(this);
            this.source.stream().filter(w -> w != this).forEach(w -> w.source.add(this));
            startTimer();
        }
        Block test = getSupport().getRelative(facing.getOppositeFace(),2);
        if (test.getType()==Material.REDSTONE_BLOCK||test.getType()==Material.EMERALD_BLOCK){
            test.setType(powered?Material.REDSTONE_BLOCK:Material.EMERALD_BLOCK);
        }
        if (getRelated() != null) {
            getRelated().stream().filter(w -> origin != w && powered != w.powered).forEach(w -> w.setPowered(this));
        }
        updateInRegion();
        orient();
    }
    private void updateInRegion() {
        if (isIndicatorWire()) return;
        plugin.getWireManager().getInRegion(loc).stream().filter(w -> this != w && powered != w.powered && w.type == this.type).forEach(w -> {
            w.setPowered(this);
            w.signsource.add(this);
        });
    }
    public boolean isIndicatorWire() {
        return (type == WireType.WIRE||type == WireType.TIMER ||type == WireType.INDICATOR);
    }
    public boolean isSign() {
        return (type == WireType.TIMER ||type == WireType.INDICATOR||type.name().startsWith("dots")||type.name().startsWith("shape"));
    }
    public void remove() {
        stand.remove();
        plugin.getWireManager().wiresupport.get(new V10Block(this.getSupport())).remove(this);
        plugin.getWireManager().wireloc.get(loc).remove(this);
        for (Wire w: plugin.getWireManager().wireloc.get(loc)) {
            if (new V10Block(this.getSupport()).equals(new V10Block(w.getSupport()))) {
                w.remove();
            }
        }
        plugin.getWireManager().getNearbyWire(this.loc).forEach(Wire::orient);
    }
    private ItemStack getItemStack() {
        if (type == WireType.TIMER) {
            if (timerState < 1) {
                return WireType.TIMER.offStack;
            }
            return WireType.valueOf("timer"+timerState).onStack;
        }
        if (type==WireType.INDICATOR) {
            getSupport().getRelative(facing.getOppositeFace()).setType(powered?Material.REDSTONE_BLOCK:Material.AIR);
            getSupport().getRelative(facing.getOppositeFace()).getState().update();
        }
        if (type == WireType.WIRE){
            Set<BlockFace> directions = plugin.getWireManager().getConnections(this);
            boolean ceil = FaceUtil.isVertical(facing);
            boolean left = false;
            boolean right = false;
            boolean up = false;
            boolean down = false;

            if (ceil) {
                if (canConnect(BlockFace.SOUTH) || directions.contains(BlockFace.SOUTH)) down = true;
                if (canConnect(BlockFace.NORTH) || directions.contains(BlockFace.NORTH)) up = true;
                if (canConnect(BlockFace.WEST) || directions.contains(BlockFace.WEST)) right = true;
                if (canConnect(BlockFace.EAST)|| directions.contains(BlockFace.EAST)) left = true;
            } else {
                if (loc.getHandle().getBlock().getType() == Material.REDSTONE_WIRE
                        || canConnect(BlockFace.DOWN) || directions.contains(BlockFace.DOWN)) down = true;
                if (directions.contains(BlockFace.UP) || canConnect(BlockFace.UP)) up = true;
                if (canConnect(FaceUtil.rotate(facing, -2)) || directions.contains(FaceUtil.rotate(facing, -2)))
                    right = true;
                if (canConnect(FaceUtil.rotate(facing, 2)) || directions.contains(FaceUtil.rotate(facing, 2)))
                    left = true;
                for (BlockFace b : FaceUtil.BLOCK_SIDES) {
                    if (b == facing || b == facing.getOppositeFace()) continue;
                    if (canConnect(getSupport().getRelative(b).getType()) && getSupport().getRelative(b).getType() != Material.REDSTONE_WIRE) {
                        getSupport().getRelative(b).getRelative(facing.getOppositeFace()).setType(powered ? Material.REDSTONE_BLOCK : Material.AIR);
                        getSupport().getRelative(b).getRelative(facing.getOppositeFace()).getState().update();
                    }

                }
            }

            return types.get(left,up,right,down).getWireType(powered);

        }
        return type.getWireType(powered);
    }
    public boolean canConnect(BlockFace dir) {
        return canConnect(getSupport().getRelative(dir).getType())||
                canConnect(getSupport().getRelative(facing).getRelative(dir).getType());
    }
    public boolean canConnect(Material mt) {
        switch (mt) {
            case REDSTONE_WIRE:
            case REDSTONE_BLOCK:
            case DIODE_BLOCK_OFF:
            case DIODE_BLOCK_ON:
            case ACACIA_DOOR:
            case BIRCH_DOOR:
            case DARK_OAK_DOOR:
            case IRON_DOOR:
            case JUNGLE_DOOR:
            case SPRUCE_DOOR:
            case TRAP_DOOR:
            case WOOD_DOOR:
            case WOODEN_DOOR:
            case IRON_TRAPDOOR:
            case LEVER:
            case STONE_BUTTON:
            case WOOD_BUTTON:
            case PISTON_BASE:
            case PISTON_STICKY_BASE:
            case ACACIA_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
            case FENCE_GATE:
            case JUNGLE_FENCE_GATE:
            case SPRUCE_FENCE_GATE:
            case DROPPER:
            case GOLD_PLATE:
            case IRON_PLATE:
            case STONE_PLATE:
            case WOOD_PLATE:
            case REDSTONE_TORCH_OFF:
            case REDSTONE_COMPARATOR_OFF:
            case REDSTONE_COMPARATOR_ON:
            case REDSTONE_TORCH_ON:
            case REDSTONE_LAMP_ON:
            case REDSTONE_LAMP_OFF:
            case HOPPER:
            case POWERED_RAIL:
            case DISPENSER:
                return true;
            default:
                return false;
        }
    }
    private static MultiKeyMap<Boolean,WireType> types = new MultiKeyMap<>();
    static {
        types.put(false,false,false,false, WireType.DOT);
        types.put(true,false,false,false, WireType.LEFT);
        types.put(false,true,false,false, WireType.UP);
        types.put(false,false,true,false, WireType.RIGHT);
        types.put(false,false,false,true, WireType.DOWN);
        types.put(true,false,true,false, WireType.LEFT_RIGHT);
        types.put(false,true,false,true, WireType.UP_DOWN);
        types.put(true,true,false,false, WireType.LEFT_UP);
        types.put(false,true,true,false, WireType.RIGHT_UP);
        types.put(false,false,true,true, WireType.RIGHT_DOWN);
        types.put(true,false,false,true, WireType.LEFT_DOWN);
        types.put(true,true,false,true, WireType.LEFT_UP_DOWN);
        types.put(true,true,true,false, WireType.LEFT_RIGHT_UP);
        types.put(false,true,true,true, WireType.RIGHT_UP_DOWN);
        types.put(true,false,true,true, WireType.LEFT_RIGHT_DOWN);
        types.put(true,true,true,true, WireType.ALL);

    }
    public enum PoweredReason {
        REDSTONE, WIRE, GRILL
    }
    private static final String REDSTONE_PREFIX = "item/redstone/";
    private static final String WALL_PREFIX = REDSTONE_PREFIX +"wall/";
    private static final String COUNTDOWN_PREFIX = REDSTONE_PREFIX +"countdown/";
    private static final String DOTS_PREFIX = REDSTONE_PREFIX+"signage_overlay_dots";
    public enum WireType {
        WIRE(WALL_PREFIX +"wire"),INDICATOR(REDSTONE_PREFIX +"indicator_checked", REDSTONE_PREFIX +"indicator_unchecked"),
        DOT(WALL_PREFIX +"none",true), LEFT(WALL_PREFIX +"l",true),UP(WALL_PREFIX +"u",true),RIGHT(WALL_PREFIX +"r",true),
        DOWN(WALL_PREFIX +"d",true),LEFT_RIGHT(WALL_PREFIX +"lr",true),UP_DOWN(WALL_PREFIX +"ud",true),
        LEFT_UP(WALL_PREFIX +"lu",true),RIGHT_UP(WALL_PREFIX +"ru",true),RIGHT_DOWN(WALL_PREFIX +"rd",true),
        LEFT_DOWN(WALL_PREFIX +"ld",true),LEFT_UP_DOWN(WALL_PREFIX +"lud",true),LEFT_RIGHT_UP(WALL_PREFIX +"lru",true),
        RIGHT_UP_DOWN(WALL_PREFIX +"rud",true),LEFT_RIGHT_DOWN(WALL_PREFIX +"lrd",true),ALL(WALL_PREFIX +"all",true),
        dots1(DOTS_PREFIX +"1"),dots2(DOTS_PREFIX +"2"), dots3(DOTS_PREFIX +"3"),dots4(DOTS_PREFIX +"4"),
        shape1(REDSTONE_PREFIX +"shape01"), shape2(REDSTONE_PREFIX +"shape02"), shape3(REDSTONE_PREFIX +"shape03"),
        shape4(REDSTONE_PREFIX +"shape04"), shape5(REDSTONE_PREFIX +"shape05"), TIMER(COUNTDOWN_PREFIX +"off"),
        timer1(COUNTDOWN_PREFIX +"1"),timer2(COUNTDOWN_PREFIX +"2"), timer3(COUNTDOWN_PREFIX +"3"),
        timer4(COUNTDOWN_PREFIX +"4"),timer5(COUNTDOWN_PREFIX +"5"), timer6(COUNTDOWN_PREFIX +"6"),
        timer7(COUNTDOWN_PREFIX +"7"),timer8(COUNTDOWN_PREFIX +"8");
        String off;
        String on;
        //Looking up this data is expensive, so lets cache it.
        ItemStack onStack;
        ItemStack offStack;
        WireType(String on, String off) {
            this.on = on;
            this.off = off;
            this.onStack = NMSUtils.getInstance().getResourcePackAPI().getItemStack(on);
            this.offStack = NMSUtils.getInstance().getResourcePackAPI().getItemStack(off);
        }
        WireType(String base, boolean b) {
            this(base+"_on",base);
        }
        WireType(String b) {
            this(b,b);
        }
        public ItemStack getWireType(boolean powered) {
            if (this == WireType.WIRE) return new ItemStack(Material.REDSTONE);
            return powered?onStack:offStack;
        }
        public boolean getState(ItemStack mt) {
            return compareStack(onStack,mt);
        }
        public static WireType getType(ItemStack it) {
            if (!it.hasItemMeta() || !it.getItemMeta().getItemFlags().contains(ItemFlag.HIDE_UNBREAKABLE)) return null;
            for (WireType t:WireType.values()) {
                if (compareStack(t.onStack, it) || compareStack(t.offStack, it)) {
                    if (t == DOT || t.name().contains("LEFT")|| t.name().contains("UP")|| t.name().contains("RIGHT")|| t.name().contains("DOWN")) {
                        return WireType.WIRE;
                    }
                    if (t.name().contains("timer")) {
                        return WireType.TIMER;
                    }
                    return t;
                }
            }
            return null;
        }
    }
    private static boolean compareStack(ItemStack first, ItemStack second) {
        return first.getDurability() == second.getDurability() && first.getType() == second.getType();
    }
    public BukkitTask timer;
    public BukkitTask soundTimer;
    public int timerState;
    public void initTimer() {
        timerState = -1;
        orient();
        stopTimer();
    }
    public void startTimer() {
        this.reason = PoweredReason.WIRE;
        initTimer();
        timerState = 8;
        orient();
        int timerSeconds = 8;
        Block sign = getSupport().getRelative(facing.getOppositeFace());
        if (sign.getType() == Material.WALL_SIGN) {
            Sign s = (Sign) sign.getState();
            timerSeconds = Integer.parseInt(s.getLine(0));
        }
        timer = Bukkit.getScheduler().runTaskTimer(plugin, this::decreaseTimer,(long) ((timerSeconds/8d)*20),(long) ((timerSeconds/8d)*20));
        soundTimer = Bukkit.getScheduler().runTaskTimer(plugin, () -> Util.playSound(Sound.TICK_TOCK, new V10Block(stand.getLocation())), 1l, 20l);

    }
    public void decreaseTimer() {
        this.hasPoweredSource();
        if (source.size() > 1 || hasPower()) {
            timerState = 8;
            orient();
            return;
        }
        if (timerState < 1) {
            initTimer();
            setPowered(false, PoweredReason.WIRE);
            return;
        }
        orient();
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
    public void orient() {
        this.stand.setHelmet(getItemStack());
        if (powered) {
            if (loc.getHandle().getBlock().getType() == Material.REDSTONE_WIRE && loc.getHandle().getBlock().getData() == (byte)0) {
                loc.getHandle().getBlock().getRelative(BlockFace.DOWN,2).setType(Material.LEVER);
                loc.getHandle().getBlock().getRelative(BlockFace.DOWN,2).setData((byte)8);
                loc.getHandle().getBlock().getRelative(BlockFace.DOWN,2).getState().update();
            }
        } else {
            if (loc.getHandle().getBlock().getType() == Material.REDSTONE_WIRE) {
                loc.getHandle().getBlock().getRelative(BlockFace.DOWN,2).setType(Material.AIR);
            }
        }
    }

}
