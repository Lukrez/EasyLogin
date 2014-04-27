package me.markus.easylogin;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EasyLogin extends JavaPlugin implements Listener {

	public static EasyLogin instance;
	public static Permission permission;
	public static MySQLDataSource database;
	private HashMap<String, PlayerInfo> players = new HashMap<String, PlayerInfo>();
	private HashMap<String, LoginTrial> loginTrials = new HashMap<String, LoginTrial>();
	private ArrayList<String> guests = new ArrayList<String>();
	private int nrLogins;
	private long lastLoginCycle;
	private boolean spamBotAttack;
	private int purgeTaskId = -1;
	private long nexPurge;
	private String whitelistReason = "Der Server ist momentan wegen Wartungsarbeiten im Whitelist-Modus. Vielleicht gibts im Forum nähere Infos!";

	@Override
	public void onDisable() {
		if (database != null)
			database.close();
		//set groups of all inloggedinusers and kick them
		for (PlayerInfo pi : this.players.values()) {
			pi.removeUnloggedinUser();
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
		this.nrLogins = 0;
		this.lastLoginCycle = 0;
		this.spamBotAttack = false;
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
				if (this.spamBotAttack) {
					sender.sendMessage(ChatColor.GRAY + "Spambot-Attacke: " + ChatColor.RED + "JA");
				} else {
					sender.sendMessage(ChatColor.GRAY + "Spambot-Attacke: " + ChatColor.GREEN + "NEIN");
				}
				double waittime = (new Date().getTime() - this.lastLoginCycle) / 1000;
				if (waittime > 0) {
					sender.sendMessage(ChatColor.GRAY + "Loginfrequenz: " + ChatColor.WHITE + this.nrLogins / waittime);
				} else {
					sender.sendMessage(ChatColor.GRAY + "Loginfrequenz: " + ChatColor.WHITE + "??");
				}

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
					sender.sendMessage(ChatColor.RED + "Bitte die Anzahl an Einloggversuche pro 10 Sekunden angeben! (0 für AUS)");
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
			PlayerInfo pi = this.players.get(player.getName());
			if (pi == null) {
				player.sendMessage(ChatColor.GREEN + "Du bist bereits eingeloggt oder Gast auf dem Server. Wenn du dich soeben registriert hast starte Minecraft einmal neu!");
				return true;
			}

			if (args.length == 0) {
				player.sendMessage(ChatColor.RED + "Bitte logge dich mit /l <password> innerhalb von 30 Sekunden ein.");
				return true;
			}

			if (!pi.checkPassword(args[0])) {
				player.kickPlayer(ChatColor.RED + "Falsches Passwort!");
				this.getLogger().info("Spieler "+ player.getName() + " hat ein falsches Passwort eingegeben!");
				return true;
			}

			this.players.remove(player.getName());
			if (pi.removeUnloggedinUser()) {
				player.sendMessage(ChatColor.GREEN + "Login erfolgreich.");
				this.getLogger().info("Spieler "+ player.getName() + " hat sich erfolgreich eingeloggt!");
				pi.cancelTask();

				// remove possible loginTrials
				this.loginTrials.remove(player.getName());

				Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
			} else {
				player.sendMessage(ChatColor.RED + "Ein Fehler beim Verschieben in deine alte Gruppe ist aufgetreten. Bitte kontaktiere ein Staffmitglied!");
			}

			return true;
		}

		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(AsyncPlayerPreLoginEvent event) { // Search for Kick reasons

		// Anti-Spambot
		// calculate threshold
		this.nrLogins++;
		if (Settings.getLoginsPerTenSeconds > 0 && this.nrLogins > Settings.getLoginsPerTenSeconds) {
			this.nrLogins = 0;
			this.lastLoginCycle = new Date().getTime();
		}
		if (Settings.getLoginsPerTenSeconds > 0 && new Date().getTime() - this.lastLoginCycle < 10000) { // TODO: logins > 0 ??
			this.spamBotAttack = true;
		} else {
			this.spamBotAttack = false;
		}

		String playerName = event.getName();

		int min = Settings.getMinNickLength;
		int max = Settings.getMaxNickLength;
		String regex = Settings.getNickRegex;

		if (playerName.length() > max || playerName.length() < min) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Dein Minecraftname muss zwischen " + min + " und " + max + " Zeichen lang sein!");
			return;
		}

		if (!playerName.matches(regex) || playerName.equals("Player")) {
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

		// SpamBot
		if (this.spamBotAttack || Settings.getNrAllowedGuests < this.guests.size()) {
			// Nur unregistrierte?
			PlayerAuth playerAuth = database.getAuth(playerName.toLowerCase());
			if (playerAuth == null) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Bitte registriere dich auf www.minecraft-spielewiese.de. Bis gleich :-)");
				return;
			}
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
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.isOp()) {
			player.setOp(false);
		}
		// a) Already Logged in -> Kick?
		// b) Unregistered -> Guest
		PlayerAuth playerAuth = database.getAuth(player.getName().toLowerCase());
		if (playerAuth == null) {
			// remove all previous groups
			for (String group : permission.getPlayerGroups(player)) {
				permission.playerRemoveGroup(player, group);
			}
			// set Guest group
			permission.playerAddGroup(player, "Guest");
			this.guests.add(player.getName());
			player.sendMessage(ChatColor.GREEN + "Willkommen auf dem Minecraft-Spielewiese Server!");
			return;
		}
		boolean isNewTraveller = true;
		for (String group : permission.getPlayerGroups(player)) {
			if (!group.equals("Guest")) {
				isNewTraveller = false;
				break;
			}

		}
		if (isNewTraveller) {
		      permission.playerAddGroup("", player.getName(), "Traveller"); // New pex system
		      permission.playerRemoveGroup("", player.getName(), "Guest");
		}

		// c) Registered -> UnloggedinUsers -> After Login in old groups
		PlayerInfo pi = new PlayerInfo(player.getName(), playerAuth, player.getLocation()); // TODO: Already logged in?
		this.players.put(player.getName(), pi);
		LoginTrial lt = (LoginTrial)this.loginTrials.get(player.getName());
	    if (lt == null)
	    {
	      lt = new LoginTrial(player.getName());
	      this.loginTrials.put(player.getName(), lt);
	    }
	    lt.addLogin(player.getAddress().toString());
		player.sendMessage(ChatColor.RED + "Bitte logge dich mit /l <password> innerhalb von 30 Sekunden ein.");
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = this.players.get(player.getName());
		if (pi != null) {
			pi.removeUnloggedinUser();
			pi.cancelTask();
			player.teleport(pi.getLocation());
		}
		this.players.remove(player.getName());
		this.guests.remove(player.getName());

	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = this.players.get(player.getName());
		if (pi != null) {
			pi.removeUnloggedinUser();
			pi.cancelTask();
			player.teleport(pi.getLocation());
		}
		this.players.remove(player.getName());
		this.guests.remove(player.getName());
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked().getType() != EntityType.PLAYER)
			return;

		Player player = (Player) event.getWhoClicked();
		event.setCancelled(this.players.containsKey(player.getName()));
	}

	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		event.setCancelled(this.players.containsKey(event.getPlayer().getName()));
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		event.setCancelled(this.players.containsKey(event.getPlayer().getName()));
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
}
