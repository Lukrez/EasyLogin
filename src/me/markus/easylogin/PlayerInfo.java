package me.markus.easylogin;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class PlayerInfo {

	private String playerName;
	private int taskId;
	private PlayerAuth playerAuth;
	private Location location;

	public PlayerInfo(String playerName, PlayerAuth playerAuth, Location location) {
		this.playerName = playerName.toLowerCase();
		this.playerAuth = playerAuth;
		this.location = location;
		this.startTask();
	}

	public PlayerInfo() {
	};


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
	
	public boolean checkPassword(String cleartext, String playername){
		if (this.playerAuth == null)
			return false;
		if (!this.playerName.equals(playername.toLowerCase()))
			return false;
		return this.playerAuth.checkPswd(cleartext, this.playerName);
	}

	

}
