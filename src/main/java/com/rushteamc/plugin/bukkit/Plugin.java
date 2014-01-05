package com.rushteamc.plugin.bukkit;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.rushteamc.plugin.common.Assembler;
import com.rushteamc.plugin.common.Group;
import com.rushteamc.plugin.common.Player;
import com.rushteamc.plugin.common.World;
import com.rushteamc.plugin.common.Authentication.Authenticator;
import com.rushteamc.plugin.common.Authentication.GroupAccessChecker;
import com.rushteamc.plugin.common.Events.GroupChatEvent;
import com.rushteamc.plugin.common.FormattedString.FormattedString;
import com.rushteamc.plugin.common.FormattedString.FormattedString.ParseErrorException;
import com.rushteamc.plugin.common.Permissions.Permission;

public class Plugin extends JavaPlugin
{
	private Assembler assembler;
	private PermissionsManager permissionsManager;
	private BukkitEventListener bukkitEventListener;
	private SharedEventListener sharedEventListener;
	
	private Map<String, FormattedString> chatFormats = new HashMap<String, FormattedString>();
	private Group adminChatGroup;
	private boolean handleList = false;
	private Handler logHandler;
	private Logger loggerGlobal;
	
	public void onEnable()
	{
		FileConfiguration config = getConfig();
		
		ConfigurationSection configDatabase = config.getConfigurationSection("database");
		String host = configDatabase.getString("host");
		if(configDatabase.isInt("port"))
			host += ":" + configDatabase.getInt("port");
		
		byte[] publicKey = null; // TODO: Read from local file, if fails read from rushteamc.com (HTTP)
		byte[] privateKey = null; // TODO: Read from config or file or whatever...
		
		GroupAccessChecker groupAccessChecker = new GroupAccessChecker()
		{
			@Override
			public boolean grandAccess(String username, String password, String groupname)
			{
				Player player;
				try {
					player = Authenticator.authenticateUser(username, password);
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
					return false;
				}
				
				if(player == null)
					return false;
				
				Boolean access = player.hasPermission("rtmc.externalaccess.log");
				if(access == null)
					access = false;
				
				return access;
			}
		};
		
		assembler = new Assembler(
				config.getStringList("synchronizer.list"),
				host,
				configDatabase.getString("database"),
				configDatabase.getString("user"),
				configDatabase.getString("pass"),
				config.getString("security.user"),
				config.getString("security.pass"),
				publicKey,
				privateKey,
				groupAccessChecker
				);
		
		FormattedString.addFormatter("MC", new MCStringFormatter());
		
		ConfigurationSection chatFormatConfig = config.getConfigurationSection("chat.format");
		ConfigurationSection chatFormatDefaultConfig = chatFormatConfig.getDefaultSection();
		
		Map<String, Object> result = chatFormatDefaultConfig.getValues(false);
		result.putAll(chatFormatConfig.getValues(false));
		
		for( Entry<String, Object> entry : result.entrySet() )
		{
			if(entry.getKey() instanceof String)
			{
				try {
					chatFormats.put(entry.getKey(), new FormattedString("MC", ((String)entry.getValue()).replace('&', ChatColor.COLOR_CHAR) ));
				} catch (ParseErrorException e) {
					e.printStackTrace();
				}
			}
		}
		handleList = config.getBoolean("enabledfeatures.list");
		
		adminChatGroup = new Group(config.getString("adminchat.admingroup"));
		
		permissionsManager = new PermissionsManager(this);
		
		bukkitEventListener = new BukkitEventListener(this);
		sharedEventListener = new SharedEventListener(this);
		
        loggerGlobal = Logger.getLogger("");
		// In 1.7 can be replaced with something like: org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
		
        loggerGlobal.addHandler(assembler.getLoggerHandler());
	}
	
	public void onDisable()
	{
		HandlerList.unregisterAll(this);
		
		handleList = false;
		
		bukkitEventListener.deinit();
		bukkitEventListener = null;

		sharedEventListener.deinit();
		sharedEventListener = null;
		
		loggerGlobal.removeHandler(logHandler);
		logHandler = null;

		permissionsManager.deinit();
		permissionsManager = null;
		
		assembler.deinit();
		assembler = null;
		
		chatFormats = null;
		adminChatGroup = null;
	}
	
