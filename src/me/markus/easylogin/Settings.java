package me.markus.easylogin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class Settings {

	public static String getMySQLHost;
	public static String getMySQLPort;
	public static String getMySQLUsername;
	public static String getMySQLDatabase;
	public static String getMySQLTablename;
	public static String getMySQLPassword;
	public static boolean isStopEnabled;
	public static String getMySQLColumnName;
	public static String getMySQLColumnPassword;
	public static String getMySQLColumnSalt;
	public static int getMinNickLength;
	public static int getMaxNickLength;
	public static String getNickRegex;
	public static int getWaittimeIncrement;
	public static int getLoginsPerTenSeconds;
	public static int getNrAllowedGuests;
	public static boolean isWhitelisted;
	public static int getPurgeInterval;
	public static int getPurgeThreshold;

	public static void loadSettings() {

		// Default settings
		getMySQLHost = "foo.server.com";
		getMySQLPort = "1234";
		getMySQLUsername = "sqlAdmin";
		getMySQLDatabase = "forumDB";
		getMySQLTablename = "all_users";
		getMySQLPassword = "foobar";
		isStopEnabled = true;
		getMySQLColumnName = "username";
		getMySQLColumnPassword = "password";
		getMySQLColumnSalt = "salt";
		getMinNickLength = 3;
		getMaxNickLength = 20;
		getNickRegex = "[a-zA-Z0-9_?]*";
		getLoginsPerTenSeconds = 20;
		getWaittimeIncrement = 1;
		getNrAllowedGuests = 30;
		getPurgeInterval = 60;
		getPurgeThreshold = 3;
		isWhitelisted = false;

		File file = new File(EasyLogin.instance.getDataFolder(), "config.yml");
		if (!file.exists())
			saveSettings();
		YamlConfiguration yaml = new YamlConfiguration();
		try {
			yaml.load(file);
			
			getMySQLHost = yaml.getString("Datasource.mySQLHost");
			getMySQLPort = yaml.getString("Datasource.mySQLPort");
			getMySQLUsername = yaml.getString("Datasource.mySQLUsername");
			getMySQLDatabase = yaml.getString("Datasource.mySQLDatabase");
			getMySQLTablename = yaml.getString("Datasource.mySQLTablename");
			getMySQLPassword = yaml.getString("Datasource.mySQLPassword");
			getMySQLColumnName = yaml.getString("Datasource.mySQLColumnName");
			getMySQLColumnPassword = yaml.getString("Datasource.mySQLColumnPassword");
			getMySQLColumnSalt = yaml.getString("Datasource.mySQLColumnSalt");

			isStopEnabled = yaml.getBoolean("Security.SQLProblem.stopServer");

			getMinNickLength = yaml.getInt("restrictions.minNicknameLength");
			getMaxNickLength = yaml.getInt("restrictions.maxNicknameLength");
			getNickRegex = yaml.getString("restrictions.allowedChars");
			getWaittimeIncrement = yaml.getInt("restrictions.waittimeIncrement");

			getLoginsPerTenSeconds = yaml.getInt("antibot.loginsPer10Seconds");
			getNrAllowedGuests = yaml.getInt("antibot.nrAllowedGuests");
			
			getPurgeInterval = yaml.getInt("purge.interval");
			getPurgeThreshold = yaml.getInt("purge.amountOfLogins");
			
			isWhitelisted = yaml.getBoolean("whitelist");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		saveSettings();
	}

	public static void saveSettings() {
		File file = new File(EasyLogin.instance.getDataFolder(), "config.yml");
		YamlConfiguration yaml = new YamlConfiguration();

		yaml.set("Datasource.mySQLHost", getMySQLHost);
		yaml.set("Datasource.mySQLPort", getMySQLPort);
		yaml.set("Datasource.mySQLUsername", getMySQLUsername);
		yaml.set("Datasource.mySQLPassword", getMySQLPassword);
		yaml.set("Datasource.mySQLTablename", getMySQLTablename);
		yaml.set("Datasource.mySQLDatabase", getMySQLDatabase);
		yaml.set("Datasource.mySQLColumnName", getMySQLColumnName);
		yaml.set("Datasource.mySQLColumnPassword", getMySQLColumnPassword);
		yaml.set("Datasource.mySQLColumnSalt", getMySQLColumnSalt);

		yaml.set("Security.SQLProblem.stopServer", isStopEnabled);

		yaml.set("restrictions.minNicknameLength", getMinNickLength);
		yaml.set("restrictions.maxNicknameLength", getMaxNickLength);
		yaml.set("restrictions.allowedChars", getNickRegex);
		yaml.set("restrictions.waittimeIncrement", getWaittimeIncrement);

		yaml.set("antibot.loginsPer10Seconds", getLoginsPerTenSeconds);
		yaml.set("antibot.nrAllowedGuests", getNrAllowedGuests);
		
		yaml.set("purge.interval", getPurgeInterval);
		yaml.set("purge.amountOfLogins", getPurgeThreshold);
		
		yaml.set("whitelist", isWhitelisted);

		try {
			yaml.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
