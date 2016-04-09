package me.markus.easylogin;

import org.bukkit.entity.Player;

public class RegisterInfo {
	
	Player player;
	private String email = "";
	private String password = "";
	boolean check_ok = false;
	//boolean ok = false;
	
	public RegisterInfo(Player player) {
		this.player = player;
		player.sendMessage("Willkommen! Um die Regisrierung abzubrechen,gib im Chat 'stop' ein!");
		player.sendMessage("Ansonsten gib ein Passwort ein (mindestens 8 Zeichen lang):");
	}
	
	public void newResponse(String message) {
		if (message.toLowerCase().replaceAll(" ", "").equals("stop")) {
			EasyLogin.instance.endRegisteringPlayer(player.getName());
		}
		if (password.equals("")) {
			if (message.length() < 8) {
				player.sendMessage("Dein Passwort muss mindestens 8 Zeichen lang sein");
				return;
			}
			password = message;
			
			player.sendMessage("Gib nun bitte eine Email-Addresse ein, um dein Passwort notfalls zurücksetzen zu können:");
			
		} else if (email.equals("")) {
			if (!message.matches(".+@.+\\..+")) {
				player.sendMessage("Bitte gib eine gültige Email-Addresse ein!");
				return;
			}
			email = message;
			
			player.sendMessage("Überprüfe deine Daten:");
			player.sendMessage("Passwort:" + password);
			player.sendMessage("Email:" + email);
			player.sendMessage("Sind alle Daten korrekt? Ja, P, E");
			
			
		} else if (check_ok == false) {
			String strp = message.toLowerCase().replaceAll(" ", "");
			if (strp.equals("ja")) {
				check_ok = true;
				player.sendMessage("Registrierung abgeschlossen!");
				EasyLogin.instance.endRegisteringPlayer(player.getName());
				// TODO: register and login player
			} else if (strp.equals("p")) {
				password = "";
			} else if (strp.equals("e")) {
				email = "";
			} else {
				player.sendMessage("Gib entweder 'Ja','P' für Passwort order 'E' für Email-Änderungen ein!");
			}
			
		} else {
			EasyLogin.instance.endRegisteringPlayer(player.getName());
		}
		//System.out.println("recvieved message: " + message);
	}

}
