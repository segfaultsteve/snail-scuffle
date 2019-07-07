package com.snailscuffle.game.accounts;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Accounts implements Closeable {
	
	private static final Logger logger = LoggerFactory.getLogger(Accounts.class);
	private final Connection sqlite;
	
	public Accounts(String dbPath) throws SQLException {
		String connectionUrl = "jdbc:sqlite:" + dbPath;
		sqlite = DriverManager.getConnection(connectionUrl);
		
		String createAccountsTableSql = "CREATE TABLE IF NOT EXISTS accounts ("
				+ "ardor_account_id INTEGER PRIMARY KEY NOT NULL CHECK (ardor_account_id > 0), "
				+ "user_name TEXT NOT NULL CHECK (length(user_name) > 0), "
				+ "public_key TEXT NOT NULL CHECK (length(public_key) > 0), "
				+ "wins INTEGER NOT NULL CHECK (wins >= 0) DEFAULT 0, "
				+ "losses INTEGER NOT NULL CHECK (losses >= 0) DEFAULT 0, "
				+ "streak INTEGER NOT NULL DEFAULT 0, "		// positive for winning streak, negative for losing streak
				+ "rating REAL NOT NULL"
				+ ");";
		
		try (Statement statement = sqlite.createStatement()) {
			statement.executeUpdate(createAccountsTableSql);
		}
	}
	
	public void insertOrUpdate(Account account) throws AccountsException {
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
			throw new AccountsException(error, e);
		}
	}
	
	public Account get(AccountsQuery query) throws AccountNotFoundException {
		boolean useId = query.id != null;
		String getAccountSql = "SELECT * FROM accounts WHERE "
				+ (useId ? "ardor_account_id" : "user_name")
				+ " LIKE ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setString(1, useId ? query.id : query.player);
			ResultSet result = getAccount.executeQuery();
			Account match = extractAccounts(result).get(0);
			
			Account[] allAccounts = (Account[]) getAllAccounts().toArray(new Account[0]);
			int index = Arrays.binarySearch(allAccounts, match, compareByRank());
			match.rank = index + 1;
			return match;
		} catch (SQLException | AccountsException e) {
			String error = "Database error while attempting to retrieve account " + (useId ? query.id : ("for " + query.player));
			logger.error(error, e);
			throw new AccountNotFoundException(error, e);
		}
	}
	
	private List<Account> getAllAccounts() throws AccountsException {
		String getAllAccountsSql = "SELECT * FROM accounts ORDER BY rating DESC;";
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(getAllAccountsSql);
			return extractAccounts(result);
		} catch (SQLException e) {
			String error = "Database error while retrieving all accounts";
			logger.error(error, e);
			throw new AccountsException(error, e);
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
	
	private static Comparator<Account> compareByRank() {
		return new Comparator<Account>() {
			public int compare(Account a1, Account a2) {
				// If a1 has a higher rating, it has a lower rank (i.e., closer to 1st place).
				return a2.rating - a1.rating;
			}
		};
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
