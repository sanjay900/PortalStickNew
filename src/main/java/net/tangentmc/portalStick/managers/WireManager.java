package net.tangentmc.portalStick.managers;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.tangentmc.nmsUtils.events.MetadataCreateEvent;
import net.tangentmc.nmsUtils.utils.MetadataSaver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Region;
import net.tangentmc.portalStick.components.Wire;
import net.tangentmc.portalStick.components.Wire.PoweredReason;
import net.tangentmc.portalStick.components.Wire.WireType;

public class WireManager implements Listener {
    private final PortalStick plugin = PortalStick.getInstance();
    public final HashMap<V10Block,ArrayList<Wire>> wiresupport = new HashMap<>();
    public final HashMap<V10Block,ArrayList<Wire>> wireloc = new HashMap<>();
    public WireManager() {
        Bukkit.getPluginManager().registerEvents(this, PortalStick.getInstance());
    }
    public void loadAllWire() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> new ArrayList<>(wiresupport.values()).forEach(w2 -> new ArrayList<>(w2).forEach(Wire::update)), 1L, 1L);
        wiresupport.values().forEach(w2 -> w2.forEach(Wire::orient));
    }
    @EventHandler
    public void wireLoad(MetadataCreateEvent evt) {
        MetadataSaver saver = evt.getMetadata();
        if (saver instanceof Wire) {
            Wire w = (Wire) saver;
            V10Block blk = new V10Block(w.getSupport());
            wiresupport.get(blk).add(w);
            wireloc.get(w.loc).add(w);
            w.orient();
            w.updateNearby();
        }
    }


    public void createWire(Block block, BlockFace facing, Vector entityDirection) {
        createWire(block,facing,WireType.WIRE,entityDirection);
    }
    public void createSign(Block block, BlockFace facing, WireType type, Vector entityDirection) {
        createWire(block,facing,type,entityDirection);
    }
    private void createWire(Block block, BlockFace facing,WireType type, Vector entityDirection) {
        V10Block blk = new V10Block(block);
        V10Block loc = new V10Block(block.getRelative(facing));
        if (!wiresupport.containsKey(blk)) wiresupport.put(blk, new ArrayList<>());
        if (!wireloc.containsKey(loc)) wireloc.put(loc, new ArrayList<>());
        if (wiresupport.get(blk).stream().anyMatch(e -> wireloc.get(loc).contains(e))) {
            return;
        }
        new Wire(block,facing,type,plugin,entityDirection);
    }
    public Set<Wire> getNearbyWire(V10Block loc) {
        return loc.getHandle().getWorld().getNearbyEntities(loc.getHandle(),2,2,2).stream().map(t -> MetadataSaver.get(t,Wire.class)).filter(Objects::nonNull).collect(Collectors.toSet());
    }
    public List<Wire> getInRegion(V10Block loc) {
        List<Wire> wire = new ArrayList<Wire>();
        Region r = plugin.getRegionManager().getRegion(loc);
        for (Entry<V10Block, ArrayList<Wire>> w: wiresupport.entrySet()) {
            if (r == plugin.getRegionManager().getRegion(w.getKey())) {
                wire.addAll(w.getValue());
            }
        }
        return wire;
    }
    public Set<Wire> getNearbyWire(Wire wire) {
        HashSet<Wire> related = new HashSet<>();
        V10Block support = new V10Block(wire.getSupport());
        for (Wire w : wiresupport.get(support)) {
            if (w == wire) continue;
            if (w.facing == wire.facing.getOppositeFace()) continue;
            if (w.loc == wire.loc) continue;
            related.add(w);
        }
        for (BlockFace face:FaceUtil.BLOCK_SIDES) {
            V10Block support2 = wire.loc.getRelative(face);
            if (wireloc.containsKey(support2)) {
                for (Wire w : wireloc.get(support2)){
                    if (w.facing != wire.facing) continue;
                    related.add(w);
                }
            }
        }
        if (wireloc.containsKey(wire.loc)) {
            if (wire.facing != BlockFace.DOWN) {
                for (Wire w : wireloc.get(wire.loc)) {
                    if (w == wire) continue;
                    related.add(w);
                }
            } else {
                for (Wire w : wireloc.get(wire.loc)) {
                    if (w == wire || w.facing == wire.facing.getOppositeFace()) continue;
                    related.add(w);
                }
            }
        }
        return related;
    }
    public Set<BlockFace> getConnections(Wire wire) {
        HashSet<BlockFace> nearby = new HashSet<>();
        V10Block support = new V10Block(wire.getSupport());
        for (Wire w : wiresupport.get(support)) {
            if (w == wire) continue;
            if (w.facing == wire.facing.getOppositeFace()) continue;
            if (w.loc == wire.loc) continue;
            Vector v = wire.getSupport().getLocation().toVector().subtract(w.loc.getHandle().toVector());
            BlockFace face =FaceUtil.getDirection(v).getOppositeFace();
            nearby.add(face);
        }
        for (BlockFace face:FaceUtil.BLOCK_SIDES) {
            if (nearby.contains(face)) continue;;
            V10Block support2 = wire.loc.getRelative(face);
            if (wireloc.containsKey(support2)) {
                for (Wire w : wireloc.get(support2)){
                    if (w.facing != wire.facing) continue;
                    nearby.add(face);
                }
            }
        }
        if (wireloc.containsKey(wire.loc)) {
            if (wire.facing != BlockFace.DOWN) {
                for (Wire w : wireloc.get(wire.loc)) {
                    if (w == wire) continue;
                    if (w.facing == BlockFace.DOWN) {
                        nearby.add(BlockFace.UP);
                        continue;
                    }
                    Vector v = wire.loc.getHandle().toVector().subtract(w.getSupport().getLocation().toVector());
                    BlockFace face = FaceUtil.getDirection(v).getOppositeFace();
                    nearby.add(face);
                }
            } else {
                for (Wire w : wireloc.get(wire.loc)) {
                    if (w == wire || w.facing == wire.facing.getOppositeFace()) continue;
                    if (w.facing == BlockFace.DOWN) {
                        nearby.add(BlockFace.UP);
                        continue;
                    }
                    Vector v = wire.loc.getHandle().toVector().subtract(w.getSupport().getLocation().toVector());
                    BlockFace face = FaceUtil.getDirection(v).getOppositeFace();
                    nearby.add(face);
                }
            }
        }
        return nearby;
    }
    public void powerBlock(Block block, boolean power, PoweredReason reason) {
        if (block.getType() == Material.REDSTONE_WIRE) {
            if (block.getRelative(BlockFace.DOWN,2).getType() == Material.LEVER) {
                return;
            }
        }
        if (block.getType() == Material.LEVER) {
            return;
        }
        if (this.wiresupport.containsKey(new V10Block(block))) {
            Iterator<Wire> it = this.wiresupport.get(new V10Block(block)).stream().iterator();
            while (it.hasNext()) {
                it.next().setPowered(power, reason);
            }
        }
        if (block.getType() == Material.REDSTONE_WIRE) return;
        if (this.wireloc.containsKey(new V10Block(block))) {
            for (Wire wire : this.wireloc.get(new V10Block(block))) {
                wire.setPowered(power, reason);
            }
        }
    }
    public void blockBreak(Block block) {
        if (this.wiresupport.containsKey(new V10Block(block))) {
            new ArrayList<>(this.wiresupport.get(new V10Block(block))).forEach(Wire::remove);
        }
    }

    public void blockUpdate(Block block) {
        getNearbyWire(new V10Block(block)).forEach(Wire::orient);
    }
}
