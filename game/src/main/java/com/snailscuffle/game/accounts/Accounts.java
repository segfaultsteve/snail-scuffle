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

public class Accounts implements Closeable {
	
	private static final Logger logger = LoggerFactory.getLogger(Accounts.class);
	private final Connection sqlite;
	
	public Accounts(String dbPath) throws AccountException {
		String createAccountsTableSql = "CREATE TABLE IF NOT EXISTS accounts ("
				+ "ardor_account_id INTEGER PRIMARY KEY NOT NULL CHECK (ardor_account_id > 0), "
				+ "user_name TEXT NOT NULL CHECK (length(user_name) > 0), "
				+ "public_key TEXT NOT NULL CHECK (length(public_key) > 0), "
				+ "wins INTEGER NOT NULL CHECK (wins >= 0) DEFAULT 0, "
				+ "losses INTEGER NOT NULL CHECK (losses >= 0) DEFAULT 0, "
				+ "streak INTEGER NOT NULL DEFAULT 0, "		// positive for winning streak, negative for losing streak
				+ "rating REAL NOT NULL"
				+ ");";
		
		try {
			String connectionUrl = "jdbc:sqlite:" + dbPath;
			sqlite = DriverManager.getConnection(connectionUrl);
			
			try (Statement statement = sqlite.createStatement()) {
				statement.executeUpdate(createAccountsTableSql);
			}
		} catch (SQLException e) {
			String error = "Database error while initializing accounts";
			logger.error(error, e);
			throw new AccountException(error, e);
		}
	}
	
	public void insertOrUpdate(Account account) throws AccountException {
		String upsertAccountSql = "INSERT INTO accounts VALUES (?, ?, ?, ?, ?, ?, ?) "
				+ "ON CONFLICT (ardor_account_id) DO UPDATE SET "
				+ "user_name=excluded.user_name,"
				+ "wins=excluded.wins,"
				+ "losses=excluded.losses,"
				+ "streak=excluded.streak,"
				+ "rating=excluded.rating;";
		
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
	
	public Account get(AccountQuery query) throws AccountException {
		boolean useId = query.id != null;
		String getAccountSql = "SELECT * FROM accounts WHERE "
				+ (useId ? "ardor_account_id" : "user_name")
				+ " LIKE ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setString(1, useId ? query.id : query.player);
			ResultSet result = getAccount.executeQuery();
			List<Account> accountsInResult = extractAccounts(result);
			if (accountsInResult.size() == 1) {
				Account match = accountsInResult.get(0);
				match.rank = determineRank(match);
				return match;
			} else {
				throw new AccountException("Could not find account " + (useId ? query.id : ("for username '" + query.player + "'")));
			}
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve account " + (useId ? query.id : ("for username '" + query.player + "'"));
			logger.error(error, e);
			throw new AccountException(error, e);
		}
	}
	
	private static List<Account> extractAccounts(ResultSet queryResult) throws SQLException {
		List<Account> accounts = new ArrayList<Account>();
		while (queryResult.next()) {
			accounts.add(new Account(
					queryResult.getLong("ardor_account_id"),
					queryResult.getString("user_name"),
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
