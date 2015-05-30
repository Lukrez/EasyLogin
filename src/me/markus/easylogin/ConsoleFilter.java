package me.markus.easylogin;


import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class ConsoleFilter implements Filter {
	
	private String pluginname = "";
	
	public ConsoleFilter() {
		this.pluginname = EasyLogin.instance.getDescription().getFullName();
	}
	
	@Override
	public boolean isLoggable(LogRecord record) {
		
		try {
			
			if (record == null || record.getMessage() == null)
				return true;
			
			String logM = record.getMessage().toLowerCase();
			
			if (!logM.contains("issued server command:"))
				return true;

			if (!logM.contains("/login ") &&
				!logM.contains("/l ")) 
	
					return true;
			
			String playername = record.getMessage().split(" ")[0];
			record.setMessage(playername + " issued command from "+ this.pluginname +".");
			return true;
			
		} 
		catch (NullPointerException npe) {
			return true;
		}
	}
}