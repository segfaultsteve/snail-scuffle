package com.snailscuffle.game.accounts;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.game.Constants;

public class Accounts implements Closeable {
	
	private static final Logger logger = LoggerFactory.getLogger(Accounts.class);
	private final Connection sqlite;
	
	public Accounts(String dbPath) throws AccountException {
		try {
			String connectionUrl = "jdbc:sqlite:" + dbPath;
			sqlite = DriverManager.getConnection(connectionUrl);
			
			if (!dbPreviouslyInitialized()) {
				initDb();
			}
		} catch (SQLException e) {
			String error = "Database error while initializing accounts";
			logger.error(error, e);
			throw new AccountException(error, e);
		}
	}
	
	private boolean dbPreviouslyInitialized() throws SQLException {
		String accountsTableExistsSql = "SELECT name FROM sqlite_master WHERE type='table' AND name='accounts'";
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(accountsTableExistsSql);
			return result.next();
		}
	}
	
	private void initDb() throws SQLException {
		String createAccountsTableSql = "CREATE TABLE accounts ("
				+ "ardor_account_id INTEGER PRIMARY KEY NOT NULL CHECK (ardor_account_id > 0), "
				+ "username TEXT NOT NULL CHECK (length(username) > 0), "
				+ "public_key TEXT NOT NULL CHECK (length(public_key) > 0), "
				+ "wins INTEGER NOT NULL CHECK (wins >= 0) DEFAULT 0, "
				+ "losses INTEGER NOT NULL CHECK (losses >= 0) DEFAULT 0, "
				+ "streak INTEGER NOT NULL DEFAULT 0, "		// positive for winning streak, negative for losing streak
				+ "rating REAL NOT NULL"
				+ ");";
		
		String createSnapshotsTableSql = "CREATE TABLE snapshots ("
				+ "table_name TEXT PRIMARY KEY NOT NULL, "
				+ "sync_height INTEGER NOT NULL, "
				+ "sync_block_id TEXT NOT NULL"
				+ ");";
		
		String updateSnapshotsTableSql = "INSERT INTO snapshots VALUES ("
				+ "'accounts', "
				+ Constants.INITIAL_SYNC_HEIGHT + ", "
				+ "'" + Long.toUnsignedString(Constants.INITIAL_SYNC_BLOCK_ID) + "'"
				+ ")";
		
		sqlite.setAutoCommit(false);
		try (Statement statement = sqlite.createStatement()) {
			statement.executeUpdate(createAccountsTableSql);
			statement.executeUpdate(createSnapshotsTableSql);
			statement.executeUpdate(updateSnapshotsTableSql);
			sqlite.commit();
		} catch (SQLException e) {
			sqlite.rollback();
		} finally {
			sqlite.setAutoCommit(true);
		}
	}
	
	public void insertOrUpdate(Account account) throws AccountException {
		String upsertAccountSql = "INSERT INTO accounts VALUES (?, ?, ?, ?, ?, ?, ?) "
				+ "ON CONFLICT (ardor_account_id) DO UPDATE SET "
				+ 	"username=excluded.username,"
				+ 	"wins=excluded.wins,"
				+ 	"losses=excluded.losses,"
				+ 	"streak=excluded.streak,"
				+ 	"rating=excluded.rating;";
		
		try (PreparedStatement upsertAccount = sqlite.prepareStatement(upsertAccountSql)) {
			upsertAccount.setLong(1, account.numericId());
			upsertAccount.setString(2, account.username);
			upsertAccount.setString(3, account.publicKey);
			upsertAccount.setInt(4, account.wins);
			upsertAccount.setInt(5, account.losses);
			upsertAccount.setInt(6, account.streak);
			upsertAccount.setDouble(7, account.rating);
			upsertAccount.executeUpdate();
		} catch (SQLException e) {
			String error = "Database error while attempting to insert or update account " + account.id;
			logger.error(error, e);
			throw new AccountException(error, e);
		}
	}
	
	public Account getById(String id) throws AccountException {
		String getAccountSql = "SELECT * FROM accounts WHERE ardor_account_id LIKE ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setString(1, id);
			return executeQueryForAccount(getAccount);
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve account " + id;
			logger.error(error, e);
			throw new AccountException(error, e);
		}
	}
	
	public Account getByUsername(String username) throws AccountException {
		String getAccountSql = "SELECT * FROM accounts WHERE username LIKE ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setString(1, username);
			return executeQueryForAccount(getAccount);
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve account for player '" + username + "'";
			logger.error(error, e);
			throw new AccountException(error, e);
		}
	}
	
	private Account executeQueryForAccount(PreparedStatement getAccount) throws AccountException, SQLException {
		ResultSet result = getAccount.executeQuery();
		List<Account> accountsInResult = extractAccounts(result);
		if (accountsInResult.size() == 1) {
			Account match = accountsInResult.get(0);
			match.rank = determineRank(match);
			return match;
		} else {
			throw new AccountException("Account not found");
		}
	}
	
	private static List<Account> extractAccounts(ResultSet queryResult) throws SQLException {
		List<Account> accounts = new ArrayList<Account>();
		while (queryResult.next()) {
			accounts.add(new Account(
					queryResult.getLong("ardor_account_id"),
					queryResult.getString("username"),
					queryResult.getString("public_key"),
					queryResult.getInt("wins"),
					queryResult.getInt("losses"),
					queryResult.getInt("streak"),
					queryResult.getInt("rating")));
		}
		return accounts;
	}
	
	private int determineRank(Account account) throws AccountException {
		String higherRankedAccountsSql = "SELECT COUNT(*) FROM accounts WHERE rating > ?;";
		try (PreparedStatement higherRankedAccounts = sqlite.prepareStatement(higherRankedAccountsSql)) {
			higherRankedAccounts.setInt(1, account.rating);
			ResultSet result = higherRankedAccounts.executeQuery();
			return result.next() ? (result.getInt(1) + 1) : 1;
		} catch (SQLException e) {
			String error = "Database error while determining rank of " + account.id;
			logger.error(error, e);
			throw new AccountException(error, e);
		}
	}
	
	@Override
	public void close() {
		if (sqlite == null) {
			return;
		}
		
		try {
			if (!sqlite.getAutoCommit()) {
				sqlite.rollback();
			}
			sqlite.close();
		} catch (SQLException e) {
			logger.error("Failed to close connection to SQLite", e);
		}
	}

}
