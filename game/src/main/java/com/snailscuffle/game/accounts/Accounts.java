package com.snailscuffle.game.accounts;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
		String accountsTableExistsSql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'accounts'";
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(accountsTableExistsSql);
			return result.next();
		}
	}
	
	private void initDb() throws SQLException {
		String createAccountsTableSql =
				  "CREATE TABLE accounts ("
				+ 	"ardor_account_id INTEGER PRIMARY KEY NOT NULL CHECK (ardor_account_id > 0), "
				+ 	"username TEXT NOT NULL COLLATE NOCASE CHECK (length(username) > 0), "
				+ 	"public_key TEXT NOT NULL COLLATE NOCASE CHECK (length(public_key) > 0), "
				+ 	"wins INTEGER NOT NULL CHECK (wins >= 0) DEFAULT 0, "
				+ 	"losses INTEGER NOT NULL CHECK (losses >= 0) DEFAULT 0, "
				+ 	"streak INTEGER NOT NULL DEFAULT 0, "		// positive for winning streak, negative for losing streak
				+ 	"rating INTEGER NOT NULL CHECK (rating > 0)"
				+ ")";
		
		String createSnapshotsTableSql =
				  "CREATE TABLE snapshots ("
				+ 	"table_name TEXT PRIMARY KEY NOT NULL COLLATE NOCASE, "
				+ 	"sync_height INTEGER NOT NULL, "
				+ 	"sync_block_id TEXT NOT NULL COLLATE NOCASE"
				+ ")";
		
		String updateSnapshotsTableSql =
				  "INSERT INTO snapshots VALUES ("
				+ 	"'accounts', "
				+ 	Constants.INITIAL_SYNC_HEIGHT + ", "
				+ 	"'" + Long.toUnsignedString(Constants.INITIAL_SYNC_BLOCK_ID) + "'"
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
	
	public void insertOrUpdate(Iterable<Account> accounts, int newHeight, long blockIdAtNewHeight) throws AccountsException {
		try {
			sqlite.setAutoCommit(false);
			try {
				for (Account account : accounts) {
					insertOrUpdate(account);
				}
				updateSyncHeight(newHeight, blockIdAtNewHeight);
				sqlite.commit();
			} catch (AccountsException | SQLException e) {
				sqlite.rollback();
			} finally {
				sqlite.setAutoCommit(true);
			}
		} catch (SQLException e) {
			String error = "Database error while attempting to insert or update multiple accounts";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private void insertOrUpdate(Account account) throws AccountsException {
		String upsertAccountSql =
				  "INSERT INTO accounts VALUES (?, ?, ?, ?, ?, ?, ?) "
				+ "ON CONFLICT (ardor_account_id) DO UPDATE SET "
				+ 	"username = excluded.username,"
				+ 	"wins = excluded.wins,"
				+ 	"losses = excluded.losses,"
				+ 	"streak = excluded.streak,"
				+ 	"rating = excluded.rating";
		
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
	
	public void updateSyncHeight(int height, long blockId) throws AccountsException {
		String updateHeightSql =
				  "UPDATE snapshots "
				+ "SET sync_height = " + height + ", sync_block_id = '" + Long.toUnsignedString(blockId) + "' "
				+ "WHERE table_name = 'accounts'";
		
		try (Statement statement = sqlite.createStatement()) {
			statement.executeUpdate(updateHeightSql);
		} catch (SQLException e) {
			String error = "Database error while attempting to update sync height";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public int getSyncHeight() throws AccountsException {
		String getHeightSql = "SELECT sync_height FROM snapshots WHERE table_name = 'accounts'";
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(getHeightSql);
			return result.getInt("sync_height");
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve sync height";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public Account getById(long id) throws AccountsException {
		String getAccountSql = "SELECT * FROM accounts WHERE ardor_account_id = ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setLong(1, id);
			return executeQueryForAccounts(getAccount).get(0);
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve account " + id;
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public List<Account> getById(List<Long> ids) throws AccountsException {
		String getAccountsSql = "SELECT * FROM accounts WHERE ardor_account_id IN (?)";
		try (PreparedStatement getAccounts = sqlite.prepareStatement(getAccountsSql)) {
			String idsString = ids.stream()
					.map(id -> String.valueOf(id))
					.reduce((result, next) -> result + ", " + next)
					.get();
			getAccounts.setString(1, idsString);
			return executeQueryForAccounts(getAccounts);
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve multiple accounts";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public Account getByUsername(String username) throws AccountsException {
		String getAccountSql = "SELECT * FROM accounts WHERE username = ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setString(1, username);
			return executeQueryForAccounts(getAccount).get(0);
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve account for player '" + username + "'";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private List<Account> executeQueryForAccounts(PreparedStatement getAccount) throws AccountsException, SQLException {
		ResultSet result = getAccount.executeQuery();
		List<Account> matchedAccounts = extractAccounts(result);
		
		if (matchedAccounts.isEmpty()) {
			throw new AccountsException("Account(s) not found");
		}
		
		List<Integer> allRatings = accountRatingsInDescendingOrder();
		for (Account account : matchedAccounts) {
			account.rank = determineRank(account.rating, allRatings);
		}
		
		return matchedAccounts;
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
	
	private List<Integer> accountRatingsInDescendingOrder() throws SQLException {
		String ratingsSql = "SELECT rating FROM accounts ORDER BY rating DESC";
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(ratingsSql);
			
			List<Integer> ratings = new ArrayList<>();
			while (result.next()) {
				ratings.add(result.getInt("rating"));
			}
			return ratings;
		}
	}
	
	private static int determineRank(int rating, List<Integer> allRatings) {
		Comparator<Integer> descendingOrder = new Comparator<Integer>() {
			@Override
			public int compare(Integer i1, Integer i2) {
				return i2 - i1;
			}
		};
		int matchIndex = Collections.binarySearch(allRatings, rating, descendingOrder);
		
		if (matchIndex < 0) {
			// Collections.binarySearch returns (-(insertion point) - 1) when it fails
			// to find the item. This normally shouldn't happen, but it technically is
			// possible for the blockchain sync thread to update the accounts (and their
			// ratings) after the accounts query and before the ratings query in
			// executeQueryForAccounts. No big deal, just go with it.
			matchIndex = -matchIndex - 1;
		}
		
		while (matchIndex > 0 && allRatings.get(matchIndex - 1) == rating) {
			--matchIndex;
		}
		
		return matchIndex + 1;
	}
	
	public void takeSnapshot(String nameSuffix) throws AccountsException {
		String createSnapshotSql = "CREATE TABLE accounts_" + nameSuffix + " AS SELECT * FROM accounts";
		String updateSnapshotsTableSql =
				  "INSERT INTO snapshots "
				+ "SELECT 'accounts_" + nameSuffix + "', sync_height, sync_block_id "
				+ "FROM snapshots WHERE table_name = 'accounts'";
		String countSnapshotsSql = "SELECT COUNT(*) FROM snapshots";
		
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
		String snapshotsToDeleteSql = "SELECT table_name FROM snapshots ORDER BY rowid LIMIT " + count + " OFFSET 1";
		ResultSet result = statement.executeQuery(snapshotsToDeleteSql);
		int deleted = 0;
		while (result.next()) {
			deleteSnapshot(result.getString("table_name"), statement);
			++deleted;
		}
		if (deleted != count) {
			throw new AccountsException("Database error while deleting old snapshots");
		}
	}
	
	public List<AccountsSnapshot> getAllSnapshots() throws AccountsException {
		String getSnapshotsSql = "SELECT * FROM snapshots ORDER BY sync_height DESC";
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(getSnapshotsSql);
			return extractSnapshots(result);
		} catch (SQLException e) {
			String error = "Database error while getting accounts snapshots";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public int rollBackTo(int height) throws AccountsException {
		String snapshotsToDeleteSql = "SELECT * FROM snapshots WHERE sync_height > " + height;
		
		try {
			sqlite.setAutoCommit(false);
			try (Statement statement = sqlite.createStatement()) {
				ResultSet result = statement.executeQuery(snapshotsToDeleteSql);
				deleteSnapshots(extractSnapshots(result), statement);
				
				AccountsSnapshot mostRecent = getMostRecentSnapshot();
				if (!mostRecent.name.equals("accounts")) {
					restoreSnapshot(mostRecent, statement);
				}
				
				sqlite.commit();
				
				return mostRecent.height;
			} catch (SQLException e) {
				sqlite.rollback();
				throw e;
			} finally {
				sqlite.setAutoCommit(true);
			}
		} catch (SQLException e) {
			String error = "Database error while rolling back snapshots";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private static void deleteSnapshots(List<AccountsSnapshot> snapshots, Statement statement) throws SQLException {
		String clearAccountsTableSql = "DELETE FROM accounts";
		String resetAccountsSyncInfoSql =
				  "UPDATE snapshots SET "
				+ 	"sync_height = " + Constants.INITIAL_SYNC_HEIGHT + ", "
				+ 	"sync_block_id = '" + Long.toUnsignedString(Constants.INITIAL_SYNC_BLOCK_ID) + "' "
				+ "WHERE table_name = 'accounts'";
		
		for (AccountsSnapshot snapshot : snapshots) {
			if (snapshot.name.equals("accounts")) {
				statement.executeUpdate(clearAccountsTableSql);
				statement.executeUpdate(resetAccountsSyncInfoSql);
			} else {
				deleteSnapshot(snapshot.name, statement);
			}
		}
	}
	
	private static void deleteSnapshot(String name, Statement statement) throws SQLException {
		statement.executeUpdate("DROP TABLE " + name + ";");
		statement.executeUpdate("DELETE FROM snapshots WHERE table_name = '" + name + "'");
	}
	
	private AccountsSnapshot getMostRecentSnapshot() throws SQLException {
		String mostRecentRemainingSnapshotSql = "SELECT * FROM snapshots ORDER BY sync_height DESC LIMIT 1";
		
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(mostRecentRemainingSnapshotSql);
			return extractSnapshots(result).get(0);
		}
	}
	
	private static List<AccountsSnapshot> extractSnapshots(ResultSet queryResult) throws SQLException {
		List<AccountsSnapshot> snapshots = new ArrayList<>();
		while (queryResult.next()) {
			String name = queryResult.getString("table_name");
			int syncHeight = queryResult.getInt("sync_height");
			String syncBlockId = queryResult.getString("sync_block_id");
			snapshots.add(new AccountsSnapshot(name, syncHeight, Long.parseUnsignedLong(syncBlockId)));
		}
		return snapshots;
	}
	
	private static void restoreSnapshot(AccountsSnapshot snapshot, Statement statement) throws SQLException {
		String copyIntoAccountsSql = "INSERT INTO accounts SELECT * FROM " + snapshot.name + "";
		String updateSnapshotsSql =
				  "UPDATE snapshots SET "
				+ 	"sync_height = " + snapshot.height + ", "
				+ 	"sync_block_id = '" + Long.toUnsignedString(snapshot.blockId) + "' "
				+ "WHERE table_name = 'accounts'";
		
		statement.executeUpdate(copyIntoAccountsSql);
		statement.executeUpdate(updateSnapshotsSql);
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
