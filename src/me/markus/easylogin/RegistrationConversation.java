package me.markus.easylogin;

import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.RegexPrompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.plugin.Plugin;


public class RegistrationConversation {
    
    private Conversation p_conversation;
    
    private static final String REGISTRATION_START_MSG = ChatColor.GREEN + "Um die Registrierung abzubrechen, gib im Chat "+ChatColor.RED + ChatColor.BOLD+"stop"+ChatColor.GREEN+" ein!";
    private static final String REGISTRATION_ENTER_PW = ChatColor.GREEN + "Bitte gib ein " + ChatColor.GOLD + "Passwort"+ChatColor.GREEN+" ein (mindestens 8 Zeichen lang):";
    private static final String REGISTRATION_ENTER_PW_FAILED = ChatColor.RED + "Dein Passwort muss mindestens 8 Zeichen lang sein!";
    private static final String REGISTRATION_CONFIRM_PW = ChatColor.GREEN + "Bitte gib dein " + ChatColor.GOLD + "Passwort"+ChatColor.GREEN+" erneut ein:";
    private static final String REGISTRATION_CONFIRM_PW_FAILED = ChatColor.RED + "Die eingegebenen Passwörter sind nicht identisch!";
    private static final String REGISTRATION_ENTER_MAIL = ChatColor.GREEN + "Bitte gib eine " + ChatColor.GOLD + "Email-Addresse" + ChatColor.GREEN + " ein:";
    private static final String REGISTRATION_ENTER_MAIL_FAILED = ChatColor.RED + "Bitte gib eine gültige Email-Addresse ein!";
    private static final String REGISTRATION_COMPLETE = ChatColor.GOLD + "Registrierung abgeschlossen!";
    
    public static final String SESSION_PASSWORD = "password";
    public static final String SESSION_EMAIL = "email";
    public static final String SESSION_RESULT = "result";

    public RegistrationConversation(Plugin plugin, Conversable forWhom, String escapeString, ConversationAbandonedListener _interface) {
        this.p_conversation = 
                new ConversationFactory(plugin)
                .withFirstPrompt(new RegistrationStartPromt())
                .thatExcludesNonPlayersWithMessage("YOU ARE NOT A PLAYER!")
                .withEscapeSequence(escapeString)
                .addConversationAbandonedListener(_interface)
                .buildConversation(forWhom);
    }
    
    public Conversation getConversation() {
        return this.p_conversation;
    }
        
    private class RegistrationStartPromt extends MessagePrompt {

        @Override
        public String getPromptText(ConversationContext context) {
        	context.getForWhom().sendRawMessage(ChatColor.GREEN + "-------------------"+ ChatColor.GOLD + " Registrierung "+ChatColor.GREEN+"-------------------");
            return RegistrationConversation.REGISTRATION_START_MSG;
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext arg0) {
            return new RegistrationConversation.RegistrationPW1Promt();
        }
        
    }
    
    private class RegistrationPW1Promt extends ValidatingPrompt {

        @Override
        public String getPromptText(ConversationContext arg0) {
            return RegistrationConversation.REGISTRATION_ENTER_PW;
        }       

        @Override
        protected boolean isInputValid(ConversationContext arg0, String arg1) {

            if (arg1.isEmpty() || arg1.length() < 8 || arg1.startsWith("/") || arg1.contains(" "))
                return false;
            
            arg0.setSessionData(RegistrationConversation.SESSION_PASSWORD, arg1);
            arg0.setSessionData(RegistrationConversation.SESSION_RESULT, RegistrationConversation.Result.OK);
            return true;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, String invalidInput) {
            return RegistrationConversation.REGISTRATION_ENTER_PW_FAILED;
        }
        
        @Override
        protected Prompt acceptValidatedInput(ConversationContext arg0, String arg1) {
            return new RegistrationPW2Promt();
        }
        
    }
    
    private class RegistrationPW2Promt extends ValidatingPrompt {

        @Override
        public String getPromptText(ConversationContext arg0) {
            return RegistrationConversation.REGISTRATION_CONFIRM_PW;
        }

