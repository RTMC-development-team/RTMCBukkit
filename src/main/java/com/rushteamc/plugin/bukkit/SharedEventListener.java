package com.rushteamc.plugin.bukkit;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.rushteamc.lib.SharedEventBus.Subscribe;
import com.rushteamc.plugin.common.Group;
import com.rushteamc.plugin.common.World;
import com.rushteamc.plugin.common.Events.ChangeGroupMemberEvent;
import com.rushteamc.plugin.common.Events.ChangeGroupPermissionEvent;
import com.rushteamc.plugin.common.Events.ChangePlayerPermissionEvent;
import com.rushteamc.plugin.common.Events.ChatEvent;
import com.rushteamc.plugin.common.Events.GroupChatEvent;
import com.rushteamc.plugin.common.Events.PlayerJoinWorldEvent;
import com.rushteamc.plugin.common.Events.PlayerLeaveWorldEvent;
import com.rushteamc.plugin.common.Events.RequestGroupAccessEvent;
import com.rushteamc.plugin.common.FormattedString.FormattedString;
import com.rushteamc.plugin.common.FormattedString.FormattedString.ParseErrorException;

public class SharedEventListener
{
	private final Plugin plugin;
	
	public SharedEventListener(Plugin plugin)
	{
		this.plugin = plugin;
		plugin.getAssembler().getSharedEventBus().addHandler(this);
	}

	public void deinit()
	{
		;
	}
	
	private FormattedString getGroupString(com.rushteamc.plugin.common.Player player)
	{
		Set<Group> groups = player.getMainGroup();
		if(groups.size() < 1)
			return new FormattedString();
		
		Iterator<Group> iterator = groups.iterator();
		StringBuilder result = new StringBuilder(iterator.next().getName());
		while(iterator.hasNext())
		{
			Group group = iterator.next();
			result.append(", ");
			result.append(group.getName());
		}
		
		try {
			return new FormattedString(result.toString());
		} catch (ParseErrorException e) {
			e.printStackTrace();
			return new FormattedString();
		}
	}
	
	@Subscribe
	public void onChatEvent(ChatEvent event)
	{
		try {
			FormattedString message = event.getMessage();
			FormattedString playername = new FormattedString(event.getPlayer().getDisplayName());
			FormattedString prefix = event.getPlayer().getPrefix();
			if(prefix == null)
				prefix = new FormattedString();
			FormattedString suffix = event.getPlayer().getSuffix();
			if(suffix == null)
				suffix = new FormattedString();
			FormattedString worldname = new FormattedString(event.getWorldName());
			FormattedString group = getGroupString(event.getPlayer());
			
			FormattedString result = FormattedString.Format(plugin.getChatFormat("public"), playername, prefix, suffix, worldname, group, message);
			plugin.getServer().broadcastMessage( result.toString("MC") );
		} catch (ParseErrorException e) {
			e.printStackTrace();
		}
	}

	@Subscribe
	public void onPlayerJoinEvent(String eventGroup, PlayerJoinWorldEvent event)
	{
		if(!plugin.getAssembler().getEventBusGroupName().equals(eventGroup))
			return;
		
		try {
			FormattedString message = new FormattedString();
			FormattedString playername = new FormattedString(event.getPlayer().getDisplayName());
			FormattedString prefix = event.getPlayer().getPrefix();
			if(prefix==null)
				prefix = new FormattedString();
			FormattedString suffix = event.getPlayer().getSuffix();
			if(suffix==null)
				suffix = new FormattedString();
			FormattedString worldname = new FormattedString(event.getWorldName());
			FormattedString group = getGroupString(event.getPlayer());
			
			FormattedString result = FormattedString.Format(plugin.getChatFormat("join"), playername, prefix, suffix, worldname, group, message);
			plugin.getServer().broadcastMessage( result.toString("MC") );
		} catch (ParseErrorException e) {
			e.printStackTrace();
		}
	}

