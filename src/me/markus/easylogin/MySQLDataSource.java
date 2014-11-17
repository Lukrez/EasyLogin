package me.markus.easylogin;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

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
	private MiniConnectionPoolManager conPool;

	public MySQLDataSource() throws ClassNotFoundException, SQLException {
		this.host = Settings.getMySQLHost;
		this.port = Settings.getMySQLPort;
		this.username = Settings.getMySQLUsername;
		this.password = Settings.getMySQLPassword;
		this.database = Settings.getMySQLDatabase;
		this.tableName = Settings.getMySQLTablename;
		this.columnName = Settings.getMySQLColumnName;
		this.columnPassword = Settings.getMySQLColumnPassword;

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
				return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword));
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

	/*public synchronized void registerUser(String username, String passwordHash, String salt) throws SQLException {
		Connection con = null;
		Statement st = null;
		try {
			con = makeSureConnectionIsReady();
			st = con.createStatement();
			String state = "INSERT INTO " + tableName + " (" + columnName + "," + columnPassword + "," + columnSalt + ")" + " VALUES (" + "'" + username + "','" + passwordHash + "','" + salt + "');";
			System.out.println(state);
			st.executeUpdate(state);
		} finally {
			close(st);
			close(con);
		}

	}*/

}
