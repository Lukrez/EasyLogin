package me.markus.easylogin;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import me.markus.easylogin.MiniConnectionPoolManager.TimeoutException;

public class MySQLDataSource {

	private String host;
	private String port;
	private String username;
	private String password;
	private String database;
	private String tableName;
	private String columnName;
	private String columnPassword;
	private String columnStatus;
	private MiniConnectionPoolManager conPool;
	
	// SQL statements
	private static final String INSERT_INTO = "INSERT INTO ";
	private static final String SELECT = "SELECT ";
	private static final String FROM = " FROM ";
	private static final String WHERE = " WHERE ";

	private static final String USER_TABLE = "wcf1_user";
	private static final String USER_TABLE_USERID = "userID";
	private static final String USER_TABLE_USERNAME = "username";
	private static final String USER_STORAGE_TABLE = "wcf1_user_storage";
	private static final String USER_TO_GROUP_TABLE = "wcf1_user_to_group";
	private static final String USER_TO_LANGUAGE_TABLE = "wcf1_user_to_language";
	private static final String USER_OPTION_VALUE_TABLE = "wcf1_user_option_value";
	private static final String USER_NOTIFICATION_EVENT_TO_USER_TABLE = "wcf1_user_notification_event_to_user";

	public MySQLDataSource() throws ClassNotFoundException, SQLException {
		this.host = Settings.getMySQLHost;
		this.port = Settings.getMySQLPort;
		this.username = Settings.getMySQLUsername;
		this.password = Settings.getMySQLPassword;
		this.database = Settings.getMySQLDatabase;
		this.tableName = Settings.getMySQLTablename;
		this.columnName = Settings.getMySQLColumnName;
		this.columnPassword = Settings.getMySQLColumnPassword;
		this.columnStatus = Settings.getMySQLColumnLoginStatus;

		connect();
	}