	@Subscribe
	public void onPlayerLeaveEvent(String eventGroup, PlayerLeaveWorldEvent event)
	{
		if(!plugin.getAssembler().getEventBusGroupName().equals(eventGroup))
			return;
		
		try {
			FormattedString message = new FormattedString();
			FormattedString playername = new FormattedString(event.getPlayer().getDisplayName());
			FormattedString prefix = (event.getPlayer().getPrefix()==null)?new FormattedString():event.getPlayer().getPrefix();
			FormattedString suffix = (event.getPlayer().getSuffix()==null)?new FormattedString():event.getPlayer().getSuffix();
			FormattedString worldname = new FormattedString(event.getWorldName());
			FormattedString group = getGroupString(event.getPlayer());
			
			FormattedString result = FormattedString.Format(plugin.getChatFormat("leave"), playername, prefix, suffix, worldname, group, message);
			plugin.getServer().broadcastMessage( result.toString("MC") );
		} catch (ParseErrorException e) {
			e.printStackTrace();
		}
	}

	@Subscribe
	public void onUpdatePlayerPermissionEvent(String eventGroup, ChangePlayerPermissionEvent event)
	{
		plugin.getPermissionsManager().updatePermission(event.getPlayer());
	}
	
	@Subscribe
	public void onGroupChatEvent(String eventGroup, GroupChatEvent event)
	{
		if(!plugin.getAssembler().getEventBusGroupName().equals(eventGroup))
			return;

		try {
			FormattedString message = event.getMessage();
			FormattedString playername = new FormattedString(event.getPlayer().getDisplayName());
			FormattedString prefix = (event.getPlayer().getPrefix()==null)?new FormattedString():event.getPlayer().getPrefix();
			FormattedString suffix = (event.getPlayer().getSuffix()==null)?new FormattedString():event.getPlayer().getSuffix();
			FormattedString worldname = new FormattedString(event.getWorldName());
			FormattedString group = getGroupString(event.getPlayer());
			
			FormattedString format = plugin.getChatFormat(event.getGroup().getName().toLowerCase());
			if(format == null)
				format = plugin.getChatFormat("public");
						
			String result = FormattedString.Format(format, playername, prefix, suffix, worldname, group, message).toString("MC");
			
			for( Player bukkitPlayer : Bukkit.getOnlinePlayers() )
				if(new com.rushteamc.plugin.common.Player(bukkitPlayer.getName()).inGroup(event.getGroup()))
					bukkitPlayer.sendMessage(result);
			
			Bukkit.getConsoleSender().sendMessage(result);
			
		} catch (ParseErrorException e) {
			e.printStackTrace();
		}
	}
	
	@Subscribe
	public void onChangePlayerPermissionEvent(String eventGroup, ChangePlayerPermissionEvent event)
	{
		if(!plugin.getAssembler().getEventBusGroupName().equals(eventGroup))
			return;
		
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Changed permission " + event.getPermission().toString() + " for player " + event.getPlayer().getDisplayName());

		plugin.getPermissionsManager().updatePermission(event.getPlayer());
	}
	
	@Subscribe
	public void onChangeGroupMemberEvent(String eventGroup, ChangeGroupMemberEvent event)
	{
		if(!plugin.getAssembler().getEventBusGroupName().equals(eventGroup))
			return;
		
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Player " + event.getPlayer().getDisplayName() + " left of joined group " + event.getGroup().getName());
		
		plugin.getPermissionsManager().updatePermission(event.getPlayer());
	}
	
	@Subscribe
	public void onChangeGroupPermissionEvent(String eventGroup, ChangeGroupPermissionEvent event)
	{
		if(!plugin.getAssembler().getEventBusGroupName().equals(eventGroup))
			return;

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Changed permission " + event.getPermission().toString() + " for group " + event.getGroup().getName());

		for( World world : World.getWorlds() )
			for( com.rushteamc.plugin.common.Player player : world.getOnlinePlayers() )
				plugin.getPermissionsManager().updatePermission(player);
	}

	@Subscribe(instanceOf=true)
	public void onRequestGroupAccessEvent(RequestGroupAccessEvent event)
	{
		plugin.getAssembler().getAuthenticator().handleRequestGroupAccess(event.getData(), event.getKey());
	}
}
