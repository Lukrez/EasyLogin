package me.markus.easylogin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.permission.Permission;

public class EasyLogin extends JavaPlugin implements Listener {

	public static EasyLogin instance;
	public static Permission permission;
	public static MySQLDataSource database;
	private HashMap<String, PlayerInfo> players = new HashMap<String, PlayerInfo>();
	private HashMap<String, LoginTrial> loginTrials = new HashMap<String, LoginTrial>();
	private HashMap<String, RegistrationConversation> registering = new HashMap<String, RegistrationConversation>();
	private HashSet<String> guests = new HashSet<String>();
	private int purgeTaskId = -1;
	private long nexPurge;
	private String whitelistReason = "Der Server ist momentan wegen Wartungsarbeiten im Whitelist-Modus. Vielleicht gibts im Forum nähere Infos!";

	
	
	@Override
	public void onDisable() {
		
		if (database != null)
			database.close();
		//set groups of all inloggedinusers and kick them
		for (PlayerInfo pi : this.players.values()) {
			Player player = this.getServer().getPlayerExact(pi.getPlayerName());
			if (player != null)
				player.kickPlayer("Server wird gestoppt!");
			pi.cancelTask();
			this.players.remove(pi.getPlayerName());
		}

		if (this.purgeTaskId != -1) {
			Bukkit.getScheduler().cancelTask(this.purgeTaskId);
		}
		this.getLogger().info("v" + this.getDescription().getVersion() + " disabled.");
	}

	@Override
	public void onEnable() {
		instance = this;
		if (!this.setupPermissions())
			this.getServer().getPluginManager().disablePlugin(this);
		this.getServer().getPluginManager().registerEvents(this, this);
		
		if (!this.getDataFolder().exists())
			this.getDataFolder().mkdir();
		Settings.loadSettings();
		try {
			database = new MySQLDataSource();
		} catch (Exception ex) {
			this.getLogger().severe(ex.getMessage());
			if (Settings.isStopEnabled) {
				this.getLogger().severe("Can't use MySQL... Please input correct MySQL informations ! SHUTDOWN...");
				this.getServer().shutdown();
			}
			if (!Settings.isStopEnabled)
				this.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		/* Set Console Filter */
		if (Settings.isConsoleFilterEnabled) {
			this.getLogger().setFilter(new ConsoleFilter());
			Bukkit.getLogger().setFilter(new ConsoleFilter());
			
			try {
				Class.forName("org.apache.logging.log4j.core.Filter");
				this.setLog4JFilter();
				} catch (ClassNotFoundException e) {
					this.getLogger().info("You're using Minecraft 1.6.x or older, Log4J support is disabled");
				} catch (NoClassDefFoundError e) {
					this.getLogger().info("You're using Minecraft 1.6.x or older, Log4J support is disabled");
			}
			
		}
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "LoginFoo");
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "Register");
		
