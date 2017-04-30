package net.tangentmc.portalStick;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import lombok.Getter;
import net.tangentmc.nmsUtils.events.EntityCollideWithEntityEvent;
import net.tangentmc.nmsUtils.utils.CommandBuilder;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.commands.*;
import net.tangentmc.portalStick.components.*;
import net.tangentmc.portalStick.listeners.BlockListener;
import net.tangentmc.portalStick.listeners.EntityListener;
import net.tangentmc.portalStick.listeners.PlayerListener;
import net.tangentmc.portalStick.managers.*;
import net.tangentmc.portalStick.utils.Config;
import net.tangentmc.portalStick.utils.GravityGunRunnable;
import net.tangentmc.portalStick.utils.I18n;
import net.tangentmc.portalStick.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

@Getter
public class PortalStick extends JavaPlugin implements CommandExecutor {
	@Getter
	private static PortalStick instance;
	private Config configuration;
	private Util util;
	private ButtonManager buttonManager;
	private WireManager wireManager;
	private RegionManager regionManager;
	private LaserManager laserManager;
	private GelManager gelManager;
	private GrillManager grillManager;
	private PlayerListener playerListener;
	private BlockListener blockListener;
	private EntityListener entityListener;
	private I18n i18n;
	private FunnelBridgeManager bridgeManager;
	private HashMap<String,PortalUser> users = new HashMap<>();
	private HashMap<V10Block,Portal> portals = new HashMap<>();
	BaseCommand[] commands;
	@Override
	public void onEnable() {
		instance = this;
		buttonManager=  new ButtonManager();
		regionManager = new RegionManager();
		grillManager = new GrillManager();
		laserManager = new LaserManager();
		wireManager = new WireManager();
		gelManager = new GelManager();
		wireManager.loadAllWire();
		bridgeManager = new FunnelBridgeManager();
		playerListener = new PlayerListener();
		blockListener = new BlockListener();
		entityListener = new EntityListener();
		configuration = new Config();
		i18n = new I18n(getFile());
		util = new Util();
		new CommandBuilder("portal").withCommandExecutor(this).build();
		configuration.load();
		//Register commands
		ArrayList<BaseCommand> tmpList = new ArrayList<BaseCommand>();
		tmpList.add(new RegionToolCommand());
		tmpList.add(new SayCommand());
		tmpList.add(new SetRegionCommand());
		tmpList.add(new ReloadCommand());
		tmpList.add(new DebugCommand());
		tmpList.add(new DeleteAllCommand());
		tmpList.add(new DeleteCommand());
		tmpList.add(new HelpCommand());
		tmpList.add(new RegionListCommand());
		tmpList.add(new DeleteRegionCommand());
		tmpList.add(new FlagCommand());
		tmpList.add(new RegionInfoCommand());
		tmpList.add(new LanguageCommand());
		tmpList.add(new GetGunCommand());
		tmpList.add(new GetGravityGunCommand());
		tmpList.add(new ToggleTextureCommand());
		commands = tmpList.toArray(new BaseCommand[0]);
		new GravityGunRunnable();
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Client.POSITION,PacketType.Play.Client.POSITION_LOOK,PacketType.Play.Client.FLYING) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				if (event.getPacket().getBooleans().read(1)) {
					StructureModifier<Double> structs = event.getPacket().getDoubles();
					double vx = structs.read(0)-event.getPlayer().getLocation().getX();
					double vy = structs.read(1)-event.getPlayer().getLocation().getY();
					double vz = structs.read(2)-event.getPlayer().getLocation().getZ();
                    Collection<Entity> list = event.getPlayer().getWorld().getNearbyEntities(event.getPlayer().getLocation().add(vx,vy,vz),0.25,0.25,0.25);
					list.remove(event.getPlayer().getPassenger());
					list.remove(event.getPlayer());
                    list.stream().filter(entity1 -> Util.checkInstance(Portal.PortalFrame.class, entity1)).forEach(entity1 -> {
                        EntityCollideWithEntityEvent ev = new EntityCollideWithEntityEvent(event.getPlayer(), entity1, false, new org.bukkit.util.Vector(vx,vy,vz));
                        Bukkit.getPluginManager().callEvent(ev);
                        event.setCancelled(true);
                    });
				}
			}
		});
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String args[])
	{
		if (args.length == 0)
			args = new String[]{"help"};
		for (BaseCommand command : commands) {
			if (command.name.equalsIgnoreCase(args[0]))
				return command.run(sender, args, commandLabel);
		}
		return false;
	}

	public void onDisable() {
//		for (World w: Bukkit.getWorlds()) {
//			for (Entity en: w.getEntities()) {
//				Cube c = Util.getInstance(Cube.class, en);
//				Portal p = Util.getInstance(Portal.class, en);
//				Grill g = Util.getInstance(Grill.class, en);
//				if (p == null) p = Util.getInstance("portalobj2",en);
//				if (c != null) c.remove();
//				if (p != null) p.closePlugin();
//				if (g != null) g.close();
//			}
//		}
		bridgeManager.disableAll();
		gelManager.disableAll();
		//Reset all recievers
		laserManager.lasers.forEach(Laser::remove);
		commands = new BaseCommand[]{};
	}

	public PortalUser getUser(String name) {
		if (users.containsKey(name)) {
			return users.get(name);
		}
		PortalUser user = new PortalUser(name);
		users.put(name, user);
		return user;
	}

	public static final String PERM_CREATE_BRIDGE	= "portalstick.createbridge";
	public static final String PERM_CREATE_GRILL	= "portalstick.creategrill";
	public static final String PERM_PLACE_PORTAL	= "portalstick.placeportal";
	public static final String PERM_DELETE_ALL		= "portalstick.admin.deleteall";
	public static final String PERM_ADMIN_REGIONS	= "portalstick.admin.regions";
	public static final String PERM_DELETE_BRIDGE	= "portalstick.deletebridge";
	public static final String PERM_DELETE_GRILL	= "portalstick.deletegrill";
	public static final String PERM_DELETE_LASER	= "portalstick.deletelaser";
	public static final String PERM_GET_GUN         = "portalstick.gun";
	public static final String PERM_GET_GRAVITY_GUN = "portalstick.gravitygun";
	public static final String PERM_DAMAGE_BOOTS	= "portalstick.damageboots";
	public static final String PERM_TELEPORT 		= "portalstick.teleport";
	public static final String PERM_LANGUAGE		= "portalstick.admin.language";
	public static final String PERM_DEBUG			= "portalstick.admin.debug";
	public static final String PERM_TEXTURE        = "portalstick.admin.texture";
	public static final String PERM_SAY            = "portalstick.admin.say";
	public boolean hasPermission(Player player, String node) {
		if(player.hasPermission(node))
			return true;
		while(node.contains("."))
		{
			node = node.substring(0, node.lastIndexOf("."));
			if(player.hasPermission(node))
				return true;
			node = node.substring(0, node.length() - 1);
			if(player.hasPermission(node))
				return true;
		}
		return player.hasPermission("*");
	}

}
