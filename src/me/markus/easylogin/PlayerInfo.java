package me.markus.easylogin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerInfo {

	private String playerName;
	private int taskId;
	private String[] groups;
	private PlayerAuth playerAuth;
	private Location location;

	public PlayerInfo(String playerName, PlayerAuth playerAuth, Location location) {
		this.playerName = playerName;
		this.playerAuth = playerAuth;
		this.groups = new String[0];
		if (!this.setUnloggedinUser()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null)
				player.kickPlayer("Ein Fehler beim Verschieben in die Gruppe <UnloggedinUser> ist aufgetreten. Kontaktiere bitte den Staff im Forum oder im TS!");
		}
		this.startTask();
	}

	public PlayerInfo() {
	};

	private boolean setUnloggedinUser() {
		Player player = Bukkit.getPlayer(this.playerName);
		if (player == null) {
			return false;
		}
		this.groups = EasyLogin.permission.getPlayerGroups(player);
		// remove old groups
		for (String group : this.groups) {
			 EasyLogin.permission.playerRemoveGroup("", player.getName(), group);
		}
		 EasyLogin.permission.playerAddGroup("", player.getName(), "UnloggedinUser");
		return true;
	}

	public boolean removeUnloggedinUser() {
		Player player = Bukkit.getPlayerExact(this.playerName);
		if (player == null)
			return false;
		EasyLogin.permission.playerRemoveGroup("", player.getName(), "UnloggedinUser");
		for (int i = this.groups.length - 1; i >= 0; i--) {
			EasyLogin.permission.playerAddGroup("", player.getName(), this.groups[i]);
		}
		return true;
	}

	private void startTask() {
		this.taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(EasyLogin.instance, new LoginTask(this), 600);
	}

	public void cancelTask() {
		Bukkit.getScheduler().cancelTask(this.taskId);
	}

	public String getPlayerName() {
		return this.playerName;
	}
	
	public Location getLocation(){
	    return this.location;
	}
	
	public boolean checkPassword(String cleartext){
		if (this.playerAuth == null)
			return false;
		return this.playerAuth.checkPswd(cleartext);
	}

	

}