		startPurgeTask();
		this.getLogger().info("v" + this.getDescription().getVersion() + " enabled.");
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (command.getName().equalsIgnoreCase("easylogin") && sender.hasPermission("easylogin.manage")) {
			if (args.length == 0) {
				sender.sendMessage("Befehle für das Plugin EasyLogin");
				sender.sendMessage("/easylogin reload - Lädt die Config datei neu");
				sender.sendMessage("/easylogin status - Status des Plugins plus Liste aller Spieler mit momentanen Wartezeiten");
				sender.sendMessage("/easylogin purge - Löscht alle Einlogdateien");
				sender.sendMessage("/easylogin show <player name> - Zeigt an ob der angebene User eine Loginbeschränkung hat");
				sender.sendMessage("/easylogin pardon <player name> - Löscht die Loginbeschränkung für den User");
				sender.sendMessage("/easylogin guestamount <number> - Setzt die Anzahl an möglichen Gästen auf dem Server");
				sender.sendMessage("/easylogin guestexempt <name> - Nimm den Gast von der Spambot-Protection aus");
				sender.sendMessage("/easylogin guestunexempt <name,*> - Entferne den Gast von der Spambot-Protection-Ausnahmeliste");				
				sender.sendMessage("/easylogin listexempt -  Zeige alle Gäste an, welche von der Spambotprotection ausgenommen sind");
				sender.sendMessage("/easylogin joins <number> - Anzahl an Logins pro 10 Sekunden, Spambotdetection (0 = Aus)");
				sender.sendMessage("/easylogin whitelist <ON|OFF> [reason for whitelist]- Toggelt die Whitelist");

				return true;
			}

			if (args[0].equalsIgnoreCase("reload")) {
				Settings.loadSettings();
				startPurgeTask();
				sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
				return true;
			}
			if (args[0].equalsIgnoreCase("status")) {
				if (Settings.isWhitelisted) {
					sender.sendMessage(ChatColor.GRAY + "Whitelisted: " + ChatColor.RED + "JA");
				} else {
					sender.sendMessage(ChatColor.GRAY + "Whitelisted: " + ChatColor.GREEN + "NEIN");
				}
				sender.sendMessage(ChatColor.GRAY + "Anzahl an Gästen: " + ChatColor.WHITE + this.guests.size());
				sender.sendMessage(ChatColor.GRAY + "Anzahl an UnloggedinUsers: " + ChatColor.WHITE + this.players.size());
				sender.sendMessage(ChatColor.GRAY + "Nächster Loginpurge in: " + ChatColor.WHITE + (this.nexPurge - new Date().getTime()) / 1000 / 60 + "  min");
				sender.sendMessage(ChatColor.GRAY + "Spieler mit momentanen Login-Wartezeiten:");
				for (LoginTrial lt : this.loginTrials.values()) {
					sender.sendMessage(lt.playerName + " Wartezeit: " + (int) lt.waitForNextLogin() / 1000 + " sec" + " IP: " + lt.lastIP);
				}
				return true;
			}

			if (args[0].equalsIgnoreCase("purge")) {
				this.loginTrials.clear();
				sender.sendMessage(ChatColor.GREEN + "Einlogdaten gelöscht!");
				return true;
			}

			if (args[0].equalsIgnoreCase("show")) {
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Bitte einen Spielernamen angeben!");
					return true;
				}
				String name = args[1];
				LoginTrial lt = this.loginTrials.get(name);
				if (lt == null) {
					sender.sendMessage(ChatColor.RED + "Für diesen Spieler existiert momentan keine Loginbeschränkung!");
					return true;
				}
				sender.sendMessage(lt.playerName + " Wartezeit: " + (int) lt.waitForNextLogin() / 1000 + " sec" + " IP: " + lt.lastIP);
				return true;

			}

