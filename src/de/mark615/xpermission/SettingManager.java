package de.mark615.xpermission;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import de.mark615.xpermission.object.Group;
import de.mark615.xpermission.object.XPlayerSubject;
import de.mark615.xpermission.object.XUtil;

public class SettingManager
{
    static SettingManager instance = new SettingManager();
   
    public static SettingManager getInstance()
    {
    	return instance;
    }
    
    FileConfiguration config;
    File cFile;
    
    FileConfiguration message;
    File mFile;
   
    FileConfiguration permission;
    File pFile;
   
    @SuppressWarnings("deprecation")
	public void setup(Plugin p)
    {
    	if (!p.getDataFolder().exists())
    		p.getDataFolder().mkdir();
    	
    	cFile = new File(p.getDataFolder(), "config.yml");
    	if(!cFile.exists())
    		p.saveResource("config.yml", true);

		//Store it
		config = YamlConfiguration.loadConfiguration(cFile);
		config.options().copyDefaults(true);
		
		//Load default messages
		InputStream defConfigStream = p.getResource("config.yml");
		YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
		config.setDefaults(defConfig);
		
        
        
        mFile = new File(p.getDataFolder(), "messages.yml");
        if(!mFile.exists())
			p.saveResource("messages.yml", true);
		
		//Store it
		message = YamlConfiguration.loadConfiguration(mFile);
		message.options().copyDefaults(true);
		
		//Load default messages
		InputStream defMessageStream = p.getResource("messages.yml");
		YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(defMessageStream);
		message.setDefaults(defMessages);
		try
		{
			message.save(mFile);
		}
		catch (IOException e)
		{
			XUtil.severe("Could not save message.yml!");
		}
		
       
		
        pFile = new File(p.getDataFolder(), "permission.yml");
        if (!pFile.exists())
        {
            try {
                pFile.createNewFile();
            }
            catch (IOException e) {
                XUtil.severe("Could not create permission.yml!");
            }
        }
       
        permission = YamlConfiguration.loadConfiguration(pFile);
        if (permission.getString("permissions") == null)
        {
        	addDefault();
        	
        	String temp = "permissions.groups.admin"; 
        	permission.set(temp + ".prefix", "§f[Admin]"); 
        	permission.set(temp + ".suffix", "§f"); 
        	permission.set(temp + ".default", false); 
        	permission.set(temp + ".inheriance", "default");
        	permission.set(temp + ".rank", "1");
        	permission.set(temp + ".permission", new String[]{"*", "*.*"});
        }
        
        if (permission.getString("permissions.groups.default") == null)
        {
        	addDefault();
        }

    	savePermission();
    }
    
    public void checkPermissionFile()
    {
    	//checkConfiguration section
    	for (String key : permission.getConfigurationSection("permissions.groups").getKeys(false))
    	{
    		String path = "permissions.groups." + key;
    		checkGroupConfigurationSection(path, key, permission.getConfigurationSection(path));
    	}
    	for (String key : permission.getConfigurationSection("permissions").getKeys(false))
    	{
    		if (!key.equalsIgnoreCase("groups"))
    		{
    			String path = "permissions." + key;
    			checkPlayerConfigurationSection(path ,permission.getConfigurationSection(path));
        	}
    	}
    	permission.options().copyDefaults(true);
    	savePermission();
    }
    
    private void addDefault()
    {
    	String temp = "permissions.groups.default"; 
    	permission.set(temp + ".prefix", "§f[default]"); 
    	permission.set(temp + ".suffix", "§f"); 
    	permission.set(temp + ".default", true); 
    	permission.set(temp + ".inheriance", "");
    	permission.set(temp + ".rank", "2");
    	permission.set(temp + ".permission", new String[0]);
    }
    
    
//---------Permission section    
    
    public List<Group> loadGroups()
    {
    	if (permission.getConfigurationSection("permissions") == null)
    		return null;
    	
    	List<Group> groups = new ArrayList<>();
    	for (String gp : permission.getConfigurationSection("permissions.groups").getKeys(false))
		{
			Group group = new Group(permission.getConfigurationSection("permissions.groups." + gp));
			groups.add(group);
		}
		XUtil.info("Permissiongroups loaded successfully");
		
		return groups;
    }
    
