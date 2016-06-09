package net.tangentmc.portalStick.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.tangentmc.nmsUtils.utils.Utils;
import net.tangentmc.portalStick.PortalStick;

import java.lang.reflect.InvocationTargetException;

public abstract class BaseCommand {
	protected final PortalStick plugin = PortalStick.getInstance();
	
	public final String name;
	final int argLength;
	final String usage;
	final boolean bePlayer;
	
	protected CommandSender sender;
	String[] args;
	protected Player player;
	protected String playerName;
	String usedCommand;
	
	public BaseCommand(String name, int argLength, String usage, boolean bePlayer)
	{
	  this.name = name;
	  this.argLength = argLength;
	  this.usage = usage;
	  this.bePlayer = bePlayer;
	}
	
	public boolean run(CommandSender sender, String[] preArgs, String cmd) {
		this.sender = sender;
		usedCommand = cmd;
		
		int nl = preArgs.length - 1;
		if (!(argLength == -1)&&argLength != nl) {
			sendUsage();
			return true;
		}
		
		args = new String[nl];
		System.arraycopy(preArgs, 1, args, 0, nl);
		
		if(!(sender instanceof Player))
		{
		  if(bePlayer)
		  {
			cleanup();
			return true;
		  }
		  playerName = "Console";
		}
		else
		{
		  player = (Player)sender;
		  if (!permission(player))
		  {
			cleanup();
			return false;
		  }
		  playerName = player.getName();
		}
		
		boolean ret = execute();
		cleanup();
		return ret;
	}
	
	public abstract boolean execute();
	public abstract boolean permission(Player player);
	
	void sendUsage() {
		Utils.sendMessage(sender, "&c/"+usedCommand+" " + name + " " + usage);
	}
	
	private void cleanup()
	{
	  sender = null;
	  args = null;
	  player = null;
	  usedCommand = null;
	}
}
