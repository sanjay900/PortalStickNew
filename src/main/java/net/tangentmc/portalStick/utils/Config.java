package net.tangentmc.portalStick.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Bridge;
import net.tangentmc.portalStick.components.GelTube;
import net.tangentmc.portalStick.components.Grill;
import net.tangentmc.portalStick.components.Laser;
import net.tangentmc.portalStick.components.Region;


public class Config {

	private final PortalStick plugin = PortalStick.getInstance();
	private FileConfiguration mainConfig;
	private final FileConfiguration regionConfig;
	private final FileConfiguration grillConfig;
	private final FileConfiguration bridgeConfig;
	private final FileConfiguration laserConfig;
	private final FileConfiguration gelConfig;

	private final File gelConfigFile;
	private final File laserConfigFile;
	private final File regionConfigFile;
	private final File grillConfigFile;
	private final File bridgeConfigFile;

	public HashSet<String> DisabledWorlds;
	public int PortalTool;
	public short portalToolData; //Short for spout compatiblity!
	public String portalToolName, portalToolDesc;
	public String gravityGunName, gravityGunDesc;
	public Region GlobalRegion;
	public int RegionTool;
	public boolean RestoreInvOnWorldChange;
	public List<String> ColorPresets;
	public byte portalBackData;
	public String textureURL = null;
	public String defaultTextureURL = null;

	public boolean newPortals;
	public int soundRange;
	public final String[] soundNative = new String[Sound.values().length];

	public String lang;

	public boolean debug;

	public Config() {

		regionConfigFile = getConfigFile("regions.yml");
		grillConfigFile = getConfigFile("grills.yml");
		bridgeConfigFile = getConfigFile("bridges.yml");
		laserConfigFile = getConfigFile("lasers.yml");
		gelConfigFile = getConfigFile("gel.yml");

		mainConfig = plugin.getConfig();
		regionConfig = getConfig(regionConfigFile);
		grillConfig = getConfig(grillConfigFile);
		bridgeConfig = getConfig(bridgeConfigFile);
		laserConfig = getConfig(laserConfigFile);
		gelConfig = getConfig(gelConfigFile);
	}


	public void deleteGel(String gel) {
		List<String> list =  grillConfig.getStringList("gelTubes");
		list.remove(gel);
		gelConfig.set("gelTubes", list);
		saveAll();
	}
	public void deleteGrill(String grill) {
		List<String> list =  grillConfig.getStringList("grills");
		list.remove(grill);
		grillConfig.set("grills", list);
		saveAll();
	}


	public void deleteLaser(String laser) {
		List<String> list =  laserConfig.getStringList("lasers");
		list.remove(laser);
		laserConfig.set("grills", list);
		saveAll();
	}

	public void deleteRegion(String name) {
		regionConfig.set(name, null);
		saveAll();
	}

	public void deleteBridge(String bridge) {
		List<String> list = bridgeConfig.getStringList("bridges");
		list.remove(bridge);
		bridgeConfig.set("bridges", list);
		saveAll();
	}

