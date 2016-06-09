package net.tangentmc.portalStick.listeners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.Door;

import net.tangentmc.nmsUtils.entities.NMSEntity;
import net.tangentmc.nmsUtils.events.PlayerInteractWithEntityEvent;
import net.tangentmc.nmsUtils.events.PlayerInteractWithEntityEvent.EntityUseAction;
import net.tangentmc.nmsUtils.events.PlayerPushedKeyEvent;
import net.tangentmc.nmsUtils.packets.Key;
import net.tangentmc.nmsUtils.utils.Cooldown;
import net.tangentmc.nmsUtils.utils.FaceUtil;
import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.nmsUtils.utils.V10Location;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.AutomatedPortal;
import net.tangentmc.portalStick.components.Cube;
import net.tangentmc.portalStick.components.Funnel;
import net.tangentmc.portalStick.components.Grill;
import net.tangentmc.portalStick.components.Laser;
import net.tangentmc.portalStick.components.Portal;
import net.tangentmc.portalStick.components.PortalUser;
import net.tangentmc.portalStick.components.Region;
import net.tangentmc.portalStick.components.Wire;
import net.tangentmc.portalStick.managers.RegionManager;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.RegionChangeEvent;
import net.tangentmc.portalStick.utils.RegionSetting;
import net.tangentmc.portalStick.utils.Util;
import net.tangentmc.portalStick.utils.VectorUtil;

