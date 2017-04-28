package net.tangentmc.portalStick.listeners;

import net.tangentmc.nmsUtils.events.PlayerInteractWithEntityEvent;
import net.tangentmc.nmsUtils.events.PlayerInteractWithEntityEvent.EntityUseAction;
import net.tangentmc.nmsUtils.utils.*;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.*;
import net.tangentmc.portalStick.managers.RegionManager;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.RegionChangeEvent;
import net.tangentmc.portalStick.utils.RegionSetting;
import net.tangentmc.portalStick.utils.Util;
import net.tangentmc.portalStick.utils.VectorUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlayerListener implements Listener {
    public PlayerListener() {
        Bukkit.getPluginManager().registerEvents(this, PortalStick.getInstance());
        interactBlocks.add(Material.WALL_SIGN);
        interactBlocks.add(Material.AIR);
    }

    @EventHandler
    public void quit(PlayerQuitEvent evt) {
        PortalStick.getInstance().getUser(evt.getPlayer().getName()).deletePortals();
    }
    @EventHandler
    public void vertPortalCollision(PlayerMoveEvent evt){
        Portal portal;
        Vector velocity = evt.getTo().toVector().subtract(evt.getFrom().toVector());
        for (Entity target: evt.getTo().getWorld().getNearbyEntities(evt.getTo(),2,2,2)) {
            if (!target.hasMetadata("portalobj2")) continue;
            portal = (Portal) target.getMetadata("portalobj2").get(0).value();
            if (Math.abs(velocity.lengthSquared()) > 1 && portal.getFacing().getY() > 0) {
                BlockIterator it = new BlockIterator(evt.getPlayer().getWorld(), evt.getFrom().toVector(), velocity, 0, 5);
                while (it.hasNext()) {
                    //Increase the "reach" of vertical portals based on speed
                    double d = Math.abs(velocity.lengthSquared())/2;
                    Location l = it.next().getLocation();
                    if (portal.getEntDirection() != null && (portal.getPortal().getLocation().distance(l)<d ||
                            portal.getPortal().getLocation()
                                    .add(portal.getEntDirection().clone().multiply(0.5)).distance(l) < d||
                            portal.getPortal().getLocation()
                                    .subtract(portal.getEntDirection().clone().multiply(0.5)).distance(l)<d)) {
                        portal.teleportEntity(evt.getPlayer(), evt.getTo().toVector().subtract(evt.getFrom().toVector()));
                        return;
                    }
                }
            }

        }
    }

    @EventHandler
    public void regionChange(PlayerMoveEvent evt) {
        if (new V10Location(evt.getTo()).equals(new V10Location(evt.getFrom()))) return;
        RegionManager rm = PortalStick.getInstance().getRegionManager();
        Region to = rm.getRegion(new V10Block(evt.getTo()));
        Region from = rm.getRegion(new V10Block(evt.getFrom()));
        if (from != to) {
            RegionChangeEvent evt2 = new RegionChangeEvent(from, to, false);
            Bukkit.getPluginManager().callEvent(evt2);
            evt.setCancelled(evt2.isCancelled());
            //Clear portals when you move between regions to fix issues.
            if (!evt.isCancelled()) {
                PortalStick.getInstance().getUser(evt.getPlayer().getName()).deletePortals();
            }
        }
        Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(),PortalStick.getInstance().getUser(evt.getPlayer().getName())::setCrosshair,1L);
    }
    @EventHandler
    public void gunPickup(PlayerMoveEvent event)
    {
        if(PortalStick.getInstance().getConfiguration().DisabledWorlds.contains(event.getPlayer().getLocation().getWorld().getName()))
            return;

        Optional<Entity> stand = event.getPlayer().getNearbyEntities(1, 1, 1).stream().filter(en -> en.getCustomName() != null && en.getCustomName().equals("portalstand")).findFirst();
        if (stand.isPresent()) {
            stand.get().remove();
            ItemStack gun = Util.createPortalGun();
            if (!event.getPlayer().getInventory().contains(gun))
                event.getPlayer().getInventory().addItem(gun);
            Region rg = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(event.getTo()));
            PortalUser user = PortalStick.getInstance().getUser(event.getPlayer().getName());
            user.setUpgradedGun(rg.getBoolean(RegionSetting.ENABLE_ORANGE_PORTALS));
            user.setCrosshair();
            ItemStack tmp;
            for (int i = 0;i<9;i++) {
                tmp = event.getPlayer().getInventory().getItem(i);
                if (tmp!= null && tmp.getType() == gun.getType() && tmp.getDurability() == gun.getDurability())
                {
                    event.getPlayer().getInventory().setHeldItemSlot(i);
                    event.setCancelled(true);
                    Util.playSoundTo(Sound.GUN_TAKE, event.getPlayer());
                    return;
                }

            }
        }
    }
    @SuppressWarnings("deprecation")
    @EventHandler
    public void interactEntity(PlayerInteractWithEntityEvent event) {
        if (event.getAction() != EntityUseAction.ATTACK) {
            Wire w = Util.getInstance(Wire.class, event.getEntity());
            if (w != null) {
                HashSet<Material> tb = new HashSet<>();
                tb.add(Material.AIR);
                List<Block> targetBlocks = event.getPlayer().getLineOfSight(tb, 120);
                if (targetBlocks.size() < 1)
                    return;
                Block target = targetBlocks.get(targetBlocks.size() - 1);
                Block placed = targetBlocks.get(targetBlocks.size() - 2);
                Entity en = event.getEntity();

                if (!(en instanceof ArmorStand)) return;
                if (event.getAction() != EntityUseAction.ATTACK) {
                    //Allow placing through wire and funnels, as their entities go below where they sit.
                    if (!Util.checkInstance(Wire.class, en) && !Util.checkInstance(Funnel.class, en)) return;

                    PlayerInteractEvent evt = new PlayerInteractEvent(event.getPlayer(), Action.RIGHT_CLICK_BLOCK, event.getPlayer().getItemInHand(), target, FaceUtil.getDirection(placed.getLocation().toVector().subtract(target.getLocation().toVector())));
                    Bukkit.getPluginManager().callEvent(evt);
                } else {
                    //Placing through redstone.
                    if (!Util.checkInstance(Wire.class, en)) return;
                    PlayerInteractEvent evt = new PlayerInteractEvent(event.getPlayer(), Action.LEFT_CLICK_BLOCK, event.getPlayer().getItemInHand(), target, FaceUtil.getDirection(placed.getLocation().toVector().subtract(target.getLocation().toVector())));
                    Bukkit.getPluginManager().callEvent(evt);
                }
            }
        }
    }
    HashSet<Material> interactBlocks = new HashSet<>();

    @EventHandler
    public void swapHands(PlayerSwapHandItemsEvent event) {
        if (PortalStick.getInstance().getConfiguration().DisabledWorlds.contains(event.getPlayer().getLocation().getWorld().getName()))
            return;
        event.setCancelled(true);
        PortalUser user = PortalStick.getInstance().getUser(event.getPlayer().getName());
        if (user.hasCube()) {
            Cube c = user.getCube();
            c.hold(event.getPlayer());
        } else {
            Optional<Cube> cube = event.getPlayer().getNearbyEntities(2, 2, 2).stream().filter(en -> en.hasMetadata("cuben")).filter(en -> VectorUtil.isLookingAt(event.getPlayer(), en)).map(en -> (Cube) en.getMetadata("cuben").get(0).value()).findFirst();
            if (cube.isPresent()) {
                Cube c = cube.get();
                c.hold(event.getPlayer());
            } else {
                Block target = event.getPlayer().getTargetBlock(interactBlocks, 3);
                if (target != null) {
                    if (target.getState() instanceof Door && !target.getType().name().contains("IRON")) {
                        Door d = (Door) target.getState().getData();
                        d.setOpen(!d.isOpen());
                        target.getState().setData(d);
                        target.getState().update();
                    } else {
                        switch (target.getType()) {
                            case STONE_BUTTON:
                            case WOOD_BUTTON:
                                Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), () ->
                                        this.setState(target), 10L);
                            case LEVER:
                                Bukkit.getScheduler().runTask(PortalStick.getInstance(), () ->
                                        this.setState(target));
                                break;
                            default:
                        }
                    }

                }
            }
        }

    }
    @EventHandler
    public void use(PlayerDropItemEvent evt) {
        if (Util.isPortalGun(evt.getItemDrop().getItemStack())) {
            evt.setCancelled(true);
            //Make sure hte inventory is restored before updating the crosshair
            Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(),()->
                    PortalStick.getInstance().getUser(evt.getPlayer().getName()).deletePortals(),1L);
        }
    }
    @EventHandler
    public void onPickBlock(final InventoryCreativeEvent event)
    {
        if (event.getAction() != InventoryAction.PLACE_ALL) return;
        final Player player = (Player)event.getWhoClicked();
        ItemStack item = event.getCursor();
        if (item != null && item.getType() == Material.ARMOR_STAND) {
            Entity entity = VectorUtil.getTargetEntity(player);
            if (entity != null && entity instanceof ArmorStand) {
                Wire w = Util.getInstance(Wire.class,entity);
                if (w != null) {
                    item = w.type.getWireType(false);
                    if (player.getInventory().contains(item)) {
                        ItemStack tmp;
                        for (int i = 0;i<9;i++) {
                            tmp = player.getInventory().getItem(i);
                            if (tmp!= null && tmp.getType() == item.getType() && tmp.getDurability() == item.getDurability())
                            {
                                player.getInventory().setHeldItemSlot(i);
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                    event.setCursor(item);

                }
            }
        }
    }
    @EventHandler
    public void destroyEntity(PlayerInteractWithEntityEvent evt) {
        if (evt.getAction() == EntityUseAction.ATTACK && evt.getPlayer().isOp()) {
            Bukkit.getScheduler().runTask(PortalStick.getInstance(), () -> {
                Entity en = evt.getEntity();
                if (en.isInsideVehicle()) en = evt.getEntity().getVehicle();
                AutomatedPortal ap = Util.getInstance(AutomatedPortal.class, en);
                if (ap != null) {
                    ap.close();
                    ap.getAutoStand().remove();
                }
                Wire w = Util.getInstance(Wire.class, en);
                if (w != null) {
                    w.remove();
                }
                Grill g = Util.getInstance(Grill.class, en);
                if (g != null && PortalStick.getInstance().hasPermission(evt.getPlayer(), PortalStick.PERM_DELETE_GRILL))
                    g.remove();
            });
        }
    }

    private void setState(Block block) {

        boolean state = Util.setState(block);
        V10Block bloc = new V10Block(block);

        Util.playSound(state ? Sound.BUTTON_PUSHED : Sound.BUTTON_RELEASED, bloc);
    }

    @EventHandler
    public void armorStand(PlayerArmorStandManipulateEvent evt) {
        if (evt.getRightClicked().hasMetadata("cuben")) {
            evt.setCancelled(true);
        }
    }

    @EventHandler
    public void dropItem(PlayerDropItemEvent evt) {
        PortalStick.getInstance().getUser(evt.getPlayer().getName()).addItem(evt.getItemDrop());
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void playerInteract(PlayerInteractEvent evt) {
        HashSet<Material> transparent = new HashSet<>();
        transparent.add(Material.GLASS);
        transparent.add(Material.AIR);
        for (Material mt : Material.values()) {
            if (Util.isTranslucent(mt)) transparent.add(mt);
        }
        PortalUser user = PortalStick.getInstance().getUser(evt.getPlayer().getName());
        if (!evt.hasItem()) return;
        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK && evt.getItem().getType() == Material.NETHER_FENCE) {
            new AutomatedPortal(evt.getClickedBlock(), evt.getBlockFace(),evt.getPlayer());
            evt.setCancelled(true);
        }
        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (evt.getPlayer().getItemInHand().getType() == Material.REDSTONE) {
                BlockFace clicked = evt.getBlockFace();
                if (clicked != BlockFace.UP) {
                    Bukkit.getScheduler().runTask(PortalStick.getInstance(), () -> PortalStick.getInstance().getWireManager().createWire(evt.getClickedBlock(), clicked,evt.getPlayer().getLocation().getDirection()));
                    evt.setCancelled(true);
                }

            } else {
                Wire.WireType type = Wire.WireType.getType(evt.getItem());
                if (type != null) {
                    evt.setCancelled(true);
                    BlockFace clicked = evt.getBlockFace();
                    if (evt.getBlockFace() == BlockFace.UP && type == Wire.WireType.WIRE) {
                        evt.getClickedBlock().getRelative(BlockFace.UP).setType(Material.REDSTONE_WIRE);
                        return;
                    }
                    Bukkit.getScheduler().runTask(PortalStick.getInstance(), () -> PortalStick.getInstance().getWireManager().createSign(evt.getClickedBlock(), clicked, type, evt.getPlayer().getLocation().getDirection()));
                }
            }

        }


        PortalStick plugin = PortalStick.getInstance();
        if (user.isUsingTool() && evt.getPlayer().getItemInHand().getTypeId() == plugin.getConfiguration().RegionTool) {

            switch (evt.getAction()) {
                case RIGHT_CLICK_BLOCK:
                    user.setPointTwo(new V10Block(evt.getClickedBlock()));
                    Utils.sendMessage(evt.getPlayer(), plugin.getI18n().getString("RegionPointTwoSet", evt.getPlayer().getName()));
                    evt.setCancelled(true);
                    break;
                case LEFT_CLICK_BLOCK:
                    user.setPointOne(new V10Block(evt.getClickedBlock()));
                    Utils.sendMessage(evt.getPlayer(), plugin.getI18n().getString("RegionPointOneSet", evt.getPlayer().getName()));
                    evt.setCancelled(true);
                default:
                    break;
            }
        }
        Player player = evt.getPlayer();
        //Gravity Gun tool
        if (Util.isGravityGun(player.getItemInHand()) && Cooldown.tryCooldown(evt.getPlayer(), "gravitygun", 10l)) {

            Region region = plugin.getRegionManager().getRegion(new V10Block(evt.getPlayer().getLocation()));
            HashSet<Byte> tb = region.getList(RegionSetting.TRANSPARENT_BLOCKS).stream().map(i -> ((Integer) i).byteValue()).collect(Collectors.toCollection(HashSet::new));
            switch (evt.getAction()) {
                //Entity
                case LEFT_CLICK_AIR:
                    //TODO: play sounds
                    if (user.getHeldEntity() != null) {
                        user.getHeldEntity().setGravity(true);
                        user.setHeldEntity(null);
                        return;
                    }
                    if (VectorUtil.getTargetEntity(player) != null) {
                        VectorUtil.getTargetEntity(player).setVelocity(player.getLocation().getDirection());
                        return;
                    }
                    if (player.getTargetBlock(tb, 128) != null) {
                        Block b = player.getTargetBlock(tb, 128);
                        FallingBlock fb = b.getWorld().spawnFallingBlock(b.getLocation().add(0.5, 1, 0.5), b.getTypeId(), b.getData());
                        fb.setVelocity(player.getLocation().getDirection().setY(1));
                        b.setType(Material.AIR);
                    }
                    break;
                case RIGHT_CLICK_AIR:
                    if (user.getHeldEntity() != null) {
                        user.setHeldEntity(null);
                        return;
                    }
                    if (VectorUtil.getTargetEntity(player) != null) {
                        user.setHeldEntity(VectorUtil.getTargetEntity(player));
                        return;
                    }
                    if (player.getTargetBlock(tb, 128) != null) {
                        Block b = player.getTargetBlock(tb, 128);
                        FallingBlock fb = b.getWorld().spawnFallingBlock(b.getLocation().add(0.5, 1, 0.5), b.getTypeId(), b.getData());
                        b.setType(Material.AIR);
                        user.setHeldEntity(fb);
                    }
                    break;
                //Block
                case LEFT_CLICK_BLOCK:
                    if (user.getHeldEntity() != null) {
                        user.setHeldEntity(null);
                        return;
                    }
                    Block b = evt.getClickedBlock();
                    FallingBlock fb = b.getWorld().spawnFallingBlock(b.getLocation().add(0.5, 1, 0.5), b.getTypeId(), b.getData());
                    fb.setVelocity(player.getLocation().getDirection().setY(1));
                    b.setType(Material.AIR);
                    break;
                case RIGHT_CLICK_BLOCK:
                    if (user.getHeldEntity() != null) {
                        user.setHeldEntity(null);
                        return;
                    }
                    Block b2 = evt.getClickedBlock();
                    FallingBlock fb2 = b2.getWorld().spawnFallingBlock(b2.getLocation().add(0.5, 1, 0.5), b2.getTypeId(), b2.getData());
                    b2.setType(Material.AIR);
                    user.setHeldEntity(fb2);
                    break;
                default:
                    break;

            }
        }
        if (evt.getItem().getType() == Material.FLINT_AND_STEEL) {
            if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (evt.getClickedBlock().getType() == Material.STAINED_GLASS_PANE && evt.getClickedBlock().getData() == (byte) 0) {
                    if (Util.retrieveMetadata(evt.getClickedBlock(), 2, Laser.class) == null) {
                        PortalStick.getInstance().getLaserManager().createLaser(new V10Block(evt.getClickedBlock()));
                        PortalStick.getInstance().getConfiguration().saveAll();
                        evt.setCancelled(true);
                    }
                }
                if (evt.getClickedBlock().getType() == Material.STAINED_GLASS_PANE && (evt.getClickedBlock().getData() == (byte) 14 || evt.getClickedBlock().getData() == (byte) 15)) {
                    PortalStick.getInstance().getBridgeManager().createBridge(evt.getClickedBlock());
                }
                if (evt.getClickedBlock().getType() != Material.MOSSY_COBBLESTONE) return;
                Grill grill = new Grill(new V10Block(evt.getClickedBlock()));
                if (grill.isComplete()) {
                    evt.setCancelled(true);
                    PortalStick.getInstance().getConfiguration().saveAll();
                }
            }
        }

        if (!Util.isPortalGun(evt.getItem()) || !player.hasPermission(PortalStick.PERM_PLACE_PORTAL)) return;
        evt.setCancelled(true);
        if (!Cooldown.tryCooldown(evt.getPlayer(), "portalplace", 50)) return;
        boolean pri = evt.getAction().name().contains("LEFT");
        boolean far = evt.getAction().name().contains("AIR") || Util.isTranslucent(evt.getClickedBlock().getType());
        List<Block> targetBlocks = evt.getPlayer().getLineOfSight(transparent, 120);
        for (Block b: targetBlocks) {
            Optional<Entity> e = b.getWorld().getNearbyEntities(b.getLocation(),0.5,0.5,0.5).stream().filter(en -> Util.checkInstance(Grill.class,en)).findFirst();
            if (e.isPresent()) {
                //TODO: Change grill textures, add some way to make them flash
                //Util.getInstance(Grill.class,e.get()).flash();
                LocationIterator it = new LocationIterator(evt.getPlayer().getWorld(),evt.getPlayer().getEyeLocation().add(0,-0.15,0).toVector(),evt.getPlayer().getLocation().getDirection().multiply(0.2), (int)(evt.getPlayer().getEyeLocation().distance(e.get().getLocation())/0.2));
                while(it.hasNext()) {
                    Particles.REDSTONE.display(new Particles.OrdinaryColor(pri ? 0 : 255, pri ? 255 - 140 : 150, pri ? 255 : 0), it.next(), evt.getPlayer());
                }
                Util.playSoundTo(Sound.PORTAL_CANNOT_CREATE, player);
                return;
            }
        }
        List<Block> b = evt.getPlayer().getLastTwoTargetBlocks(transparent, 100);
        Block clicked = far?(b.size()==2?b.get(1):b.get(0)):evt.getClickedBlock();
        LocationIterator it = new LocationIterator(evt.getPlayer().getWorld(),evt.getPlayer().getEyeLocation().add(0,-0.15,0).toVector(),evt.getPlayer().getLocation().getDirection().multiply(0.2), (int)(evt.getPlayer().getEyeLocation().distance(clicked.getLocation())/0.2));
        while(it.hasNext()) {
            Particles.REDSTONE.display(new Particles.OrdinaryColor(pri ? 0 : 255, pri ? 255 - 140 : 150, pri ? 255 : 0), it.next(), evt.getPlayer());
        }
        boolean success = false;

        if (!far || b.size() == 2) {
            Vector dir = FaceUtil.faceToVector(evt.getBlockFace());
            if (far) dir = b.get(0).getLocation().toVector().subtract(b.get(1).getLocation().toVector());
            if (pri) success = user.setPrimary(clicked, dir, evt.getPlayer());
            else success = user.setSecondary(clicked, dir, evt.getPlayer());
        }
        if (!success) {
            Util.playSoundTo(Sound.PORTAL_CANNOT_CREATE, player);
        } else {
            Util.playSound(pri?Sound.PORTAL_CREATE_BLUE:Sound.PORTAL_CREATE_ORANGE,new V10Block(player.getLocation()));
        }
    }
}
