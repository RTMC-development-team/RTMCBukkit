package com.rushteamc.plugin.bukkit;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.rushteamc.plugin.common.Player;
import com.rushteamc.plugin.common.Events.ChatEvent;
import com.rushteamc.plugin.common.Events.PlayerJoinWorldEvent;
import com.rushteamc.plugin.common.Events.PlayerLeaveWorldEvent;
import com.rushteamc.plugin.common.Events.SetOnlinePlayersEvent;
import com.rushteamc.plugin.common.FormattedString.FormattedString;
import com.rushteamc.plugin.common.FormattedString.FormattedString.ParseErrorException;

public class BukkitEventListener implements Listener
{
	private final Plugin plugin;
	
	public BukkitEventListener(Plugin plugin)
	{
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void deinit()
	{
		;
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerJoinEvent(PlayerJoinEvent event)
	{
		Player player = new Player(event.getPlayer().getName());
		World world = event.getPlayer().getWorld();
		
		plugin.getAssembler().publisSecureEvent(new SetOnlinePlayersEvent(world.getName(), getOnlinePlayerList(world, player, true)));
		plugin.getAssembler().publisSecureEvent(new PlayerJoinWorldEvent(player, world.getName()));
		plugin.getPermissionsManager().addAttachment(player);

		plugin.getPermissionsManager().updatePermission(player);
		
		event.setJoinMessage(null);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerQuitEvent(PlayerQuitEvent event)
	{
		Player player = new Player(event.getPlayer().getName());
		World world = event.getPlayer().getWorld();
		
		plugin.getAssembler().publisSecureEvent(new SetOnlinePlayersEvent(world.getName(), getOnlinePlayerList(world, player, false)));
		plugin.getAssembler().publisSecureEvent(new PlayerLeaveWorldEvent(player, event.getPlayer().getWorld().getName()));
		plugin.getPermissionsManager().removeAttachment(player);
		event.setQuitMessage(null);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerKickEvent(PlayerKickEvent event)
	{
		Player player = new Player(event.getPlayer().getName());
		World world = event.getPlayer().getWorld();
		
		plugin.getAssembler().publisSecureEvent(new SetOnlinePlayersEvent(world.getName(), getOnlinePlayerList(world, player, false)));
		plugin.getAssembler().publisSecureEvent(new PlayerLeaveWorldEvent(player, event.getPlayer().getWorld().getName()));
		plugin.getPermissionsManager().removeAttachment(player);
		event.setLeaveMessage(null);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event)
	{
		Player player = new Player(event.getPlayer().getName());
		
		plugin.getAssembler().publisSecureEvent(new SetOnlinePlayersEvent(event.getFrom().getName(), getOnlinePlayerList(event.getFrom(), player, false)));
		plugin.getAssembler().publisSecureEvent(new SetOnlinePlayersEvent(event.getPlayer().getWorld().getName(), getOnlinePlayerList(event.getPlayer().getWorld(), player, true)));
	}
	
	private Set<Player> getOnlinePlayerList(final World world, final Player player, final boolean login)
	{
		Set<Player> onlinePlayers = new HashSet<Player>();
		for( org.bukkit.entity.Player bukkitPlayer : world.getPlayers() )
			onlinePlayers.add(new Player(bukkitPlayer.getName()));
		if(player != null)
		{
			if(login)
				onlinePlayers.add(player);
			else
				onlinePlayers.remove(player);
			/*
			boolean inList = onlinePlayers.contains(player);
			if(login && !inList)
			{
				System.out.println("Force adding player " + player.getDisplayName() + " to the online list");
				onlinePlayers.add(player);
			}
			else if(!login && inList)
			{
				System.out.println("Force removing player " + player.getDisplayName() + " from the online list");
				onlinePlayers.remove(player);
			}
			*/
		}
		return onlinePlayers;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onAsyncPlayerChatEvent(final AsyncPlayerChatEvent event)
	{
		Thread t = new Thread() {
			public void run()
			{
				Player player = new Player(event.getPlayer().getName());
				FormattedString message;
				try {
					message = new FormattedString("MC", event.getMessage());
				} catch (ParseErrorException e) {
					e.printStackTrace();
					message = new FormattedString();
				}
				plugin.getAssembler().getSharedEventBus().postGroupEvent(plugin.getAssembler().getEventBusGroupName(), new ChatEvent(player, event.getPlayer().getWorld().getName(), message));
			}
		};
		t.start();
		event.setCancelled(true);
	}
}