	private synchronized void connect() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		EasyLogin.instance.getLogger().info("MySQL driver loaded");
		MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
		dataSource.setDatabaseName(database);
		dataSource.setServerName(host);
		dataSource.setPort(Integer.parseInt(port));
		dataSource.setUser(username);
		dataSource.setPassword(password);
		conPool = new MiniConnectionPoolManager(dataSource, 20);
		EasyLogin.instance.getLogger().info("Connection pool ready");
	}

	public synchronized PlayerAuth getAuth(String user) {
		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			con = makeSureConnectionIsReady();
			pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE lower(" + columnName + ")=?;");
			pst.setString(1, user);
			rs = pst.executeQuery();
			if (rs.next()) {
				
				Playerstatus playerstatus = Playerstatus.Offline;
				try{
					playerstatus = Playerstatus.valueOf(rs.getString(this.columnStatus));
				} catch (IllegalArgumentException e) {
					playerstatus = Playerstatus.Offline;
				} catch (NullPointerException e) {
					playerstatus = Playerstatus.Offline;
				}
				
				return new PlayerAuth(rs.getString(this.columnName), 
									rs.getString(this.columnPassword),
									playerstatus);
			} else {
				return null;
			}
		} catch (SQLException ex) {
			EasyLogin.instance.getLogger().severe(ex.getMessage());
			return null;
		} catch (TimeoutException ex) {
			EasyLogin.instance.getLogger().severe(ex.getMessage());
			return null;
		} finally {
			close(rs);
			close(pst);
			close(con);
		}
	}
	
	public synchronized void updatePlayerStatus(PlayerAuth playerauth) {

		Connection con = null;
		PreparedStatement pst = null;
		try {
			con = makeSureConnectionIsReady();
			
			String statement = String.format("UPDATE %s SET %s=? WHERE %s=?;",
								this.tableName,this.columnStatus,this.columnName);
			
			
			pst = con.prepareStatement(statement);
			pst.setString(1, playerauth.getStatus().toString());
			pst.setString(2, playerauth.getName());
			pst.execute();
		} catch (SQLException ex) {
			EasyLogin.instance.getLogger().severe(ex.getMessage());
			return;
		} catch (TimeoutException ex) {
			EasyLogin.instance.getLogger().severe(ex.getMessage());
			return;
		} finally {
			close(pst);
			close(con);
		}
	}

	public synchronized void close() {
		try {
			conPool.dispose();
		} catch (SQLException ex) {
			EasyLogin.instance.getLogger().severe(ex.getMessage());
		}
	}

	private void close(Statement st) {
		if (st != null) {
			try {
				st.close();
			} catch (SQLException ex) {
				EasyLogin.instance.getLogger().severe(ex.getMessage());
			}
		}
	}

	private void close(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException ex) {
				EasyLogin.instance.getLogger().severe(ex.getMessage());
			}
		}
	}

	private void close(Connection con) {
		if (con != null) {
			try {
				con.close();
			} catch (SQLException ex) {
				EasyLogin.instance.getLogger().severe(ex.getMessage());
			}
		}
	}

	private synchronized Connection makeSureConnectionIsReady() {
		Connection con = null;
		try {
			con = conPool.getValidConnection();
		} catch (Exception te) {
			try {
				con = null;
				reconnect();
			} catch (Exception e) {
				EasyLogin.instance.getLogger().severe(e.getMessage());
				if (Settings.isStopEnabled) {
					EasyLogin.instance.getLogger().severe("Can't reconnect to MySQL database... Please check your MySQL informations ! SHUTDOWN...");
					EasyLogin.instance.getServer().shutdown();
				}
				if (!Settings.isStopEnabled)
					EasyLogin.instance.getServer().getPluginManager().disablePlugin(EasyLogin.instance);
			}
		} catch (AssertionError ae) {
			// Make sure assertionerror is caused by the connectionpoolmanager, else re-throw it
			if (!ae.getMessage().equalsIgnoreCase("AuthMeDatabaseError"))
				throw new AssertionError(ae.getMessage());
			try {
				con = null;
				reconnect();
			} catch (Exception e) {
				EasyLogin.instance.getLogger().severe(e.getMessage());
				if (Settings.isStopEnabled) {
					EasyLogin.instance.getLogger().severe("Can't reconnect to MySQL database... Please check your MySQL informations ! SHUTDOWN...");
					EasyLogin.instance.getServer().shutdown();
				}
				if (!Settings.isStopEnabled)
					EasyLogin.instance.getServer().getPluginManager().disablePlugin(EasyLogin.instance);
			}
		}
		if (con == null)
			con = conPool.getValidConnection();
		return con;
	}

	private synchronized void reconnect() throws ClassNotFoundException, SQLException, TimeoutException {
		conPool.dispose();
		Class.forName("com.mysql.jdbc.Driver");
		MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
		dataSource.setDatabaseName(database);
		dataSource.setServerName(host);
		dataSource.setPort(Integer.parseInt(port));
		dataSource.setUser(username);
		dataSource.setPassword(password);
		conPool = new MiniConnectionPoolManager(dataSource, 10);
		EasyLogin.instance.getLogger().info("ConnectionPool was unavailable... Reconnected!");
	}
	
	public synchronized RegistrationResult registerUser(String username, String password, String email) {
	    Connection con = null;
	    try {
	        con = makeSureConnectionIsReady();

	        // Test if user exists (this should not happen, but safty first)    
	        if (this.isUserRegistered(con, username))
	            return RegistrationResult.USER_ALREADY_REGISTERED;    
	    
	        // UPDATE wcf1_user TABLE
	        this.addUserToUserTable(con, username, password, email);
	        
	        int userID = getUserIDFromUser(con, username);        

	        this.initDatabaseForUser(con, userID);
	        
	    }
	    catch (SQLException e) {
	        e.printStackTrace();
	        return RegistrationResult.FAILED;
	    }
	    catch (Exception e) {
	        e.printStackTrace();
	        return RegistrationResult.UNKNOWN_ERROR;
	    } finally {
	        close(con);
	    }
	    
	    return RegistrationResult.SUCCESS;
	}



	////////////////////////////////////////
	// SQL
	////////////////////////////////////////

	private synchronized void addUserToUserTable(Connection con, String username, String password, String email) throws SQLException {

	    String accessToken = this.generateAccesstoken();
	    int activationCode = this.generateActivationCode();
	    String passwordHash = generatePasswordHash(password);
	    long now = System.currentTimeMillis() / 1000l;

	    PreparedStatement ps = con.prepareStatement(
	        INSERT_INTO + USER_TABLE + "(`username`, `email`, `password`, `accessToken`, `languageID`, "
	        		+ "`registrationDate`, `styleID`, `banned`, `banReason`, `activationCode`, "
	        		+ "`lastLostPasswordRequestTime`, `lostPasswordKey`, `lastUsernameChange`, `newEmail`, `oldUsername`, "
	        		+ "`quitStarted`, `reactivationCode`, `registrationIpAddress`, `avatarID`, `disableAvatar`, "
	        		+ "`disableAvatarReason`, `enableGravatar`, `signature`, `signatureEnableBBCodes`, `signatureEnableHtml`, "
	        		+ "`signatureEnableSmilies`, `disableSignature`, `disableSignatureReason`, `lastActivityTime`, `profileHits`, "
	        		+ "`rankID`, `userTitle`, `userOnlineGroupID`, `activityPoints`, `notificationMailToken`, "
	        		+ "`authData`, `likesReceived`, `wbbPosts`, `disclaimerAccepted`, `todos`, "
	        		+ "`loginStatus`, `playtime`) "
	        		+ "VALUES "
	        		+ "( ?, ?, ?, ?, '1', "
	        		+ "?, '0', '0', NULL, ?, "
	        		+ "'0', '', '0', '', '', "
	        		+ "'0', '0', '::ffff:b039:8de6', NULL, '0', "
	        		+ "'', '0', '', '1', '0', "
	        		+ "'1', '0', '', ?, '7', "
	        		+ "NULL, '', '1', '0', '', "
	        		+ "'', '0', '0', '1', '0', "
	        		+ "'Offline', '0' );");
	        
	    ps.setString(1, username);
	    ps.setString(2, email);
	    ps.setString(3, passwordHash);
	    ps.setString(4, accessToken);
	    ps.setLong(5, now);    
	    ps.setInt(6, activationCode);
	    ps.setLong(7, now);
	    
	    ps.executeUpdate();
	    close(ps);
	}



	private synchronized boolean isUserRegistered(Connection con, String username) throws SQLException {
	    return this.getUserIDFromUser(con, username) != -1;
	}

	private synchronized int getUserIDFromUser(Connection con, String username) throws SQLException {
	    PreparedStatement pst = null;
	    ResultSet rs = null;
	    int result = -1;
	    pst = con.prepareStatement(SELECT + USER_TABLE_USERID + FROM + USER_TABLE + WHERE + "lower(" + USER_TABLE_USERNAME + ")=?;");
	    pst.setString(1, username.toLowerCase());
	    rs = pst.executeQuery();   
	    if (rs.next())             
	        result = rs.getInt(USER_TABLE_USERID);
	    close(pst);
	    close(rs);
	    return result;
	}

	public synchronized void initDatabaseForUser(Connection con, int userID) throws SQLException {
	    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
	    
	    // UPDATE wcf1_user_to_group
	    stmt.addBatch(INSERT_INTO + USER_TO_GROUP_TABLE + " (userID, groupID) VALUES ("+userID+", 1);");
	    stmt.addBatch(INSERT_INTO + USER_TO_GROUP_TABLE + " (userID, groupID) VALUES ("+userID+", 2);");       
	    
	    // UPDATE wcf1_user_to_language
	    stmt.addBatch(INSERT_INTO + USER_TO_LANGUAGE_TABLE + " (userID, languageID) VALUES (" + userID + ",1);" );
	        
	    // UPDATE wcf1_user_option_value
	    stmt.addBatch(INSERT_INTO + USER_OPTION_VALUE_TABLE+" (userID, userOption2, userOption3, userOption16, userOption19, "
	            + "userOption20, userOption21, userOption22, userOption28, userOption34, userOption35) VALUES ("
	            + userID +", '0000-00-00', '1', '1', '3', '1', '1', '1', '1', '0', '1');");
	        
	    // UPDATE wcf1_user_notification_event_to_user
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",2,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",3,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",4,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",7,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",8,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",9,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",10,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",11,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",12,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",13,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",14,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",17,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",18,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",19,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",20,'none');");
	    stmt.addBatch(INSERT_INTO + USER_NOTIFICATION_EVENT_TO_USER_TABLE + " (userID, eventID, mailNotificationType) VALUES ("+userID+",24,'none');");
	    
	    // UPDATE wcf1_user_storage
	    stmt.addBatch(INSERT_INTO + USER_STORAGE_TABLE+" (userID, field, fieldValue) VALUES ("+userID+", 'collapsedContent-2', 'a:0:{}');");
	    stmt.addBatch(INSERT_INTO + USER_STORAGE_TABLE+" (userID, field, fieldValue) VALUES ("+userID+", 'collapsedContent-77', 'a:0:{}');");
	    stmt.addBatch(INSERT_INTO + USER_STORAGE_TABLE+" (userID, field, fieldValue) VALUES ("+userID+", 'followingUserIDs', 'a:0:{}');");
	    stmt.addBatch(INSERT_INTO + USER_STORAGE_TABLE+" (userID, field, fieldValue) VALUES ("+userID+", 'languageIDs', 'a:1:{i:0;s:1:\"1\";}');");
	    stmt.addBatch(INSERT_INTO + USER_STORAGE_TABLE+" (userID, field, fieldValue) VALUES ("+userID+", 'tourCache', 'a:3:{s:14:\"availableTours\";a:0:{}s:10:\"takenTours\";a:0:{}s:12:\"lastTourTime\";i:0;}');");
	    stmt.addBatch(INSERT_INTO + USER_STORAGE_TABLE+" (userID, field, fieldValue) VALUES ("+userID+", 'trackedUserVisits', 'a:0:{}');");
	    stmt.addBatch(INSERT_INTO + USER_STORAGE_TABLE+" (userID, field, fieldValue) VALUES ("+userID+", 'unreadConversationCount', 's:1:\"0\";');");
	    stmt.addBatch(INSERT_INTO + USER_STORAGE_TABLE+" (userID, field, fieldValue) VALUES ("+userID+", 'userNotificationCount', 's:1:\"0\";');");
	    stmt.addBatch(INSERT_INTO + USER_STORAGE_TABLE+" (userID, field, fieldValue) VALUES ("+userID+", 'wbbBoardPermissions', 'a:2:{s:3:\"mod\";a:0:{}s:4:\"user\";a:0:{}}');");
	    
	    // commit
	    stmt.executeBatch();
	    con.commit();
	    close(stmt);
	}

	    
	////////////////////////////////////////
	// GENERATORS
	////////////////////////////////////////

	private int generateActivationCode() {
	    SecureRandom random = new SecureRandom();
	    int c = random.nextInt(900000000) + 100000000;
	    return c;
	}

	private String generateAccesstoken() {
	    //40 chars  320bit hexadezimal
	    SecureRandom rng = new SecureRandom();
	    int length = 40;
	    String characters = "0123456789abcdef";
	    char[] text = new char[length];
	    for (int i = 0; i < length; i++) {
	        text[i] = characters.charAt(rng.nextInt(characters.length()));
	    }
	    return new String(text);
	}

	private String generatePasswordHash(String password) {
	    String salt = BCrypt.gensalt(8);
	    String passinfo = getSaltedHash(getSaltedHash(password, salt), salt);
	    return passinfo;
	}
	
	private static String getSaltedHash(String password, String salt) {		
		return BCrypt.hashpw(password, salt);
	}



}
