package me.markus.easylogin;

import java.util.Date;

public class LoginTrial {

	public String playerName;
	public Date lastLogin;
	public int trialNr;
	public String lastIP;

	public LoginTrial(String playerName) {
		this.playerName = playerName;
		this.lastLogin = new Date();
		this.trialNr = -1;
		this.lastIP = "";
	}

	public long waitForNextLogin() {
		if (Settings.getWaittimeIncrement == 0)
			return 0;
		Date now = new Date();
		return lastLogin.getTime() + Settings.getWaittimeIncrement * 1000 * (int) Math.pow(2, trialNr) - now.getTime();
	}

	public void addMissedLogin(String lastIP) {
		this.trialNr++;
		this.lastLogin = new Date();
		this.lastIP = lastIP;
	}

}