        @Override
        protected boolean isInputValid(ConversationContext arg0, String arg1) {

            if (!arg0.getSessionData(RegistrationConversation.SESSION_PASSWORD).equals(arg1)) 
                arg0.setSessionData(RegistrationConversation.SESSION_RESULT, RegistrationConversation.Result.FAILED);
            return true;
        }
        
        @Override
        protected Prompt acceptValidatedInput(ConversationContext arg0, String arg1) {

            if ((RegistrationConversation.Result) arg0.getSessionData(RegistrationConversation.SESSION_RESULT) ==  RegistrationConversation.Result.OK ) 
                return new RegistrationEMailPromt();
            else {
                arg0.getForWhom().sendRawMessage(RegistrationConversation.REGISTRATION_CONFIRM_PW_FAILED);
                return new RegistrationConversation.RegistrationPW1Promt();
            }
            
        }
        
    }
    
    private class RegistrationEMailPromt extends RegexPrompt {
    	
    	public RegistrationEMailPromt() {
    		super(Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE));
    	}
    	
        @Override
        public String getPromptText(ConversationContext arg0) {
            return RegistrationConversation.REGISTRATION_ENTER_MAIL;
        }
        
        @Override
        protected boolean isInputValid(ConversationContext context, String input) {

            if (super.isInputValid(context, input)) {
                context.setSessionData(RegistrationConversation.SESSION_EMAIL, input);
                return true;
            } 
            else 
                return false;
        }
        
        @Override
        protected String getFailedValidationText(ConversationContext context, String invalidInput) {
            return RegistrationConversation.REGISTRATION_ENTER_MAIL_FAILED;
        }
        
        @Override
        protected Prompt acceptValidatedInput(ConversationContext arg0, String arg1) {
            return new RegistrationConfirmPromt();
        }
        
    }
    
    private class RegistrationConfirmPromt extends ValidatingPrompt{

        @Override
        public String getPromptText(ConversationContext context) {
            String password = (String) context.getSessionData(RegistrationConversation.SESSION_PASSWORD); 
            String email = (String) context.getSessionData(RegistrationConversation.SESSION_EMAIL);
            
            context.getForWhom().sendRawMessage(ChatColor.GREEN + "--------------------"+ ChatColor.GOLD + " Deine Daten "+ChatColor.GREEN+"--------------------");
            context.getForWhom().sendRawMessage(ChatColor.RED + "1."+ChatColor.GREEN+" Passwort: " + ChatColor.GOLD + password);
            context.getForWhom().sendRawMessage(ChatColor.RED + "2."+ChatColor.GREEN+" Email: " + ChatColor.GOLD + email);
            return ChatColor.GREEN + "Sind die Daten okay? "+ChatColor.GOLD+"Ja"+ChatColor.GREEN+"/"+ChatColor.RED+"Nein";
        }

        @Override
        protected boolean isInputValid(ConversationContext arg0, String arg1) {
            
            if (!(arg1 != null && ( arg1.equalsIgnoreCase("ja") || arg1.equalsIgnoreCase("j") || arg1.equalsIgnoreCase("yes") || arg1.equalsIgnoreCase("y") || arg1.equalsIgnoreCase("ok") )))                 
                arg0.setSessionData(RegistrationConversation.SESSION_RESULT, RegistrationConversation.Result.FAILED);
            return true;
        }
        
        @Override
        protected Prompt acceptValidatedInput(ConversationContext arg0, String arg1) {

            if ((RegistrationConversation.Result) arg0.getSessionData(RegistrationConversation.SESSION_RESULT) ==  RegistrationConversation.Result.OK ) 
                return new RegistrationFinishedPromt();
            else 
                return new RegistrationConversation.RegistrationPW1Promt();   
        }
        
    }
    
    private class RegistrationFinishedPromt extends MessagePrompt {
                
        @Override
        public String getPromptText(ConversationContext arg0) {
            return RegistrationConversation.REGISTRATION_COMPLETE;
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext arg0) {
            return END_OF_CONVERSATION;
        }
    }
    
    private static enum Result {
        OK, 
        FAILED;
    }
}