    private void checkGroupConfigurationSection(String path, String group, ConfigurationSection section)
    {
		permission.options().configuration().addDefault(path + ".prefix", "$f[" + group + "]");
		permission.options().configuration().addDefault(path + ".suffix", "$f");
		permission.options().configuration().addDefault(path + ".default", "");
		permission.options().configuration().addDefault(path + ".inheriance", "");
		permission.options().configuration().addDefault(path + ".rank", 0);
		permission.options().configuration().addDefault(path + ".upgrade", 0);
		permission.options().configuration().addDefault(path + ".permission", new String[0]);
		section.set(path + ".op", null);
    }
    
    private void checkPlayerConfigurationSection(String path, ConfigurationSection section)
    {
		permission.options().configuration().addDefault(path + ".name", section.getString("name"));
		permission.options().configuration().addDefault(path + ".uuid", section.getString("uuid"));
		permission.options().configuration().addDefault(path + ".group", XPermission.getInstance().getDefaultGroup().getName());
		permission.options().configuration().addDefault(path + ".op", false);
		permission.options().configuration().addDefault(path + ".firstLogin", System.currentTimeMillis());
		permission.options().configuration().addDefault(path + ".lastLogin", System.currentTimeMillis());
		permission.options().configuration().addDefault(path + ".gameTime", 0);
		permission.options().configuration().addDefault(path + ".permission", new String[0]);
    }
    
    public boolean isPlayerFirstJoin(Player p)
    {
		XPermission plugin = XPermission.getInstance();
		boolean firstJoin = false;
		
		String path = "permissions." + p.getUniqueId().toString();
		if (permission.getConfigurationSection(path) == null)
		{
			firstJoin = true;
			generatePlayerSection(plugin, p);

			if (plugin.hasAPI())
				plugin.getAPI().createPlayerFirstjoinEvent(p);

			if (hasFirstJoinMessage())
			{
				Bukkit.broadcastMessage(ChatColor.BLUE + p.getName() + ChatColor.AQUA + " " + XUtil.getMessage("message.first-join"));
			}
		}
		checkPlayerGroup(plugin, p);
		
		savePermission();
		return firstJoin;
    }
    
    public Map<String, Boolean> getPlayerPermissionList(UUID uuid, XPlayerSubject subject)
    {
    	List<String> loadedGroups = new ArrayList<>();
    	Map<String, Boolean> perms = new HashMap<>();

    	//load grouppermissions
    	String group = permission.getString("permissions." + uuid.toString() + ".group");
    	loadPlayerGroupPermission(group, perms, subject, loadedGroups);
    	
    	//load playerpermission
    	loadPlayerPermission(uuid, perms);
    	return perms;
    }
    
    private void loadPlayerGroupPermission(String group, Map<String, Boolean> playerPerms, XPlayerSubject subject, List<String> loadedGroups)
    {
    	loadedGroups.add(group);
    	String nextGroup = permission.getString("permissions.groups." + group + ".inheriance");
    	if (nextGroup != null && !nextGroup.equalsIgnoreCase(""))
    	{
    		if (!loadedGroups.contains(nextGroup))
    			loadPlayerGroupPermission(nextGroup, playerPerms, subject, loadedGroups);
    	}
    	
    	//load permissions of group
    	List<String> perms = permission.getStringList("permissions.groups." + group + ".permission");
    	if (perms != null && perms.size() > 0)
    	{
    		for (String key : perms)
    		{
    			if (key != null)
    				calculatePermission(playerPerms, key);
    		}
    	}
    }
    
    private void loadPlayerPermission(UUID uuid, Map<String, Boolean> playerPerms)
    {
    	//load permissions of player
    	List<String> perms = permission.getStringList("permissions." + uuid.toString() + ".permission");
    	if (perms != null && perms.size() > 0)
    	{
    		for (String key : perms)
    		{
    			if (key != null)
    				calculatePermission(playerPerms, key);
    		}
    	}
    }
    