			if (args[0].equalsIgnoreCase("pardon")) {
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Bitte einen Spielernamen angeben!");
					return true;
				}
				String name = args[1];
				LoginTrial lt = this.loginTrials.remove(name);
				if (lt == null) {
					sender.sendMessage(ChatColor.RED + "Für diesen Spieler existiert momentan keine Loginbeschränkung!");
					return true;
				}
				sender.sendMessage(ChatColor.GREEN + "Loginbeschränkung gelöscht!");
				return true;

			}

			if (args[0].equalsIgnoreCase("joins")) {
				if (args.length < 2) {
					sender.sendMessage(ChatColor.GREEN+"Momentan können "+Settings.getLoginsPerTenSeconds+ " Gäste pro 10 Sekunden joinen, ohne die Spambotschwelle auszulösen.");
					sender.sendMessage(ChatColor.GREEN + "Um die Schwelle zu ändern gib bitte eine Zahl pro 10 Sekunden an! (0 für AUS)");
					return true;
				}
				int freq = Integer.parseInt(args[1]);
				if (freq < 0) {
					sender.sendMessage(ChatColor.RED + "Bitte eine Zahl größer 0 angeben!");
					return true;
				}
				Settings.getLoginsPerTenSeconds = freq;
				Settings.saveSettings();
				sender.sendMessage(ChatColor.GREEN + "AntiSpamBot-Schwelle geändert!");
				return true;

			}
			
			if (args[0].equalsIgnoreCase("guestamount")) {
				if (args.length < 2) {
					sender.sendMessage(ChatColor.GREEN+"Momentan können "+Settings.getNrAllowedGuests+ " Gäste auf den Server!");
					sender.sendMessage(ChatColor.GREEN + "Um die Anzahl zu ändern gib bitte eine Zahl an!");
					return true;
				}
				int amount = Integer.parseInt(args[1]);
				if (amount < 0) {
					sender.sendMessage(ChatColor.RED + "Bitte eine Zahl größer 0 angeben!");
					return true;
				}
				Settings.getNrAllowedGuests = amount;
				Settings.saveSettings();
				sender.sendMessage(ChatColor.GREEN + "Maximal-Anzahl an Gästen auf diesem Server geändert!");
				return true;
			}
							
			if (args[0].equalsIgnoreCase("whitelist")) {
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Bitte [ON|OFF] angeben!");
					return true;
				}
				String status = args[1];
				if (status.equalsIgnoreCase("on")) {
					this.whitelistReason = "Der Server ist momentan wegen Wartungsarbeiten im Whitelist-Modus. Vielleicht gibts im Forum nähere Infos!";
					if (args.length > 2){
						this.whitelistReason = args[2];
					}
					Settings.isWhitelisted = true;
					sender.sendMessage(ChatColor.GREEN + "Server ist im Whitelist-Modus!");
				} else if (status.equalsIgnoreCase("off")) {
					Settings.isWhitelisted = false;
					sender.sendMessage(ChatColor.GREEN + "Server ist im Spiel-Modus!");
				} else {
					sender.sendMessage(ChatColor.RED + "Bitte [ON|OFF] angeben!");
					return true;
				}
				Settings.saveSettings();
				return true;

			}
			return false;
		}

		if (!(sender instanceof Player)) {
			return true;
		}

		Player player = (Player) sender;

		if (command.getName().equalsIgnoreCase("login")) {
			PlayerInfo pi = this.players.get(player.getName().toLowerCase());
			if (pi == null) {
				player.sendMessage(ChatColor.GREEN + "Du bist bereits eingeloggt oder Gast auf dem Server. Wenn du dich soeben registriert hast starte Minecraft einmal neu!");
				return true;
			}

			if (args.length == 0) {
				player.sendMessage(ChatColor.RED + "Bitte logge dich mit /l <password> innerhalb von 30 Sekunden ein.");
				return true;
			}

			if (!pi.checkPassword(args[0],player.getName().toLowerCase())) {
				player.kickPlayer(ChatColor.RED + "Falsches Passwort!");
				this.getLogger().info("Spieler "+ player.getName() + " hat ein falsches Passwort eingegeben!");
				return true;
			}
			
			
			pi.setPlayerstatus(Playerstatus.Loggedin);
			if (Settings.useBungeeCoord == true){
				database.updatePlayerStatus(pi.getAuth());
				EasyLogin.callBungeeCoord(player, "LoginFoo", "#Playerlogin#"+player.getName().toLowerCase()+"#");
			}
			
			// Remove player from unloggedin group 
			this.players.remove(player.getName().toLowerCase());
			player.sendMessage(ChatColor.GREEN + "Login erfolgreich.");
			this.getLogger().info("Spieler "+ player.getName() + " hat sich erfolgreich eingeloggt!");
			pi.cancelTask();

			// remove possible loginTrials
			this.loginTrials.remove(player.getName().toLowerCase());
			
			Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
			return true;
		}
		
		if (command.getName().equalsIgnoreCase("register")) {

			if (Settings.registerAllowRegistration == false) {
				player.sendMessage(ChatColor.RED + "Zum Registrieren benutze bitte unsere Webseite www.minecraft-spielewiese.de");
				return true;
			}
			if (!this.guests.contains(player.getName().toLowerCase())) {
				player.sendMessage(ChatColor.RED + "Nur Gäste können das Register-Kommando benutzen!");
				return true;
			}
			if (Settings.registerUseLocationLimiter == false) {
				this.registerPlayer(player);
				return true;
			}
			// check world and location
			Location loc = player.getLocation();
			if (!loc.getWorld().getName().equals(Settings.registerWorldname)) {
				player.sendMessage(ChatColor.RED + "Das Registrieren funktioniert nur auf der " + Settings.registerWorldname + " Welt!");
				return true;
			}
			boolean isInLocation = true;
			if (loc.getBlockX() < Settings.registerLoc1[0])
				isInLocation = false;
			if (loc.getBlockX() > Settings.registerLoc2[0])
				isInLocation = false;
			
			if (loc.getBlockY() < Settings.registerLoc1[1])
				isInLocation = false;
			if (loc.getBlockY() > Settings.registerLoc2[1])
				isInLocation = false;
			
			if (loc.getBlockZ() < Settings.registerLoc1[2])
				isInLocation = false;
			if (loc.getBlockZ() > Settings.registerLoc2[2])
				isInLocation = false;
			
			if (isInLocation == false) {
				player.sendMessage(ChatColor.RED + "Du befindest dich nicht an der richtigen Stelle zum registrieren - schau dich weiter um!");
				return true;
			}
			
			this.registerPlayer(player);
			return true;
		}

		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(AsyncPlayerPreLoginEvent event) { // Search for Kick reasons

		String playerName = event.getName().toLowerCase();

		int min = Settings.getMinNickLength;
		int max = Settings.getMaxNickLength;
		String regex = Settings.getNickRegex;

		if (playerName.length() > max || playerName.length() < min) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Dein Minecraftname muss zwischen " + min + " und " + max + " Zeichen lang sein!");
			return;
		}

		if (!playerName.matches(regex) || playerName.equals("player")) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Dein Minecraftname enthält nicht erlaubte Sonderzeichen!");
			return;
		}
		
		if (isAlreadyLoggedIn(playerName))
	    {
	      event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Ein Spieler mit diesem Namen ist bereits eingeloggt!");
	      return;
	    }

		// Whitelist
		if (Settings.isWhitelisted && !isInWhitelist(playerName)) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, this.whitelistReason);
			return;
		}
		// Logintrials
		LoginTrial lt = this.loginTrials.get(playerName);
		if (lt != null) {
			long time = lt.waitForNextLogin();
			if (time > 0) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Bitte warte " + (int) time / 1000 + " Sekunden bis zu deinem nächsten Login!");
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		this.onPlayerJoin(player);
	}	
	
	public void onPlayerJoin(Player player) {
		EasyLogin.callBungeeCoord(player, "LoginFoo", "#Playerjoin#"+player.getName()+"#");
		String lcName = EasyLogin.getlowerCasePlayerName(player);
		if (player.isOp()) {
			player.setOp(false);
		}
		// a) Already Logged in -> Kick?
		// b) Unregistered -> Guest
		PlayerAuth playerAuth = database.getAuth(lcName);
		if (playerAuth == null) {
			// remove all previous groups
			for (String group : permission.getPlayerGroups(player)) {
				permission.playerRemoveGroup(null,player, group);
			}
			// set Guest group
			permission.playerAddGroup(null,player, "Guest"); // TODO: Check, if pex works with lowercase names
			this.guests.add(lcName);
			player.sendMessage(ChatColor.GREEN + "Willkommen auf dem Minecraft-Spielewiese Server!");
			return;
		}
		// remove player from guest list
		this.guests.remove(lcName);
		boolean isNewTraveller = true;
		for (String group : permission.getPlayerGroups(player)) {
			if (!group.equals("Guest")) {
				isNewTraveller = false;
				break;
			}
		}
		if (isNewTraveller) {
			permission.playerAddGroup(null, player, "Traveller");
			permission.playerRemoveGroup(null,player, "Guest");
		}

		// c) Registered -> UnloggedinUsers -> After Login in old groups
		PlayerInfo pi = new PlayerInfo(lcName, playerAuth, player.getLocation()); // TODO: Already logged in?
		
		// check if player is already logged in via bungeecoord		
		if (Settings.useBungeeCoord == true && playerAuth.getStatus() == Playerstatus.Loggedin){
			this.getLogger().info("Spieler "+ player.getName() + " ist bereits über Bungeecoord eingeloggt!");
			pi.cancelTask();
			return;
		}

		this.players.put(lcName, pi);

		LoginTrial lt = (LoginTrial)this.loginTrials.get(lcName);
	    if (lt == null)
	    {
	      lt = new LoginTrial(lcName);
	      this.loginTrials.put(lcName, lt);
	    }
	    lt.addLogin(player.getAddress().toString());
		player.sendMessage(ChatColor.RED + "Bitte logge dich mit /l <password> innerhalb von 30 Sekunden ein.");
	}
	
	public void removePlayer(Player player){
		String lcName = EasyLogin.getlowerCasePlayerName(player);
		PlayerInfo pi = this.players.get(lcName);
		if (pi != null) {
			pi.cancelTask();
			player.teleport(pi.getLocation());
		}

		this.players.remove(lcName);
		this.guests.remove(lcName);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		this.removePlayer(player);
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = event.getPlayer();
		this.removePlayer(player);
	}
	
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event){
		if (this.players.containsKey(EasyLogin.getlowerCasePlayerName(event.getPlayer()))){
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event){
		if (this.players.containsKey(EasyLogin.getlowerCasePlayerName(event.getPlayer()))){
			event.setCancelled(true);
		}
	}
	
	
	@EventHandler
	public void onEntityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event){
		if (event.getTarget() instanceof Player){
			Player player = (Player)event.getTarget();
			if (this.players.containsKey(EasyLogin.getlowerCasePlayerName(player))){
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event){
		if (event.getDamager() instanceof Player){
			Player player = (Player)event.getDamager();
			if (this.players.containsKey(EasyLogin.getlowerCasePlayerName(player))){
				event.setCancelled(true);
			}
		}
		
		if (event.getEntity() instanceof Player){
			Player player = (Player)event.getEntity();
			if (this.players.containsKey(EasyLogin.getlowerCasePlayerName(player))){
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onEntityDamageEvent(EntityDamageEvent event){
		if (event.getEntity() instanceof Player){
			Player player = (Player)event.getEntity();
			if (this.players.containsKey(EasyLogin.getlowerCasePlayerName(player))){
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked().getType() != EntityType.PLAYER)
			return;

		Player player = (Player) event.getWhoClicked();
		if (this.players.containsKey(EasyLogin.getlowerCasePlayerName(player))){
			event.setCancelled(true);
		}

	}

	@EventHandler
	public void onPlayerPickupItem(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (this.players.containsKey(EasyLogin.getlowerCasePlayerName(player))) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (this.players.containsKey(EasyLogin.getlowerCasePlayerName(event.getPlayer()))){
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String playername = EasyLogin.getlowerCasePlayerName(player);
		
		if (this.registering.containsKey(playername)){
			event.setCancelled(true);
			return;
		}
		
		boolean isUnloggedin = this.players.containsKey(playername);
		
		String command = event.getMessage();
		if (!isUnloggedin)
			return;
		if (command.startsWith("/l ") || command.startsWith("/login ") || command.startsWith("/L "))
			return;
		player.sendMessage("Bitte logge dich mit /login <passwort> ein!");
		this.getLogger().info("[EasyLogin] Cancelling command "+command +" from "+player.getName());
		event.setCancelled(true);
		
	}
	
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		String playername = EasyLogin.getlowerCasePlayerName(player);
		if (this.guests.contains(playername)){
			return;
		}
		
		boolean isUnloggedin = this.players.containsKey(playername);
		if (!isUnloggedin)
			return;
		player.sendMessage("Du musst dich eingloggen um chatten zu können!");
		this.getLogger().info("[EasyLogin] Cancelling chat from "+player.getName());
		event.setCancelled(true);
		
	}

	private void startPurgeTask() {
		if (this.purgeTaskId != -1) {
			Bukkit.getScheduler().cancelTask(this.purgeTaskId);
		}
		if (Settings.getPurgeInterval > 0) {

			this.purgeTaskId = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				@Override
				public void run() {
					purgeLoginList();
					nexPurge = new Date().getTime() + Settings.getPurgeInterval * 60 * 1000;
				}
			}, 60, Settings.getPurgeInterval * 20 * 60);
			this.getLogger().info("Purgetask started. Purgefrequency set to " + Settings.getPurgeInterval + " min.");
		} else {
			this.getLogger().info("Purgeinterval is 0 min, no Purgetask started!");
		}
	}

	private void purgeLoginList() {
		this.getServer().getLogger().info("purging login data");
		long now = new Date().getTime();
		ArrayList<String> delete = new ArrayList<String>();

		for (LoginTrial lt : this.loginTrials.values()) {
			if (lt.trialNr > Settings.getPurgeThreshold)
				continue;
			if (now - lt.lastLogin.getTime() > Settings.getPurgeInterval * 20 * 60) {
				delete.add(lt.playerName);
			}
		}
		for (String name : delete) {
			this.loginTrials.remove(name);
		}
	}

	private boolean isInWhitelist(String playername) {

		for (OfflinePlayer player : this.getServer().getWhitelistedPlayers()) {
			if (player.getName().equalsIgnoreCase(playername))
				return true;
		}
		return false;
	}
	  
	  private boolean isAlreadyLoggedIn(String playername)
	  {
	    for (Player player : getServer().getOnlinePlayers()) {
	      if (player.getName().equalsIgnoreCase(playername)) {
	        return true;
	      }
	    }
	    return false;
	  }
	  
	  public static String getlowerCasePlayerName(Player player){
		  return player.getName().toLowerCase();
	  }
	  
	  public static void callBungeeCoord(Player player, String channel, String message){
		  if (!(EasyLogin.instance.getServer().getPluginManager().isPluginEnabled(EasyLogin.instance)))
			  return;
		  ByteArrayOutputStream b = new ByteArrayOutputStream();
		  DataOutputStream out = new DataOutputStream(b);
		  try {
			  out.writeUTF(message);
		  } catch (IOException e) {
			  // Can never happen
		  }
		  // currently registered channels are "LoginFoo"
		  player.sendPluginMessage(EasyLogin.instance, channel, b.toByteArray());
	  }
	  
	  private void setLog4JFilter() {
		  
		  Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			  @Override
			  public void run() {
				  org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
				  coreLogger.addFilter(new Log4JFilter());
			  }
		  });
	  }
  
	  private void registerPlayer(Player player) {
		  String lwcPlayername = EasyLogin.getlowerCasePlayerName(player);
		  if (registering.containsKey(lwcPlayername))
			  return;
		  EasyLogin.callBungeeCoord(player, "Register", "#start#"+lwcPlayername+"#");
		  RegistrationConversation c = new RegistrationConversation(this, player, "stop", new Registration(player));
		  c.getConversation().setLocalEchoEnabled(false);
		  c.getConversation().begin();
		  registering.put(lwcPlayername, c);
	  }
	  
	  public void removeRegisteringPlayer(Player player) {
		  String lwcPlayername = player.getName().toLowerCase();
		  EasyLogin.callBungeeCoord(player, "Register", "#exit#"+lwcPlayername+"#");
		  this.registering.remove(lwcPlayername);
	  }
}

class Registration implements ConversationAbandonedListener {
	
	private Player player;
	public Registration(Player player) {
		this.player = player;
	}
	
	private boolean isValidString(String s) {
        return (s != null) && (!s.isEmpty());    
    }

    @Override
    public void conversationAbandoned(ConversationAbandonedEvent event) {

    	EasyLogin.instance.removeRegisteringPlayer(player);
    	if (event.gracefulExit()) {
    		EasyLogin.instance.getServer().getLogger().info("Registering finished");
    		
    		String password = (String)event.getContext().getSessionData(RegistrationConversation.SESSION_PASSWORD);
    		String email = (String)event.getContext().getSessionData(RegistrationConversation.SESSION_EMAIL);
    		
    		if (!isValidString(password))
    			return;
    		if (!isValidString(email))
    			return;
    		
    		RegistrationResult res = EasyLogin.database.registerUser(player.getName(), password, email);
    		if (res ==  RegistrationResult.SUCCESS) {
    			EasyLogin.instance.onPlayerJoin(player);
        		EasyLogin.callBungeeCoord(player, "Register", "#login#"+player.getName().toLowerCase()+"#");
        		player.sendMessage(ChatColor.GREEN + "Registrierung abgeschlossen!");
        		player.sendMessage(ChatColor.GREEN + "Bitte logge dich bitte jetzt mit deinem Passwort an:");
        		
    		} else if (res ==  RegistrationResult.USER_ALREADY_REGISTERED) {
    			player.sendMessage(ChatColor.RED + "Du bist bereits registriert!");
    			
    		} else if (res ==  RegistrationResult. FAILED) {
    			player.sendMessage(ChatColor.RED + "Die Registrierung ist fehlgeschlagen, versuche es vielleicht später nocheinmal!");
    		
    		} else if (res ==  RegistrationResult.UNKNOWN_ERROR) {
    			player.sendMessage(ChatColor.RED + "Ein unbekannter Fehler ist aufgetreten. Bitte wende dich an einen Admin!");
    		}
    		
    	} else {
    		event.getContext().getForWhom().sendRawMessage(ChatColor.RED + "Registrierung abgebrochen!");
    		EasyLogin.instance.getServer().getLogger().info("Registering aborted");
    	}
		
    }

}
