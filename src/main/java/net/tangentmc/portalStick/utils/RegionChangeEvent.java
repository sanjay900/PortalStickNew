package net.tangentmc.portalStick.utils;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.tangentmc.portalStick.components.Region;
@Getter
@Setter
@AllArgsConstructor
public class RegionChangeEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	Region from;
	Region to;
	boolean cancelled;
	public static HandlerList getHandlerList() {      
		return handlers;  
	}
	@Override
	public HandlerList getHandlers() {
		return handlers; 
	}
}
