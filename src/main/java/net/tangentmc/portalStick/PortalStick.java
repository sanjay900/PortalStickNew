package net.tangentmc.portalStick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import net.tangentmc.portalStick.commands.*;
import net.tangentmc.portalStick.components.Laser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import net.tangentmc.nmsUtils.utils.CommandBuilder;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.components.Portal;
import net.tangentmc.portalStick.components.PortalUser;
import net.tangentmc.portalStick.listeners.BlockListener;
import net.tangentmc.portalStick.listeners.EntityListener;
import net.tangentmc.portalStick.listeners.PlayerListener;
import net.tangentmc.portalStick.managers.ButtonManager;
import net.tangentmc.portalStick.managers.FunnelBridgeManager;
import net.tangentmc.portalStick.managers.GelManager;
import net.tangentmc.portalStick.managers.GrillManager;
import net.tangentmc.portalStick.managers.LaserManager;
import net.tangentmc.portalStick.managers.RegionManager;
import net.tangentmc.portalStick.managers.WireManager;
import net.tangentmc.portalStick.utils.Config;
import net.tangentmc.portalStick.utils.GravityGunRunnable;
import net.tangentmc.portalStick.utils.I18n;
import net.tangentmc.portalStick.utils.Util;
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
		for (World w: Bukkit.getWorlds()) {
			w.getEntities().stream().collect(Collectors.toList()).stream().filter(en -> Util.checkInstance(Portal.class, en)).forEach(en -> {
				Util.getInstance(Portal.class, en).delete();
			});
		}
		bridgeManager.disableAll();
		gelManager.disableAll();
		//Reset all recievers
		laserManager.lasers.forEach(Laser::remove);
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
