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
	public static String getMySQLColumnLoginStatus;
	public static int getMinNickLength;
	public static int getMaxNickLength;
	public static String getNickRegex;
	public static int getWaittimeIncrement;
	public static int getLoginsPerTenSeconds;
	public static int getNrAllowedGuests;
	public static boolean isWhitelisted;
	public static int getPurgeInterval;
	public static int getPurgeThreshold;
	public static boolean useBungeeCoord;
	public static boolean isConsoleFilterEnabled;
	public static boolean registerAllowRegistration;
	public static boolean registerUseLocationLimiter;
	public static String registerWorldname;
	public static int[] registerLoc1;
	public static int[] registerLoc2;

	public static void loadSettings() {

		// Default settings
		getMySQLHost = "foo.server.com";
		getMySQLPort = "1234";
		getMySQLUsername = "sqlAdmin";
		getMySQLDatabase = "forumDB";
		getMySQLTablename = "all_users";
		getMySQLPassword = "foobar";
		isStopEnabled = true;
		useBungeeCoord = false;
		getMySQLColumnName = "username";
		getMySQLColumnPassword = "password";
		getMySQLColumnLoginStatus = "loginStatus";
		getMinNickLength = 3;
		getMaxNickLength = 20;
		getNickRegex = "[a-zA-Z0-9_?]*";
		getLoginsPerTenSeconds = 20;
		getWaittimeIncrement = 1;
		getNrAllowedGuests = 30;
		getPurgeInterval = 60;
		getPurgeThreshold = 3;
		isWhitelisted = false;		
		isConsoleFilterEnabled = true;
		registerAllowRegistration = false;
		registerUseLocationLimiter = false;
		registerWorldname = "world";
		registerLoc1 = new int[] {0,0,0};
		registerLoc2 = new int[] {0,0,0};

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
			getMySQLColumnLoginStatus = yaml.getString("Datasource.mySQLColumnLoginStatus");
			
			isStopEnabled = yaml.getBoolean("Security.SQLProblem.stopServer");
			useBungeeCoord = yaml.getBoolean("Security.useBungeecoord");
			isConsoleFilterEnabled = yaml.getBoolean("Security.enableConsoleFilter");

			getMinNickLength = yaml.getInt("restrictions.minNicknameLength");
			getMaxNickLength = yaml.getInt("restrictions.maxNicknameLength");
			getNickRegex = yaml.getString("restrictions.allowedChars");
			getWaittimeIncrement = yaml.getInt("restrictions.waittimeIncrement");

			getLoginsPerTenSeconds = yaml.getInt("antibot.loginsPer10Seconds");
			getNrAllowedGuests = yaml.getInt("antibot.nrAllowedGuests");
			
			getPurgeInterval = yaml.getInt("purge.interval");
			getPurgeThreshold = yaml.getInt("purge.amountOfLogins");
			
			isWhitelisted = yaml.getBoolean("whitelist");
			
			registerAllowRegistration = yaml.getBoolean("register.allowRegistration");
			registerUseLocationLimiter = yaml.getBoolean("register.useLocation");
			registerWorldname = yaml.getString("register.world");
			int l_x1 = yaml.getInt("register.x1");
			int l_y1 = yaml.getInt("register.y1");
			int l_z1 = yaml.getInt("register.z1");
			int l_x2 = yaml.getInt("register.x2");
			int l_y2 = yaml.getInt("register.y2");
			int l_z2 = yaml.getInt("register.z2");

			
			int x1 = l_x1;
			int x2 = l_x2;
			if (l_x1 > l_x2){
				x1 = l_x2;
				x2 = l_x1;
			}
			
			int y1 = l_y1;
			int y2 = l_y2;
			if (l_y1 > l_y2){
				y1 = l_y2;
				y2 = l_y1;
			}
			
			int z1 = l_z1;
			int z2 = l_z2;
			if (l_z1 > l_z2){
				z1 = l_z2;
				z2 = l_z1;
			}
			registerLoc1 = new int[] {x1,y1,z1};
			registerLoc2 = new int[] {x2,y2,z2};

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		} catch (IndexOutOfBoundsException e) {
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
		yaml.set("Datasource.mySQLColumnLoginStatus", getMySQLColumnLoginStatus);

		yaml.set("Security.SQLProblem.stopServer", isStopEnabled);
		yaml.set("Security.useBungeecoord", useBungeeCoord);
		yaml.set("Security.enableConsoleFilter", isConsoleFilterEnabled);

		yaml.set("restrictions.minNicknameLength", getMinNickLength);
		yaml.set("restrictions.maxNicknameLength", getMaxNickLength);
		yaml.set("restrictions.allowedChars", getNickRegex);
		yaml.set("restrictions.waittimeIncrement", getWaittimeIncrement);

		yaml.set("antibot.loginsPer10Seconds", getLoginsPerTenSeconds);
		yaml.set("antibot.nrAllowedGuests", getNrAllowedGuests);
		
		yaml.set("purge.interval", getPurgeInterval);
		yaml.set("purge.amountOfLogins", getPurgeThreshold);
		
		yaml.set("whitelist", isWhitelisted);
		
		yaml.set("register.allowRegistration", registerAllowRegistration);
		yaml.set("register.useLocation", registerUseLocationLimiter);
		yaml.set("register.world", registerWorldname);
		yaml.set("register.x1", registerLoc1[0]);
		yaml.set("register.y1", registerLoc1[1]);
		yaml.set("register.z1", registerLoc1[2]);
		yaml.set("register.x2", registerLoc2[0]);
		yaml.set("register.y2", registerLoc2[1]);
		yaml.set("register.z2", registerLoc2[2]);
		

		try {
			yaml.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