	private boolean commandAmsg(CommandSender sender, Command command, String label, String[] args)
	{
		if(args.length < 1)
		{
			sender.sendMessage("You need to specifie a message!\n" + command.getUsage());
			return true;
		}
		
		if(!(sender instanceof org.bukkit.entity.Player))
		{
			sender.sendMessage("Only players can send admin chat messages");
			return true;
		}
		
		Player player = new Player(sender.getName());
		
		if( !(player.inGroup(adminChatGroup) || sender.isOp()) )
		{
			sender.sendMessage("You cannot send messages to group " + adminChatGroup.getName() + " when you are not in that group!");
			return true;
		}
		
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append(args[0]);
		for(int i = 1; i < args.length; i++)
		{
			stringBuilder.append(' ');
			stringBuilder.append(args[i]);
		}
		
		try {
			FormattedString message = new FormattedString("MC", stringBuilder.toString());
			
			GroupChatEvent event = new GroupChatEvent(player, (sender instanceof org.bukkit.entity.Player)?((org.bukkit.entity.Player)sender).getWorld().getName():null, adminChatGroup, message);
			assembler.getSharedEventBus().postGroupEvent(assembler.getEventBusGroupName(), event);
		} catch (ParseErrorException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	private boolean commandPermission(CommandSender sender, Command command, String label, String[] args)
	{
		if(args.length < 2)
		{
			sender.sendMessage("No target specified!\n\n" + command.getUsage());
			return true;
		}
		
		boolean isUser;
		switch(args[0].toLowerCase())
		{
		case "user":
			isUser = true;
			break;
		case "group":
			isUser = false;
			break;
		default:
			sender.sendMessage(args[0] + " is not a valid argument!\nValid arguments are: user, group\n\n" + command.getUsage());
			return true;
		}
		
		Player player = null;
		Group group = null;
		
		if(isUser)
		{
			try {
				player = new Player(args[1]);
			} catch(IllegalArgumentException e) {
				sender.sendMessage("Cannot manage permissions for player " + args[1] + "!\n\n" + command.getUsage());
				return true;
			}
		}
		else
		{
			try {
				group = new Group(args[1]);
			} catch(IllegalArgumentException e) {
				sender.sendMessage("Cannot manage permissions for group " + args[1] + "!\n\n" + command.getUsage());
				return true;
			}
		}

		if(args.length < 3)
		{
			sender.sendMessage("No action specified!\n\n" + command.getUsage());
			return true;
		}
		
		int action;
		
		switch(args[2])
		{
		case "list":
			action = 0;
			break;
		case "bukkit":
			action = 1;
			break;
		case "check":
			action = 2;
			break;
		case "add":
			action = 3;
			break;
		case "remove":
			action = 4;
			break;
		case "unset":
			action = 5;
			break;
		default:
			sender.sendMessage(args[2] + " is not a valid action!\nValid actions are: list, check, add or remove!\n\n" + command.getUsage());
			return true;
		}

		Permission permission = null;
		if(action > 1)
		{
			if(args.length < 4)
			{
				sender.sendMessage("No permission specified!\n\n" + command.getUsage());
				return true;
			}
			
			permission = new Permission(args[3]);
		}
		
		switch(action)
		{
		case 0:
			if(isUser)
			{
				sender.sendMessage("Permission of player " + player.getDisplayName() + ": \n" + player.getPermissions().toString() );
			}
			else
			{
				sender.sendMessage("Permission of group " + group.getName() + ": \n" + group.getPermissions().toString() );
			}
			break;
		case 1:
			if(isUser)
			{
				sender.sendMessage("Bukkit permission of player " + player.getDisplayName() + ": \n" + permissionsManager.permissionList(player) );
			}
			else
			{
				sender.sendMessage("Bukkit has no build in support for groups. Group permissions are handled by adding them to the user permissions." );
			}
			break;
		case 2:
			if(isUser)
			{
				Boolean has = player.hasPermission(permission);
				sender.sendMessage("Player " + player.getDisplayName() + " " + ((has==null)?"has not specified":(has?"has":"does not have")) + " permission " + permission.toString(false) );
			}
			else
			{
				Boolean has = group.hasPermission(permission);
				sender.sendMessage("Group " + group.getName() + " " + ((has==null)?"has not specified":(has?"has":"does not have")) + " permission " + permission.toString(false) );
			}
			break;
		case 3:
			if(isUser)
			{
				player.addPermission(permission, assembler);
				sender.sendMessage("Added permission " + permission.toString(false) + " to player " + player.getDisplayName());
			}
			else
			{
				group.addPermission(permission, assembler);
				sender.sendMessage("Added permission " + permission.toString(false) + " to group " + group.getName());
			}
			break;
		case 4:
			if(isUser)
			{
				player.removePermission(permission, assembler);
				sender.sendMessage("Removed permission " + permission.toString(false) + " from player " + player.getDisplayName());
			}
			else
			{
				group.removePermission(permission, assembler);
				sender.sendMessage("Removed permission " + permission.toString(false) + " from group " + group.getName());
			}
			break;
		case 5:
			if(isUser)
			{
				player.unsetPermission(permission, assembler);
				sender.sendMessage("Unset permission " + permission.toString(false) + " from player " + player.getDisplayName());
			}
			else
			{
				group.unsetPermission(permission, assembler);
				sender.sendMessage("Unset permission " + permission.toString(false) + " from group " + group.getName());
			}
			break;
		}
		
		return true;
	}
	
	private boolean commandTitle(CommandSender sender, Command command, String label, String[] args)
	{
		if(args.length < 2)
		{
			sender.sendMessage("No target specified!\n\n" + command.getUsage());
			return true;
		}
		
		boolean isUser;
		switch(args[0].toLowerCase())
		{
		case "user":
			isUser = true;
			break;
		case "group":
			isUser = false;
			break;
		default:
			sender.sendMessage(args[0] + " is not a valid argument!\nValid arguments are: user, group\n\n" + command.getUsage());
			return true;
		}
		
		Player player = null;
		Group group = null;
		
		if(isUser)
		{
			try {
				player = new Player(args[1]);
			} catch(IllegalArgumentException e) {
				sender.sendMessage("Cannot manage permissions for player " + args[1] + "!\n\n" + command.getUsage());
				return true;
			}
		}
		else
		{
			try {
				group = new Group(args[1]);
			} catch(IllegalArgumentException e) {
				sender.sendMessage("Cannot manage permissions for group " + args[1] + "!\n\n" + command.getUsage());
				return true;
			}
		}

		if(args.length < 3)
		{
			sender.sendMessage("No action specified!\n\n" + command.getUsage());
			return true;
		}
		
		int action;
		
		switch(args[2])
		{
		case "setprefix":
			action = 0;
			break;
		case "setsuffix":
			action = 1;
			break;
		case "unsetprefix":
			action = 2;
			break;
		case "unsetsuffix":
			action = 3;
			break;
		default:
			sender.sendMessage(args[2] + " is not a valid action!\nValid actions are: setprefix, setsuffix, unsetprefix and unsetsuffix!\n\n" + command.getUsage());
			return true;
		}

		FormattedString text = null;
		
		if(action < 2)
		{
			if(args.length > 3)
			{
				String buf = args[3];
				if(buf.startsWith("\""))
					buf = buf.substring(1);
					if(buf.endsWith("\""))
						buf = buf.substring(0, buf.length() - 1);
					else
						for(int i = 4; i < args.length; i++)
						{
							if(args[i].endsWith("\""))
							{
								buf += " " + args[i].substring(0, args[i].length() - 1);
								break;
							}
							buf += " " + args[i];
						}
				try {
					text = new FormattedString("MC", buf.replace('&', ChatColor.COLOR_CHAR));
				} catch (ParseErrorException e) {
					e.printStackTrace();
				}
			}
		}
		
		if( (action & 1) == 0 )
			if(isUser)
				player.setPrefix(text, assembler);
			else
				group.setPrefix(text, assembler);
		else
			if(isUser)
				player.setSuffix(text, assembler);
			else
				group.setSuffix(text, assembler);
		
		return true;
	}
	
	private boolean commandGroup(CommandSender sender, Command command, String label, String[] args)
	{
		if(args.length < 2)
		{
			sender.sendMessage("No target specified!\n\n" + command.getUsage());
			return true;
		}
		
		boolean isUser;
		switch(args[0].toLowerCase())
		{
		case "user":
			isUser = true;
			break;
		case "group":
			isUser = false;
			break;
		default:
			sender.sendMessage(args[0] + " is not a valid argument!\nValid arguments are: user, group\n\n" + command.getUsage());
			return true;
		}
		
		Player player = null;
		Group group = null;
		
		if(isUser)
		{
			try {
				player = new Player(args[1]);
			} catch(IllegalArgumentException e) {
				sender.sendMessage("Cannot manage permissions for player " + args[1] + "!\n\n" + command.getUsage());
				return true;
			}
		}
		else
		{
			try {
				group = new Group(args[1]);
			} catch(IllegalArgumentException e) {
				sender.sendMessage("Cannot manage permissions for group " + args[1] + "!\n\n" + command.getUsage());
				return true;
			}
		}

		if(args.length < 3)
		{
			sender.sendMessage("No action specified!\n\n" + command.getUsage());
			return true;
		}
		
		int action;
		
		switch(args[2])
		{
		case "list":
			action = 0;
			break;
		case "add":
			action = 1;
			break;
		case "remove":
			action = 2;
			break;
		default:
			sender.sendMessage(args[2] + " is not a valid action!\nValid actions are: list, add or remove!\n\n" + command.getUsage());
			return true;
		}

		Group target = null;
		if(action != 0)
		{
			if(args.length < 4)
			{
				sender.sendMessage("No target group specified!\n\n" + command.getUsage());
				return true;
			}
			target = new Group(args[3]);
		}
		
		switch(action)
		{
		case 0:
			if(isUser)
			{
				StringBuilder message = new StringBuilder();
				message.append("Player ");
				message.append(player.getDisplayName());
				Group[] groups = player.getGroups();
				if(groups.length == 0)
				{
					message.append(" is not a member of any group.");
				}
				else
				{
					message.append(" is a member of groups:");
					for( Group grp : groups )
					{
						message.append("\n   * ");
						message.append(grp.getName());
					}
				}
				sender.sendMessage(message.toString());
			}
			else
			{
				StringBuilder message = new StringBuilder();
				message.append("Group ");
				message.append(group.getName());
				Group[] groups = group.getParents();
				if(groups.length == 0)
				{
					message.append(" does not inherit from any group.");
				}
				else
				{
					message.append(" inherits from groups:");
					for( Group grp : groups )
					{
						message.append("\n   * ");
						message.append(grp.getName());
					}
				}
				sender.sendMessage(message.toString());
			}
			break;
		case 1:
			if(isUser)
				player.addGroup(target, getAssembler());
			else
				group.addParent(target, getAssembler());
			break;
		case 2:
			if(isUser)
				player.removeGroup(target, getAssembler());
			else
				group.removeParent(target, getAssembler());
			break;
		}
		
		return true;
	}
	
	private boolean commandList(CommandSender sender, Command command, String label, String[] args)
	{
		if(!handleList)
			return false;
		
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(ChatColor.WHITE);
		stringBuilder.append(ChatColor.RESET);
		
		for( World world : World.getWorlds())
		{
			Set<Player> players = world.getOnlinePlayers();
			
			if(players.size() == 0)
				continue;
			
			stringBuilder.append(world.getName());
			stringBuilder.append('\n');
			
			Map<Long, List<Player>> groupedOnlinePlayers = new HashMap<Long, List<Player>>();
			
			for( Player player : players )
			{
				Set<Group> groups = player.getMainGroup();
				
				if(groups.size() < 1)
				{
					List<Player> playerList = groupedOnlinePlayers.get(null);
					if(playerList == null)
					{
						playerList = new LinkedList<Player>();
						groupedOnlinePlayers.put(null, playerList);
					}
					playerList.add(player);
				}
				else
				{
					for(Group group : groups)
					{
						List<Player> playerList = groupedOnlinePlayers.get(group.getID());
						if(playerList == null)
						{
							playerList = new LinkedList<Player>();
							groupedOnlinePlayers.put(group.getID(), playerList);
						}
						playerList.add(player);
					}
				}
			}
			
			for( Entry<Long, List<Player>> entry : groupedOnlinePlayers.entrySet() )
			{
				stringBuilder.append(" * ");
				if(entry.getKey() == null)
					stringBuilder.append("Not in any group:");
				else
					stringBuilder.append(new Group(entry.getKey()).getName());
				stringBuilder.append(":\n     ");
				Iterator<Player> iterator = entry.getValue().iterator();
				printPlayer(stringBuilder, iterator.next()); // This is safe because the list only exists when it contains at least one player. 
				while( iterator.hasNext() )
				{
					stringBuilder.append(", ");
					printPlayer(stringBuilder, iterator.next());
				}
				stringBuilder.append("\n");
			}
		}
		
		sender.sendMessage(stringBuilder.toString());
		
		return true;
	}
	
	private void printPlayer(StringBuilder stringBuilder, Player player)
	{
		FormattedString buf;
		
		buf = player.getPrefix();
		if(buf != null)
			stringBuilder.append(buf.toString("MC"));
		
		stringBuilder.append(player.getDisplayName());
		
		buf = player.getSuffix();
		if(buf != null)
			stringBuilder.append(buf.toString("MC"));

		stringBuilder.append(ChatColor.WHITE);
		stringBuilder.append(ChatColor.RESET);
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		switch(command.getName())
		{
		case "amsg":
			return commandAmsg(sender, command, label, args);
		case "permission":
			return commandPermission(sender, command, label, args);
		case "title":
			return commandTitle(sender, command, label, args);
		case "group":
			return commandGroup(sender, command, label, args);
		case "list":
			return commandList(sender, command, label, args);
		default:
			return false;
		}
	}
	
	public Assembler getAssembler()
	{
		return assembler;
	}
	
	/**
	 * @return the PermissionsManager
	 */
	public PermissionsManager getPermissionsManager()
	{
		return permissionsManager;
	}

	public FormattedString getChatFormat(String name)
	{
		return chatFormats.get(name);
	}
}
