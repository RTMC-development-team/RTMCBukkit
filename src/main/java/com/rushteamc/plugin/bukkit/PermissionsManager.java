package com.rushteamc.plugin.bukkit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionAttachment;

import com.rushteamc.plugin.common.Player;
import com.rushteamc.plugin.common.Permissions.Permission;

public class PermissionsManager
{
	private final Plugin plugin;
	private final Map<Long, PermissionAttachment> permissionAttachments = new HashMap<Long, PermissionAttachment>();

	/*
	 * TODO:
	 *   Update permissions when plugin is loaded/unloaded or when permission is created/removed
	 */
	
	public PermissionsManager(Plugin plugin)
	{
		this.plugin = plugin;
	}
	
	public void deinit()
	{
		for( Entry<Long, PermissionAttachment> entry : permissionAttachments.entrySet() )
			Bukkit.getPlayer(new Player(entry.getKey()).getDisplayName()).removeAttachment(entry.getValue());
		permissionAttachments.clear();
	}
	
	public void addAttachment(Player player)
	{
		org.bukkit.entity.Player p = Bukkit.getPlayer(player.getDisplayName());
		
		if(p == null)
			return;
		
		PermissionAttachment attachment = p.addAttachment(plugin);
		permissionAttachments.put(player.getID(), attachment);
	}
	
	public void removeAttachment(Player player)
	{
		PermissionAttachment attachement = permissionAttachments.get(player.getID());
		
		if(attachement == null)
			return;
		
		Bukkit.getPlayer(player.getDisplayName()).removeAttachment(attachement);
		permissionAttachments.remove(player.getID());
	}
	
	public void updatePermission(Player player)
	{
		PermissionAttachment attachment = permissionAttachments.get(player.getID());
		
		if(attachment == null)
		{
			addAttachment(player);
			attachment = permissionAttachments.get(player.getID());
			if(attachment == null)
				return;
		}

		for( Entry<String, Boolean> entry : attachment.getPermissions().entrySet() )
			attachment.unsetPermission(entry.getKey());
		
		Collection<Permission> permissions = player.getPermissions().getPermissions();
		for( Permission permission : permissions )
			updatePermission(attachment, permission);		
	}
	
	private void updatePermission(PermissionAttachment attachment, Permission permission)
	{
		org.bukkit.permissions.Permission bukkitPermission = Bukkit.getPluginManager().getPermission(permission.toString(false));

		if(bukkitPermission == null)
			bukkitPermission = Bukkit.getPluginManager().getPermission(permission.toString(false) + ".*");
		
		if(bukkitPermission == null)
		{
			Set<org.bukkit.permissions.Permission> bukkitPermissionSet = Bukkit.getPluginManager().getPermissions();
			String permissionString = permission.toString(false) + '.';
			int foundCount = 0;
			for( org.bukkit.permissions.Permission bukkitPermissionEntry : bukkitPermissionSet )
			{
				if(bukkitPermissionEntry.getName().startsWith(permissionString))
				{
					if(permission.isGranded() == null)
						attachment.unsetPermission(bukkitPermissionEntry);
					else
						attachment.setPermission(bukkitPermissionEntry, permission.isGranded());
					foundCount++;
				}
			}
			if(foundCount == 0)
			{
				if(permission.isGranded() != null)
				{
					org.bukkit.permissions.Permission newBukkitPermission = new org.bukkit.permissions.Permission(permission.toString(false));
					attachment.setPermission(newBukkitPermission, permission.isGranded());
				}
			}
		}
		else
		{
			if(permission.isGranded() == null)
				attachment.unsetPermission(bukkitPermission);
			else
				attachment.setPermission(bukkitPermission, permission.isGranded());
		}
	}

	public void updatePermissionOld(Player player)
	{
		PermissionAttachment attachment = permissionAttachments.get(player.getID());
		
		if(attachment == null)
		{
			addAttachment(player);
			attachment = permissionAttachments.get(player.getID());
			if(attachment == null)
				return;
		}
		
		Collection<Permission> permissions = player.getPermissions().getPermissions();
		String[] permissionStrings = new String[permissions.size()];
		String[] permissionExtendedStrings = new String[permissions.size()];
		Set<org.bukkit.permissions.Permission> bukkitPermissions = Bukkit.getPluginManager().getPermissions();
		
		Iterator<Permission> it = permissions.iterator();
		int n = 0;
		while(it.hasNext())
		{
			Permission p = it.next();
			permissionStrings[n] = p.toString(false);
			permissionExtendedStrings[n] = permissionStrings[n] + ".";
			n++;
		}
		
		for( org.bukkit.permissions.Permission bukkitPermission : bukkitPermissions )
		{
			int i = 0;
			for( Permission permission : permissions )
			{
				if(bukkitPermission.getName().equals(permissionStrings[i]))
				{
					if(permission.isGranded() == null)
						attachment.unsetPermission(bukkitPermission);
					else
						attachment.setPermission(bukkitPermission, permission.isGranded());
				}
				else if(bukkitPermission.getName().startsWith(permissionExtendedStrings[i]))
				{
					if(permission.isGranded() == null)
						attachment.unsetPermission(bukkitPermission);
					else
						attachment.setPermission(bukkitPermission, permission.isGranded());

					for( Entry<String, Boolean> entry : bukkitPermission.getChildren().entrySet() )
					{
						if(permission.isGranded() == null)
							attachment.unsetPermission(entry.getKey());
						else
							attachment.setPermission(entry.getKey(), permission.isGranded());
					}
				}
				else
				{
					for( Entry<String, Boolean> entry : bukkitPermission.getChildren().entrySet() )
					{
						if(entry.getKey().equals(permissionStrings[i]))
						{
							if(permission.isGranded() == null)
								attachment.unsetPermission(permissionStrings[i]);
							else
								attachment.setPermission(permissionStrings[i], permission.isGranded());
						}
					}
				}
				i++;
			}
		}
	}
	
	public String permissionList(Player player)
	{
		PermissionAttachment attachment = permissionAttachments.get(player.getID());
		
		if(attachment == null)
		{
			addAttachment(player);
			attachment = permissionAttachments.get(player.getID());
			if(attachment == null)
				return "No access to bukkit permissisions!";
		}

		Set<Entry<String, Boolean>> permissions = attachment.getPermissions().entrySet();
		if(permissions.isEmpty())
		{
			return "This player has no permissions!";
		}
		else
		{
			StringBuilder stringBuilder = new StringBuilder();
			Iterator<Entry<String, Boolean>> iterator = permissions.iterator();
			
			stringBuilder.append("   ");
			Entry<String, Boolean> permission = iterator.next();
			stringBuilder.append(permission.getValue()?'+':'-');
			stringBuilder.append(permission.getKey());
			while(iterator.hasNext())
			{
				permission = iterator.next();
				stringBuilder.append("\n   ");
				stringBuilder.append(permission.getValue()?'+':'-');
				stringBuilder.append(permission.getKey());
				/*
				org.bukkit.permissions.Permission perm = Bukkit.getPluginManager().getPermission(permission.getKey());
				if(permission != null)
				{
					String description = perm.getDescription();
					if(description == null)
						continue;
					if(description.isEmpty())
						continue;
					
					stringBuilder.append(" (");
					stringBuilder.append(description);
					stringBuilder.append(')');
				}
				*/
			}
			return stringBuilder.toString();
		}
		
	}
}
