package me.markus.easylogin;

public class PlayerAuth {
	public String playerName;
	public String passwordHash;
	public String salt;
	public String ip;

	public PlayerAuth(String playerName, String passwordHash, String salt, String ip) {
		this.playerName = playerName;
		this.passwordHash = passwordHash;
		this.salt = salt;
		this.ip = ip;
	}
}