	@SuppressWarnings("deprecation")
	public void load() {
		try {
			mainConfig = plugin.getConfig();
			regionConfig.load(regionConfigFile);
			grillConfig.load(grillConfigFile);
			bridgeConfig.load(bridgeConfigFile);
			laserConfig.load(laserConfigFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//Load main settings
		DisabledWorlds = new HashSet<String>(getStringList("main.disabled-worlds", new ArrayList<String>()));
		String[] split = getString("main.portal-tool", "280:0").split(":");
		PortalTool = Integer.parseInt(split[0]);
		if(split.length > 1)
			portalToolData = Short.parseShort(split[1]);
		else
			portalToolData = 0;
		portalToolName = ChatColor.translateAlternateColorCodes('&', getString("main.portal-tool-name", "&6Aperture Science Handheld Portal Device"));
		portalToolDesc = ChatColor.translateAlternateColorCodes('&', getString("main.portal-tool-description", "&aThanks to the ASHPD,\n&2the impossible is easy."));portalToolName = ChatColor.translateAlternateColorCodes('&', getString("main.portal-tool-name", "&6Aperture Science Handheld Portal Device"));
		gravityGunName = ChatColor.translateAlternateColorCodes('&', getString("main.gravity-gun-name", "&6Zero Point Energy Field Manipulator"));
		gravityGunDesc = ChatColor.translateAlternateColorCodes('&', getString("main.gravity-gun-description", "&aThanks to the ZPEFM,\n&2the impossible is easy."));
		RegionTool = getInt("main.region-tool", 268);
		RestoreInvOnWorldChange = getBoolean("main.restore-inventory-on-world-change", true);
		ColorPresets = getStringList("main.portal-color-presets", Arrays.asList(new String[]{"9-1","3-10","4-14"}));
		Iterator<String> iter = ColorPresets.iterator();
		debug = getBoolean("Debug", true); //TODO: True cause beta.
		newPortals = getBoolean("main.new-portals", true);
		String st;
		byte a = 0, b = a;
		boolean valid, changed = false;;

		while(iter.hasNext()) {
			st = iter.next();
			split = st.split("-");
			valid = true;
			if(split.length != 2)
				valid = false;
			if(valid)
				try {
					a = Byte.parseByte(split[0]);
					b = Byte.parseByte(split[1]);
				} catch(NumberFormatException e) {
					valid = false;
				}
			if(valid)
				valid = a > 0 && b > 0 &&
				a < 16 && b < 16 &&
				!DyeColor.getByData(a).name().contains("GRAY") &&
				!DyeColor.getByData(b).name().contains("GRAY");
				if(!valid) {
					if(debug)
						plugin.getLogger().warning("Removing invalid color preset: "+st);
					iter.remove();
					changed = true;
				}
		}
		if(changed)
			mainConfig.set("main.portal-color-presets", ColorPresets);
		split = getString("main.fill-portal-back", "-1").split(":");
		if(split.length > 1)
			portalBackData = Byte.parseByte(split[1]);
		else
			portalBackData = 0;

		//Load texture settings
		boolean useURL = getBoolean("texture.use-custom-texture", true);
		toggleTextureURL(false);
		if(!useURL)
			textureURL = null;

		//Load sound settings
		soundNative[Sound.PORTAL_CREATE_BLUE.ordinal()] = getString("sounds.minecraft.create-blue-portal", "portal.open");
		soundNative[Sound.PORTAL_CREATE_ORANGE.ordinal()] = getString("sounds.minecraft.create-orange-portal", "portal.open");
		soundNative[Sound.PORTAL_EXIT_BLUE.ordinal()] = getString("sounds.minecraft.exit-blue-portal", "ENDERMAN_TELEPORT");
		soundNative[Sound.PORTAL_EXIT_ORANGE.ordinal()] = getString("sounds.minecraft.exit-orange-portal", "ENDERMAN_TELEPORT");
		soundNative[Sound.PORTAL_CANNOT_CREATE.ordinal()] = getString("sounds.minecraft.cannot-create-portal", "portal.miss");
		soundNative[Sound.GUN_TAKE.ordinal()] = getString("sounds.minecraft.take-gun", "portal.getgun");
		soundNative[Sound.GRILL_EMANCIPATE.ordinal()] = getString("sounds.minecraft.grill-emancipate", "FIZZ");
		soundNative[Sound.FAITHPLATE_LAUNCH.ordinal()] = getString("sounds.minecraft.faith-plate-launch", "EXPLODE:0.5");
		soundNative[Sound.GEL_BLUE_BOUNCE.ordinal()] = getString("sounds.minecraft.blue-gel-bounce", "SLIME_WALK2");
		soundNative[Sound.BUTTON_UP.ordinal()] = getString("sounds.minecraft.button-up", "portal.buttonup");
		soundNative[Sound.BUTTON_DOWN.ordinal()] = getString("sounds.minecraft.button-down", "portal.buttondown");
		soundNative[Sound.BUTTON_PUSHED.ordinal()] = getString("sounds.minecraft.button-pushed", "portal.buttonpush");
		soundNative[Sound.BUTTON_RELEASED.ordinal()] = getString("sounds.minecraft.button-released", "portal.buttonrelease");
		soundNative[Sound.TICK_TOCK.ordinal()] = getString("sounds.minecraft.tick-tock", "portal.ticktock");

		soundRange = getInt("sounds.sound-range", 20);

		Locale locale = Locale.getDefault();
		lang = getString("Language", locale.getLanguage().toLowerCase()+"_"+locale.getCountry());

		//Load all current users
		//		for (Player player : plugin.getServer().getOnlinePlayers())
		//			plugin.userManager.createUser(player);

		//Load all regions
		for (String regionName : regionConfig.getKeys(false))
			if(!regionName.equals("global"))
				plugin.getRegionManager().loadRegion(regionName, null, null);
		plugin.getRegionManager().loadRegion("global", null, null);
		if(debug)
			plugin.getLogger().info((plugin.getRegionManager().regions.size()-1) + " (" + plugin.getRegionManager().regions.size() + ") region(s) loaded");

		//Validate regions
		for(Region region: plugin.getRegionManager().regions.values())
			if(!region.validateRedGel() && debug)
				plugin.getLogger().info("Inavlid red-gel-max-velocity for region \""+region.name+"\" - fixing!");

		//Load grills
		for (String grill : (grillConfig.getStringList("grills")))
			plugin.getGrillManager().loadGrill(grill);
		if(debug)
			plugin.getLogger().info(plugin.getGrillManager().grills.size() + " grill(s) loaded");
		//Load lasers
		for (String laser : (laserConfig.getStringList("lasers")))
			plugin.getLaserManager().loadLaser(laser);
		if(debug)
			plugin.getLogger().info(plugin.getLaserManager().lasers.size() + " lasers(s) loaded");
		//Load bridges
		for (String bridge : bridgeConfig.getStringList("bridges"))
			plugin.getBridgeManager().loadBridge(bridge);
		if(debug)
			plugin.getLogger().info(plugin.getBridgeManager().getBridges().size() + " bridge(s) loaded");
		//Load bridges
		for (String gelTube : gelConfig.getStringList("gelTubes"))
			plugin.getGelManager().loadGel(gelTube);
		if(debug)
			plugin.getLogger().info(plugin.getGelManager().getTubes().size() + " gel tube(s) loaded");
		saveAll();
	}

	public boolean toggleTextureURL(boolean save) {
		if(textureURL == null) {
			if(save)
				mainConfig.set("texture.use-custom-texture", true);
			textureURL = getString("texture.custom-URL", "http://tangentnetwork.tk/resourcepacks/Portal.zip");
		} else {
			if(save)
				mainConfig.set("texture.use-custom-texture", false);
			textureURL = null;
		}
		defaultTextureURL = getString("texture.default-URL", "http://tangentnetwork.tk/resourcepacks/default.zip");
		if(save)
			saveAll();
		return textureURL != null;
	}

	private int getInt(String path, int def)
	{

		if (mainConfig.get(path) == null)
			mainConfig.set(path, def);

		return mainConfig.getInt(path, def);
	}

	private String getString(String path, String def)
	{
		if (mainConfig.get(path) == null)
			mainConfig.set(path, def);

		return mainConfig.getString(path, def);
	}

	private List<String> getStringList(String path, List<String> def)
	{
		if (mainConfig.get(path) == null)
			mainConfig.set(path, def);

		return mainConfig.getStringList(path);
	}

	private boolean getBoolean(String path, Boolean def)
	{
		if (mainConfig.get(path) == null)
			mainConfig.set(path, def);

		return mainConfig.getBoolean(path, def);
	}

	public void reLoad() {
		load();
	}

	public boolean loadRegionSettings(Region region, Player player) {
		for (RegionSetting setting : RegionSetting.values()) {
			Object prop = regionConfig.get(region.name + "." + setting.getYaml());
			if (prop == null)
			{
				if(!region.settings.containsKey(setting))
					region.settings.put(setting, setting.getDefault());
			}
			else
				region.settings.put(setting, prop);
			regionConfig.set(region.name + "." + setting.getYaml(), region.settings.get(setting));
		}
		return region.updateLocation(player);
	}

	private File getConfigFile(String filename)
	{
		if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdir();

		File file = new File(plugin.getDataFolder(), filename);
		return file;
	}
	private FileConfiguration getConfig(File file) {
		FileConfiguration config = null;
		try {
			config = new YamlConfiguration();
			if (file.exists())
			{
				config.load(file);
				config.set("setup", null);
			}
			config.save(file);

			return config;
		} catch (Exception e) {
			plugin.getLogger().severe("Unable to load YAML file " + file.getAbsolutePath());
			e.printStackTrace();
		}
		return null;
	}

	public void saveAll() {

		//Save regions
		for (Map.Entry<String, Region> entry : plugin.getRegionManager().regions.entrySet()) {
			Region region = entry.getValue();
			for (Entry<RegionSetting, Object> setting : region.settings.entrySet())
				regionConfig.set(region.name + "." + setting.getKey().getYaml(), setting.getValue());
		}
		try
		{
			regionConfig.save(regionConfigFile);
		}
		catch (Exception e)
		{
			plugin.getLogger().severe("Error while writing to regions.yml");
			e.printStackTrace();
		}

		//Save grills
		grillConfig.set("grills", null);
		List<String> list = new ArrayList<String>();
		for (Grill grill : plugin.getGrillManager().grills)
			list.add(grill.getStringLocation());
		grillConfig.set("grills", list);
		try
		{
			grillConfig.save(grillConfigFile);
		}
		catch (Exception e)
		{
			plugin.getLogger().severe("Error while writing to grills.yml");
			e.printStackTrace();
		}

		//Save lasers
		laserConfig.set("lasers", null);
		list = new ArrayList<String>();
		for (Laser laser : plugin.getLaserManager().lasers)
			list.add(laser.getStringLocation());
		laserConfig.set("lasers", list);
		try
		{
			laserConfig.save(laserConfigFile);
		}
		catch (Exception e)
		{
			plugin.getLogger().severe("Error while writing to lasers.yml");
			e.printStackTrace();
		}


		//Save bridges
		bridgeConfig.set("bridges", null);
		list = new ArrayList<String>();
		for (Bridge bridge : plugin.getBridgeManager().getBridges())
			list.add(bridge.getStringLocation());
		bridgeConfig.set("bridges", list);
		try
		{
			bridgeConfig.save(bridgeConfigFile);
		}
		catch (Exception e)
		{
			plugin.getLogger().severe("Error while writing to bridges.yml");
			e.printStackTrace();
		}
		//Save gel
		gelConfig.set("gelTubes", null);
		list = new ArrayList<String>();
		for (GelTube tube : plugin.getGelManager().getTubes())
			list.add(tube.getStringLocation());
		gelConfig.set("gelTubes", list);
		try
		{
			gelConfig.save(gelConfigFile);
		}
		catch (Exception e)
		{
			plugin.getLogger().severe("Error while writing to gel.yml");
			e.printStackTrace();
		}
		//Save main
		mainConfig.set("Language", lang);
		mainConfig.set("Debug", debug);
		plugin.saveConfig();			
	}

	public enum Sound {
		PORTAL_CREATE_BLUE,
		PORTAL_CREATE_ORANGE,
		PORTAL_EXIT_BLUE,
		PORTAL_EXIT_ORANGE,
		PORTAL_CANNOT_CREATE,
		GUN_TAKE,
		GRILL_EMANCIPATE,
		FAITHPLATE_LAUNCH,
		GEL_BLUE_BOUNCE,
		BUTTON_UP,
		BUTTON_DOWN,
		BUTTON_PUSHED,
		BUTTON_RELEASED,
		TICK_TOCK
	}
}