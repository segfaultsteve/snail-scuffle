package com.snailscuffle.game.accounts;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.game.Constants;

public class Accounts implements Closeable {
	
	private static final Logger logger = LoggerFactory.getLogger(Accounts.class);
	private final Connection sqlite;
	private final int maxSnapshotCount;
	
	public Accounts(String dbPath, int maxSnapshotCount) throws AccountsException {
		this.maxSnapshotCount = maxSnapshotCount;
		try {
			String connectionUrl = "jdbc:sqlite:" + dbPath;
			sqlite = DriverManager.getConnection(connectionUrl);
			
			if (!dbPreviouslyInitialized()) {
				initDb();
			}
		} catch (SQLException e) {
			String error = "Database error while initializing accounts";
			logger.error(error, e);
			throw new AccountsException(error, e);
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
	
	public void insertOrUpdate(Account account) throws AccountsException {
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
			throw new AccountsException(error, e);
		}
	}
	
	public Account getById(String id) throws AccountsException {
		String getAccountSql = "SELECT * FROM accounts WHERE ardor_account_id LIKE ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setString(1, id);
			return executeQueryForAccount(getAccount);
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve account " + id;
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public Account getByUsername(String username) throws AccountsException {
		String getAccountSql = "SELECT * FROM accounts WHERE username LIKE ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setString(1, username);
			return executeQueryForAccount(getAccount);
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve account for player '" + username + "'";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private Account executeQueryForAccount(PreparedStatement getAccount) throws AccountsException, SQLException {
		ResultSet result = getAccount.executeQuery();
		List<Account> accountsInResult = extractAccounts(result);
		if (accountsInResult.size() == 1) {
			Account match = accountsInResult.get(0);
			match.rank = determineRank(match);
			return match;
		} else {
			throw new AccountsException("Account not found");
		}
	}
	
	private static List<Account> extractAccounts(ResultSet queryResult) throws SQLException {
		List<Account> accounts = new ArrayList<>();
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
	
	private int determineRank(Account account) throws AccountsException {
		String higherRankedAccountsSql = "SELECT COUNT(*) FROM accounts WHERE rating > ?;";
		try (PreparedStatement higherRankedAccounts = sqlite.prepareStatement(higherRankedAccountsSql)) {
			higherRankedAccounts.setInt(1, account.rating);
			ResultSet result = higherRankedAccounts.executeQuery();
			return result.next() ? (result.getInt(1) + 1) : 1;
		} catch (SQLException e) {
			String error = "Database error while determining rank of " + account.id;
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public void takeSnapshot(String nameSuffix) throws AccountsException {
		String createSnapshotSql = "CREATE TABLE accounts_" + nameSuffix + " AS SELECT * FROM accounts;";
		String updateSnapshotsTableSql = "INSERT INTO snapshots (table_name, sync_height, sync_block_id) "
				+ "SELECT 'accounts_" + nameSuffix + "', sync_height, sync_block_id "
				+ "FROM snapshots WHERE table_name='accounts';";
		String countSnapshotsSql = "SELECT COUNT(*) FROM snapshots;";
		
		try {
			sqlite.setAutoCommit(false);
			try (Statement statement = sqlite.createStatement()) {
				statement.executeUpdate(createSnapshotSql);
				statement.executeUpdate(updateSnapshotsTableSql);
				ResultSet result = statement.executeQuery(countSnapshotsSql);
				int rowCount = result.next() ? result.getInt(1) : -1;
				if (rowCount > maxSnapshotCount + 1) {		// +1 for the (current) accounts table
					deleteOldSnapshots(rowCount - (maxSnapshotCount + 1), statement);
				} else if (rowCount < 0) {
					throw new AccountsException("Database error while checking for old snapshots");
				}
				sqlite.commit();
			} catch (SQLException | AccountsException e) {
				sqlite.rollback();
				throw e;
			} finally {
				sqlite.setAutoCommit(true);
			}
		} catch (SQLException e) {
			String error = "Database error while taking snapshot of accounts";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private static void deleteOldSnapshots(int count, Statement statement) throws SQLException, AccountsException {
		String snapshotToDeleteSql = "SELECT table_name FROM snapshots ORDER BY rowid LIMIT " + count + " OFFSET 1;";
		ResultSet result = statement.executeQuery(snapshotToDeleteSql);
		int deleted = 0;
		while (result.next()) {
			String snapshotToDelete = result.getString("table_name");
			statement.executeUpdate("DROP TABLE " + snapshotToDelete + ";");
			statement.executeUpdate("DELETE FROM snapshots WHERE table_name='" + snapshotToDelete + "';");
			++deleted;
		}
		if (deleted != count) {
			throw new AccountsException("Database error while deleting old snapshots");
		}
	}
	
	public Map<String, AccountsSnapshot> getAllSnapshots() throws AccountsException {
		Map<String, AccountsSnapshot> snapshots = new HashMap<>();
		String getSnapshotsSql = "SELECT * FROM snapshots";
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(getSnapshotsSql);
			while (result.next()) {
				String name = result.getString("table_name");
				long syncHeight = result.getLong("sync_height");
				String syncBlockId = result.getString("sync_block_id");
				AccountsSnapshot snapshot = new AccountsSnapshot(name, syncHeight, Long.parseUnsignedLong(syncBlockId));
				snapshots.put(snapshot.name, snapshot);
			}
		} catch (SQLException e) {
			String error = "Database error while getting accounts snapshots";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
		return snapshots;
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
