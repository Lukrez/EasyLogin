package me.markus.easylogin;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerInfo {

	private String playerName;
	private int taskId;
	private String[] groups;
	private PlayerAuth playerAuth;

	public PlayerInfo(String playerName, PlayerAuth playerAuth) {
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
			EasyLogin.permission.playerRemoveGroup(player, group);
		}
		EasyLogin.permission.playerAddGroup(player, "UnloggedinUser");
		return true;
	}

	public boolean removeUnloggedinUser() {
		Player player = Bukkit.getPlayerExact(this.playerName);
		if (player == null)
			return false;
		EasyLogin.permission.playerRemoveGroup(player, "UnloggedinUser");
		for (int i = this.groups.length - 1; i >= 0; i--) {
			EasyLogin.permission.playerAddGroup(player, this.groups[i]);
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

	public String getHash(String password, String salt) throws NoSuchAlgorithmException {
		return getSHA1(salt.concat(getSHA1(salt.concat(getSHA1(password)))));
	}

	public boolean checkPassword(String password) throws NoSuchAlgorithmException {
		return this.playerAuth.passwordHash.equals(getHash(password, this.playerAuth.salt));
	}

	private String getSHA1(String message) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		sha1.reset();
		sha1.update(message.getBytes());
		byte[] digest = sha1.digest();
		return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
	}

}