    private void calculatePermission(Map<String, Boolean> playerPerms, String perm)
    {
		int value = 0;
		if (perm.startsWith("- "))
		{
			value = -1;
			perm = perm.substring(1);
		}
		else
		if (perm.startsWith("+ "))
		{
			value = 1;
			perm = perm.substring(1);
		}
		
		perm = perm.trim();
		perm = perm.toLowerCase();
		
		if (value == -1)
		{
			playerPerms.put(perm, false);
		}
		else
		if (value == 1)
		{
			playerPerms.put(perm, true);
		}
		else
		{
			if (!playerPerms.containsKey(perm))
				playerPerms.put(perm, true);
		}
    }
    
    public String getPlayerDisplayName(Player p)
    {
    	String path = "permissions.groups." + permission.getString("permissions." + p.getUniqueId().toString() + ".group"); 
		String prefix = permission.getString(path + ".prefix").replace("&", "§");
		String suffix = permission.getString(path + ".suffix").replace("&", "§");
		return prefix + p.getName() + suffix + "" + ChatColor.RESET;
    }
    
    public boolean isPlayerOp(Player p)
    {
    	return permission.getBoolean("permissions." + p.getUniqueId().toString() + ".op", false);		
    }
    
    public int getPlayerRank(UUID uuid)
    {
    	return permission.getInt("permissions.groups." + permission.getString("permissions." + uuid.toString() + ".group") + ".rank");
    }
    
	private void checkPlayerGroup(XPermission plugin, Player p)
	{
		String path = "permissions." + p.getUniqueId().toString() + ".group";
		String group = permission.getString(path);
		if (group == null)
		{
			permission.set(path, plugin.getDefaultGroup().getName());
		}
		else
		{
			boolean contain = false;
			for (Group g : plugin.getGroups())
			{
				if (group.equals(g.getName()))
					contain = true;
			}
			if (!contain)
			{
				permission.set(path, plugin.getDefaultGroup().getName());
			}
		}
	}
	
	private void generatePlayerSection(XPermission plugin, Player p)
	{
		String path = "permissions." + p.getUniqueId().toString();
		try
		{
			permission.createSection(path);
			permission.set(path + ".name", p.getName());
			permission.set(path + ".uuid", p.getUniqueId().toString());
			permission.set(path + ".group", plugin.getDefaultGroup().getName());
			permission.set(path + ".op", false);
			permission.set(path + ".firstLogin", System.currentTimeMillis());
			permission.set(path + ".lastLogin", System.currentTimeMillis());
			permission.set(path + ".gameTime", 0);
			permission.set(path + ".permission", new String[0]);
		}
		catch (NullPointerException ex)
		{
			ex.printStackTrace();
			p.sendMessage(ChatColor.RED + "Error! See Console For More Information!");
		}
	}
	
	public void saveXPlayerSubject(XPlayerSubject subject)
	{
		ConfigurationSection section = permission.getConfigurationSection("permissions." + subject.getUUID().toString());
		section.set("group", subject.getGroup().getName());
		section.set("lastLogin", subject.getLastLogin());
		section.set("gameTime", subject.getTotalGameTime());
	}
	
	public ConfigurationSection getPlayerConfigurationsection(UUID uuid)
	{
		return permission.getConfigurationSection("permissions." + uuid.toString());
	}
   
    public void savePermission()
    {
        try {
        	permission.save(pFile);
        }
        catch (IOException e)
        {
            XUtil.severe("Could not save permission.yml!");
        }
    }
   
    public void reloadPermission()
    {
    	permission = YamlConfiguration.loadConfiguration(pFile);
    }
    
   
//---------Configuration section
    
    public FileConfiguration getConfig()
    {
        return config;
    }
   
    public void saveConfig()
    {
        try {
            config.save(cFile);
        }
        catch (IOException e) {
        	XUtil.severe("Could not save config.yml!");
        }
    }
   
    public void reloadConfig()
    {
    	config = YamlConfiguration.loadConfiguration(cFile);
    }
    
    public boolean hasFirstJoinMessage()
    {
    	return config.getBoolean("first-join-message", true);
    }
    
    public boolean hasRankCountAFKTime()
    {
    	return !config.getBoolean("rank.disable-count-afktime", false);
    }
    

//---------Message section
    
    public FileConfiguration getMessage()
    {
        return message;
    }
   
    public void reloadMessage()
    {
    	message = YamlConfiguration.loadConfiguration(mFile);
    }
}
