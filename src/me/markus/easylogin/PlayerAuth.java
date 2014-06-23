package me.markus.easylogin;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PlayerAuth {
	private String playerName;
	private String passwordInfo;

	public PlayerAuth(String playerName, String passwordInfo) {
		this.playerName = playerName.toLowerCase();
		this.passwordInfo = passwordInfo;
	}
	
	
	public boolean checkPswd(String cleartext,String playername){
		if (!this.playerName.equals(playername.toLowerCase()))
			return false;
		// extract wbb version
		if (passwordInfo.startsWith("wcf1:")){
			return checkPswd_WCF1(cleartext,this.passwordInfo);
		} else if (passwordInfo.startsWith("$2")) {
			return checkPswd_WCF2(cleartext,this.passwordInfo);
		}
		
		return false;
	}
	
	
	private static boolean checkPswd_WCF1(String cleartext,  String passwordInfo){
		String[] split = passwordInfo.split(":");
		String passwordHash = split[1];
		String passwordSalt = split[2];
		try {
			return passwordHash.equals(getHash(cleartext,passwordSalt));
		} catch (NoSuchAlgorithmException e) {
			EasyLogin.instance.getLogger().severe("No Hashing algorithm!");
			e.printStackTrace();
			return false;
		}

	}
	
	
	private static boolean checkPswd_WCF2(String cleartext,  String passwordInfo){
		return passwordInfo.equals(getDoubleSaltedHash(cleartext, passwordInfo));
	}
	
	
	// ----- Methods for WCF1 hashing algorithm ----- //
	
	private static String getHash(String password, String salt) throws NoSuchAlgorithmException {
		return getSHA1(salt.concat(getSHA1(salt.concat(getSHA1(password)))));
	}

	private static String getSHA1(String message) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		sha1.reset();
		sha1.update(message.getBytes());
		byte[] digest = sha1.digest();
		return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
	}
	
	
	// ----- Methods for WCF2 hashing algorithm ----- //
	private static String getDoubleSaltedHash(String password, String salt) {		
		return getSaltedHash(getSaltedHash(password, salt), salt);
	}
	
	private static String getSaltedHash(String password, String salt) {		
		return BCrypt.hashpw(password, salt);
	}
}
