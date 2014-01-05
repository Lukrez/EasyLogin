package me.markus.easylogin;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EasyLogin extends JavaPlugin implements Listener {

	public static Plugin instance;
	public static Permission permission;
	public static MySQLDataSource database;
	private HashMap<String, PlayerInfo> players = new HashMap<String, PlayerInfo>();
	private HashMap<String, LoginTrial> loginTrials = new HashMap<String, LoginTrial>();

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
				sender.sendMessage("/easylogin list - Listet alle Spieler mit momentanen Wartezeiten auf");
				sender.sendMessage("/easylogin purge - Löscht alle Einlogdateien");
				sender.sendMessage("/easylogin show <player name> - Zeigt an ob der angebene User eine Loginbeschränkung hat");
				sender.sendMessage("/easylogin pardon <player name> - Löscht die Loginbeschränkung für den User");
				return true;
			}

			if (args[0].equalsIgnoreCase("reload")) {
				Settings.loadSettings();
				sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
				return true;
			}
			if (args[0].equalsIgnoreCase("list")) {
				sender.sendMessage(ChatColor.GREEN + "Spieler mit momentanen Login-Wartezeiten:");
				for (LoginTrial lt : this.loginTrials.values()) {
					sender.sendMessage(lt.playerName + ": " + (int) lt.waitForNextLogin() / 1000 + " sec" + " - " + lt.lastIP);
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
				sender.sendMessage(lt.playerName + ": " + (int) lt.waitForNextLogin() / 1000 + " sec" + " - " + lt.lastIP);
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
			return false;
		}

		if (!(sender instanceof Player)) {
			return true;
		}

		Player player = (Player) sender;

		if (command.getName().equalsIgnoreCase("login")) {
			PlayerInfo pi = this.players.get(player.getName());
			if (pi == null) {
				player.sendMessage(ChatColor.GREEN + "Du bist bereits eingeloggt oder Gast auf dem Server.");
				return true;
			}

			if (args.length == 0) {
				player.sendMessage(ChatColor.RED + "Bitte logge dich mit /l <password> innherhalb von 30 Sekunden ein.");
				return true;
			}

			try {
				if (!pi.checkPassword(args[0])) {
					player.kickPlayer(ChatColor.RED + "Falsches Passwort!");
					LoginTrial lt = loginTrials.get(player.getName());
					if (lt == null) {
						lt = new LoginTrial(player.getName());
						this.loginTrials.put(player.getName(), lt);
					}
					lt.addMissedLogin(player.getAddress().getHostName());

					return true;
				}
			} catch (NoSuchAlgorithmException e) {
				player.sendMessage(ChatColor.RED + "Konnte Passwort nicht überprüfen. Wende dich bitte an ein Staffmitglied!");
				e.printStackTrace();
			}
			this.players.remove(player.getName());
			if (pi.removeUnloggedinUser()) {
				player.sendMessage(ChatColor.GREEN + "Login erfolgreich.");
				pi.cancelTask();

				// remove possible loginTrials
				this.loginTrials.remove(player.getName());

				Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
			} else {
				player.sendMessage(ChatColor.RED + "Ein Fehler beim Verschieben in deine alte Gruppe ist aufgetreten. Bitte kontaktiere ein Staffmitglied!");
			}

			return true;
		}

		/*if (command.getName().equalsIgnoreCase("register")) {
			String password = args[0];
			String salt = "HelloWorld";
			PlayerInfo pi = new PlayerInfo();
			String passwordHash = "";
			try {
				passwordHash = pi.getHash(password, salt);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return true;
			}
			try {
				database.registerUser(player.getName(), passwordHash, salt);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			player.sendMessage("Erfolgreich registriert!");
		}*/

		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(AsyncPlayerPreLoginEvent event) { // Search for Kick reasons
		String playerName = event.getName();

		int min = Settings.getMinNickLength;
		int max = Settings.getMaxNickLength;
		String regex = Settings.getNickRegex;

		if (playerName.length() > max || playerName.length() < min) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Dein Minecraftname muss zwischen" + min + " und " + max + " Zeichen lang sein!");
			return;
		}

		if (!playerName.matches(regex) || playerName.equals("Player")) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Dein Minecraftname enthält nicht erlaubte Sonderzeichen!");
			return;
		}

		// Anti-Spambot
		if (Settings.getMaxUnloggedinUsers > 0 && this.players.size() > Settings.getMaxUnloggedinUsers) {
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
			permission.playerRemoveGroup(player, "Guest");
			permission.playerAddGroup(player, "Traveller");
		}

		// c) Registered -> UnloggedinUsers -> After Login in old groups
		PlayerInfo pi = new PlayerInfo(player.getName(), playerAuth); // TODO: Already logged in?
		this.players.put(player.getName(), pi);
		player.sendMessage(ChatColor.RED + "Bitte logge dich mit /l <password> innherhalb von 30 Sekunden ein.");
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = this.players.get(player.getName());
		if (pi != null) {
			pi.removeUnloggedinUser();
			pi.cancelTask();
		}
		this.players.remove(player.getName());

	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = this.players.get(player.getName());
		if (pi != null) {
			pi.removeUnloggedinUser();
			pi.cancelTask();
		}
		this.players.remove(player.getName());
	}

	/*private void sendDelayedMessage(final String playerName, final String message){
		this.getServer().getScheduler()
		.scheduleSyncDelayedTask(this, new BukkitRunnable() {

			@Override
			public void run() {
				Player player = Bukkit.getPlayerExact(playerName);
				if (player != null)
				player.sendMessage(message);
			}
		}, 20);
	}*/

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
}