public class PlayerListener implements Listener{
	public PlayerListener() {
		Bukkit.getPluginManager().registerEvents(this, PortalStick.getInstance());
        interactBlocks.add(Material.WALL_SIGN);
        interactBlocks.add(Material.AIR);
	}
	@EventHandler
	public void regionChange(PlayerMoveEvent evt) {
		if (new V10Location(evt.getTo()).equals(new V10Location(evt.getFrom()))) return;
		RegionManager rm = PortalStick.getInstance().getRegionManager();
		Region to = rm.getRegion(new V10Block(evt.getTo()));
		Region from = rm.getRegion(new V10Block(evt.getFrom()));
		if (from != to) {
			System.out.println("Moved between "+from +" and "+to);
			RegionChangeEvent evt2 = new RegionChangeEvent(from, to, false);
			Bukkit.getPluginManager().callEvent(evt2);
			evt.setCancelled(evt2.isCancelled());

		}
	}
    HashSet<Material> interactBlocks = new HashSet<>();
	@EventHandler
	public void use(PlayerPushedKeyEvent event) {
		if (event.getButtons().contains(Key.DROP_ITEM)) {
			if(PortalStick.getInstance().getConfiguration().DisabledWorlds.contains(event.getPlayer().getLocation().getWorld().getName()))
				return;
			PortalUser user = PortalStick.getInstance().getUser(event.getPlayer().getName());
			if (user.hasCube()) {
				Cube c = user.getCube();
				c.hold(event.getPlayer());
				event.setCancelled(true);

			} else {
				Optional<Cube> cube = event.getPlayer().getNearbyEntities(2, 2, 2).stream().filter(en -> en.hasMetadata("cuben")).filter(en -> VectorUtil.isLookingAt(event.getPlayer(),en)).map(en -> (Cube)en.getMetadata("cuben").get(0).value()).findFirst();
				if (cube.isPresent()) {
					Cube c = cube.get();
					c.hold(event.getPlayer());
					event.setCancelled(true);
				} else {
					Block target= event.getPlayer().getTargetBlock(interactBlocks, 3);
					if (target!= null) {
						switch (target.getType()) {
							case STONE_BUTTON:
							case WOOD_BUTTON:
								Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), ()->
										this.setState(target),10L);
							case LEVER:
								Bukkit.getScheduler().runTask(PortalStick.getInstance(), ()->
										this.setState(target));

								break;
							case WOODEN_DOOR:
								Door d = (Door) target.getState().getData();
								d.setOpen(!d.isOpen());
								target.getState().setData(d);;
								target.getState().update();
							default:
						}
					}
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
				Material type = event.getPlayer().getItemInHand().getType();
				byte data = event.getPlayer().getItemInHand().getData().getData();
				if (type == Material.STAINED_CLAY) {
					int changed;
					switch (data) {
						case 13:
							changed = w.addTimer();
							break;
						case 14:
							changed = w.removeTimer();
							break;
						default:
							w.cycle();
							event.getPlayer().sendMessage("[PortalStick] Right click with red and green stained clay to modify the time this timer runs for.");
							return;
					}
					event.getPlayer().sendMessage("[PortalStick] Timer time set to: "+changed+" seconds.");
					return;
				}
				HashSet<Material> tb = new HashSet<Material>();
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
					if (!evt.isCancelled() && event.getPlayer().getItemInHand().getType().isBlock()){
						BlockState b = placed.getState();
						b.setType(event.getPlayer().getItemInHand().getType());
						b.setData(event.getPlayer().getItemInHand().getData());
						BlockPlaceEvent bevt = new BlockPlaceEvent(placed, b, target, event.getPlayer().getItemInHand(), event.getPlayer(), true);
						if (!bevt.isCancelled() && bevt.canBuild()) {
							placed.setType(b.getType());
							placed.setData(b.getData().getData());
						}
					}
				} else {
					//Placing through redstone.
					if (!Util.checkInstance(Wire.class, en)) return;
					PlayerInteractEvent evt = new PlayerInteractEvent(event.getPlayer(), Action.LEFT_CLICK_BLOCK, event.getPlayer().getItemInHand(), target, FaceUtil.getDirection(placed.getLocation().toVector().subtract(target.getLocation().toVector())));
					Bukkit.getPluginManager().callEvent(evt);
					if (!evt.isCancelled()) {
						BlockBreakEvent evt2 = new BlockBreakEvent(target, event.getPlayer());
						if (!evt2.isCancelled()) {
							target.setType(Material.AIR);
						}
					}

				}
			}
		}
	}
	@EventHandler
	public void destroyEntity(PlayerInteractWithEntityEvent evt) {
		if (evt.getAction() == EntityUseAction.ATTACK) {
			Bukkit.getScheduler().runTask(PortalStick.getInstance(), () -> {
				Entity en = evt.getEntity();
				if (en.isInsideVehicle()) en = evt.getEntity().getVehicle();
				Laser s = Util.getInstance(Laser.class, en);
				if (s != null && PortalStick.getInstance().hasPermission(evt.getPlayer(), PortalStick.PERM_DELETE_LASER)) {
					PortalStick.getInstance().getLaserManager().removeLaser(s);
					return;
				}
				AutomatedPortal ap = Util.getInstance(AutomatedPortal.class, en);
				if (ap != null) {
					ap.close();
					ap.getAutoStand().remove();
				}
				Portal p = Util.getInstance(Portal.class, en);
				if (en.hasMetadata("portalobj2")) {
					p = (Portal) en.getMetadata("portalobj2").get(0).value();
				}
				if (p != null && (p.getOwner().equals(evt.getPlayer().getName()) || PortalStick.getInstance().hasPermission(evt.getPlayer(), PortalStick.PERM_DELETE_ALL))) {
                    Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(),p::delete,1L);
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

		Util.playSound(state?Sound.BUTTON_PUSHED:Sound.BUTTON_RELEASED, bloc);
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
		HashSet<Material> transparent = new HashSet<Material>();
		transparent.add(Material.GLASS);
		transparent.add(Material.AIR);
		for (Material mt: Material.values()) {
			if (Util.isTranslucent(mt)) transparent.add(mt);
		}
		PortalUser user = PortalStick.getInstance().getUser(evt.getPlayer().getName());
		if (!evt.hasItem()) return;
		if (evt.getAction() == Action.RIGHT_CLICK_BLOCK && evt.getItem().getType() == Material.NETHER_FENCE) {
			new AutomatedPortal(evt.getClickedBlock(),evt.getBlockFace());
			evt.setCancelled(true);
		}
		if (evt.getAction() == Action.RIGHT_CLICK_BLOCK && evt.getPlayer().getItemInHand().getType() == Material.REDSTONE)
		{
			BlockFace clicked = evt.getBlockFace();
			if (clicked != BlockFace.UP) {
				Bukkit.getScheduler().runTask(PortalStick.getInstance(),() -> {
					if (evt.getPlayer().getPlayer().isSneaking()) {
						PortalStick.getInstance().getWireManager().createSign(evt.getClickedBlock(),clicked);
					} else {
						PortalStick.getInstance().getWireManager().createWire(evt.getClickedBlock(),clicked);
					}
				});
				evt.setCancelled(true);
			}
		}

		PortalStick plugin = PortalStick.getInstance();
		if (user.isUsingTool() && evt.getPlayer().getItemInHand().getTypeId() == plugin.getConfiguration().RegionTool)
		{
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
		if (Util.isGravityGun(player.getItemInHand()) && Cooldown.tryCooldown(evt.getPlayer(), "gravitygun", 10l))
		{

			Region region = plugin.getRegionManager().getRegion(new V10Block(evt.getPlayer().getLocation()));
			HashSet<Byte> tb = region.getList(RegionSetting.TRANSPARENT_BLOCKS).stream().map(i -> ((Integer) i).byteValue()).collect(Collectors.toCollection(HashSet::new));
			switch (evt.getAction()) {
				//Entity
				case LEFT_CLICK_AIR:
					//TODO: play sounds
					if (user.getHeldEntity() != null) {
						((NMSEntity)user.getHeldEntity()).setFrozen(false);
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
						((NMSEntity)user.getHeldEntity()).setFrozen(false);
						user.setHeldEntity(null);
						return;
					}
					if (VectorUtil.getTargetEntity(player) != null) {
						((NMSEntity)VectorUtil.getTargetEntity(player)).setFrozen(true);
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
						((NMSEntity)user.getHeldEntity()).setFrozen(false);
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
						((NMSEntity)user.getHeldEntity()).setFrozen(false);
						user.setHeldEntity(null);
						return;
					}
					Block b2 = evt.getClickedBlock();
					FallingBlock fb2 = b2.getWorld().spawnFallingBlock(b2.getLocation().add(0.5, 1, 0.5), b2.getTypeId(), b2.getData());
					b2.setType(Material.AIR);
					((NMSEntity)fb2).setFrozen(true);
					user.setHeldEntity(fb2);
					break;
				default:
					break;

			}
		}
		if (evt.getItem().getType() == Material.FLINT_AND_STEEL) {
			if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (evt.getClickedBlock().getType() == Material.STAINED_GLASS_PANE && evt.getClickedBlock().getData()==(byte)0) {
					//Is there a laser at that spot??
					if (Util.retrieveMetadata(evt.getClickedBlock(),2,Laser.class) == null) {
						PortalStick.getInstance().getLaserManager().createLaser(new V10Block(evt.getClickedBlock()));
						PortalStick.getInstance().getConfiguration().saveAll();
						evt.setCancelled(true);
					}
				}
				if (evt.getClickedBlock().getType() == Material.STAINED_GLASS_PANE && (evt.getClickedBlock().getData()==(byte)14 || evt.getClickedBlock().getData()==(byte)15)) {
					PortalStick.getInstance().getBridgeManager().createBridge(evt.getClickedBlock());
				}
				if (evt.getClickedBlock().getType() != Material.MOSSY_COBBLESTONE) return;
				Grill grill = new Grill(evt.getClickedBlock());
				if (grill.isComplete()) {
					evt.setCancelled(true);
					PortalStick.getInstance().getConfiguration().saveAll();
				}
			}
		}

		if (!Util.isPortalGun(evt.getItem()) || !Cooldown.tryCooldown(evt.getPlayer(),"portalplace",50)) return;
		if (evt.getAction() == Action.LEFT_CLICK_BLOCK && !Util.isTranslucent(evt.getClickedBlock().getType())) {
			boolean success = user.setPrimary(evt.getClickedBlock(),FaceUtil.faceToVector(evt.getBlockFace()));
			if (success) evt.setCancelled(true);
			if (!success) evt.getPlayer().sendMessage(ChatColor.RED+"Unable to place portal there!");
		} else if (evt.getAction() == Action.RIGHT_CLICK_BLOCK && !Util.isTranslucent(evt.getClickedBlock().getType())) {
			boolean success = user.setSecondary(evt.getClickedBlock(),FaceUtil.faceToVector(evt.getBlockFace()));
			if (success) evt.setCancelled(true);
			if (!success) evt.getPlayer().sendMessage(ChatColor.RED+"Unable to place portal there!");
		} else if (evt.getAction() == Action.LEFT_CLICK_AIR || (evt.getAction() == Action.LEFT_CLICK_BLOCK && Util.isTranslucent(evt.getClickedBlock().getType()))) {
			List<Block> b = evt.getPlayer().getLastTwoTargetBlocks(transparent, 100);
			boolean success = b.size() ==2;
			if (success) success = user.setPrimary(b.get(1),b.get(0).getLocation().toVector().subtract(b.get(1).getLocation().toVector()));
			if (success) evt.setCancelled(true);
			if (!success) evt.getPlayer().sendMessage(ChatColor.RED+"Unable to place portal there!");
		} else if (evt.getAction() == Action.RIGHT_CLICK_AIR || (evt.getAction() == Action.RIGHT_CLICK_BLOCK && Util.isTranslucent(evt.getClickedBlock().getType()))) {
			List<Block> b = evt.getPlayer().getLastTwoTargetBlocks(transparent, 100);
			boolean success = b.size() ==2;
			if (success) success = user.setSecondary(b.get(1),b.get(0).getLocation().toVector().subtract(b.get(1).getLocation().toVector()));
			if (success) evt.setCancelled(true);
			if (!success) evt.getPlayer().sendMessage(ChatColor.RED+"Unable to place portal there!");
		}
	}
}
