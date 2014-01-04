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

	
	public static void loadSettings(){
		
		File file = new File(EasyLogin.instance.getDataFolder(),"config.yml");
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

			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void saveSettings(){
		File file = new File(EasyLogin.instance.getDataFolder(),"config.yml");
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("Datasource.mySQLHost", "");
		yaml.set("Datasource.mySQLPort", "");
		yaml.set("Datasource.mySQLUsername", "");
		yaml.set("Datasource.mySQLPassword", "");
		yaml.set("Datasource.mySQLTablename", "");
		yaml.set("Datasource.mySQLDatabase", "");
		yaml.set("Security.SQLProblem.stopServer", "");
		yaml.set("Datasource.mySQLColumnName", "");
		yaml.set("Datasource.mySQLColumnPassword", "");
		yaml.set("Datasource.mySQLColumnSalt", "");
		
		try {
			yaml.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
