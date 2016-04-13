package me.markus.easylogin;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public class Log4JFilter implements org.apache.logging.log4j.core.Filter {

    public Log4JFilter() {
    }
    
    private Result checkString(String msg) {
    	
    	try {
    		
	         if (!msg.contains("issued server command:"))
	             return Result.NEUTRAL;
	         if (!msg.contains("/login ") && !msg.contains("/l "))
	             return Result.NEUTRAL;
	         return Result.DENY;
	         
	    } catch (NullPointerException npe) {
	        return Result.NEUTRAL;
	    }
    }

    @Override
    public Result filter(LogEvent record) {
    	
        if (record == null)
        	return Result.NEUTRAL;
        if (record.getMessage() == null)
            return Result.NEUTRAL;
        if (record.getMessage().getFormattedMessage() == null)
        	return Result.NEUTRAL;
        
        String logM = record.getMessage().getFormattedMessage().toLowerCase();        
        return this.checkString(logM);          
         
    }

    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object... arg4) {

        if (message == null)
            return Result.NEUTRAL;
        
        String logM = message.toLowerCase();        
        return this.checkString(logM);       

    }

    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, Object message,
            Throwable arg4) {

        if (message == null)
            return Result.NEUTRAL;
        String logM = message.toString().toLowerCase();
        return this.checkString(logM); 
    }

    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, Message message,
            Throwable arg4) {

        if (message == null)
            return Result.NEUTRAL;
        String logM = message.getFormattedMessage().toLowerCase();
        return this.checkString(logM); 
    }

    @Override
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    @Override
    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }

}
