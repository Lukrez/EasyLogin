package me.markus.easylogin;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class LoginTask extends BukkitRunnable {

	private PlayerInfo pi;

	public LoginTask(PlayerInfo pi) {
		this.pi = pi;
	}

	@Override
	public void run() {
		this.pi.removeUnloggedinUser();
		Bukkit.getPlayer(this.pi.getPlayerName()).kickPlayer("Du hast dich nicht innerhalb von 30 Sekunden eingeloggt.");
	}
}
