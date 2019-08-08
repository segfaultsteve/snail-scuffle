package com.snailscuffle.game.accounts;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.game.Constants;
import com.snailscuffle.game.blockchain.BlockSyncInfo;
import com.snailscuffle.game.blockchain.StateChangeFromBattle;
import com.snailscuffle.game.blockchain.StateChangeFromBattle.PlayerChange;

public class Accounts implements Closeable {
	
	private static final Logger logger = LoggerFactory.getLogger(Accounts.class);
	private final Connection sqlite;
	private final int recentBattlesDepth;
	
	public Accounts(String dbPath, int recentBattlesDepth) throws AccountsException {
		this.recentBattlesDepth = recentBattlesDepth;
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
		Function<String, String> tableExistsSql = (table) -> "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '" + table + "'";
		String accountsTableExistsSql = tableExistsSql.apply("accounts");
		String recentBattlesTableExistsSql = tableExistsSql.apply("recent_battles");
		String syncStateTableExistsSql = tableExistsSql.apply("sync_state");
		
		try (Statement statement = sqlite.createStatement()) {
			ResultSet accountsResult = statement.executeQuery(accountsTableExistsSql);
			ResultSet recentBattlesResult = statement.executeQuery(recentBattlesTableExistsSql);
			ResultSet syncStateResult = statement.executeQuery(syncStateTableExistsSql);
			return accountsResult.next() && recentBattlesResult.next() && syncStateResult.next();
		}
	}
	
	private void initDb() throws SQLException {
		String createAccountsTableSql =
				  "CREATE TABLE IF NOT EXISTS accounts ("
				+	"ardor_account_id INTEGER PRIMARY KEY NOT NULL CHECK (ardor_account_id > 0), "
				+	"username TEXT NOT NULL COLLATE NOCASE CHECK (length(username) > 0), "
				+	"public_key TEXT NOT NULL COLLATE NOCASE CHECK (length(public_key) > 0), "
				+	"wins INTEGER NOT NULL CHECK (wins >= 0) DEFAULT 0, "
				+	"losses INTEGER NOT NULL CHECK (losses >= 0) DEFAULT 0, "
				+	"streak INTEGER NOT NULL DEFAULT 0, "		// positive for winning streak, negative for losing streak
				+	"rating INTEGER NOT NULL CHECK (rating > 0)"
				+ ")";
		
		String createRecentBattlesTableSql =
				  "CREATE TABLE IF NOT EXISTS recent_battles ("
				+	"finish_height INTEGER NOT NULL CHECK (finish_height > 0), "
				+	"finish_block_id INTEGER NOT NULL, "
				+	"winner_id INTEGER NOT NULL, "
				+	"winner_previous_rating INTEGER NOT NULL CHECK (winner_previous_rating > 0), "
				+	"winner_updated_rating INTEGER NOT NULL CHECK (winner_updated_rating > 0), "
				+	"winner_previous_streak INTEGER NOT NULL, "
				+	"winner_updated_streak INTEGER NOT NULL, "
				+	"loser_id INTEGER NOT NULL, "
				+	"loser_previous_rating INTEGER NOT NULL CHECK (loser_previous_rating > 0), "
				+	"loser_updated_rating INTEGER NOT NULL CHECK (loser_updated_rating > 0), "
				+	"loser_previous_streak INTEGER NOT NULL, "
				+	"loser_updated_streak INTEGER NOT NULL "
				+ ")";
		
		String createSyncStateTableSql =
				  "CREATE TABLE IF NOT EXISTS sync_state ("
				+	"height INTEGER NOT NULL CHECK (height > 0), "
				+	"block_id INTEGER NOT NULL"
				+ ")";
		
		String clearSyncStateSql = "DELETE FROM sync_state";
		String initSyncStateSql = "INSERT INTO sync_state VALUES (" + Constants.INITIAL_SYNC_HEIGHT + ", " + Constants.INITIAL_SYNC_BLOCK_ID + ")";
		
		sqlite.setAutoCommit(false);
		try (Statement statement = sqlite.createStatement()) {
			statement.executeUpdate(createAccountsTableSql);
			statement.executeUpdate(createRecentBattlesTableSql);
			statement.executeUpdate(createSyncStateTableSql);
			statement.executeUpdate(clearSyncStateSql);
			statement.executeUpdate(initSyncStateSql);
			sqlite.commit();
		} catch (SQLException e) {
			sqlite.rollback();
			throw e;
		} finally {
			sqlite.setAutoCommit(true);
		}
	}
	
	public void addIfNotPresent(Collection<Account> accounts) throws AccountsException {
		try {
			sqlite.setAutoCommit(false);
			try {
				for (Account account : accounts) {
					addIfNotPresent(account);
				}
				sqlite.commit();
			} catch (AccountsException | SQLException e) {
				sqlite.rollback();
				throw e;
			} finally {
				sqlite.setAutoCommit(true);
			}
		} catch (SQLException e) {
			String error = "Database error while attempting to add accounts";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private void addIfNotPresent(Account account) throws AccountsException {
		String insertAccountSql =
				  "INSERT INTO accounts VALUES (?, ?, ?, ?, ?, ?, ?) "
				+ "ON CONFLICT (ardor_account_id) DO NOTHING";
		
		try (PreparedStatement upsertAccount = sqlite.prepareStatement(insertAccountSql)) {
			upsertAccount.setString(1, account.id);
			upsertAccount.setString(2, account.username);
			upsertAccount.setString(3, account.publicKey);
			upsertAccount.setInt(4, account.wins);
			upsertAccount.setInt(5, account.losses);
			upsertAccount.setInt(6, account.streak);
			upsertAccount.setDouble(7, account.rating);
			upsertAccount.executeUpdate();
		} catch (SQLException e) {
			String error = "Database error while attempting to insert account " + account.id;
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public void updateUsernames(List<Account> accounts) throws AccountsException {
		try {
			sqlite.setAutoCommit(false);
			try {
				for (Account account : accounts) {
					updateUsername(account);
				}
				sqlite.commit();
			} catch (AccountsException | SQLException e) {
				sqlite.rollback();
				throw e;
			} finally {
				sqlite.setAutoCommit(true);
			}
		} catch (SQLException e) {
			String error = "Database error while attempting to update usernames";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private void updateUsername(Account account) throws AccountsException {
		String updateUsernameSql =
				  "UPDATE accounts "
				+ "SET username = ? "
				+ "WHERE ardor_account_id = ?";
		try (PreparedStatement updateUsername = sqlite.prepareStatement(updateUsernameSql)) {
			updateUsername.setString(1, account.username);
			updateUsername.setString(2, account.id);
			updateUsername.executeUpdate();
		} catch (SQLException e) {
			String error = "Database error while attempting to update username for account " + account.id;
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public void update(Collection<StateChangeFromBattle> changes) throws AccountsException {
		try {
			sqlite.setAutoCommit(false);
			try {
				int currentHeight = 0;
				long currentBlockId = 0;
				
				for (StateChangeFromBattle change : changes) {
					updateAccountsTable(change);
					insertIntoRecentBattlesTable(change);
					if (change.height > currentHeight && change.blockId > 0) {
						currentHeight = change.height;
						currentBlockId = change.blockId;
					}
				}
				
				if (currentHeight > 0) {
					updateSyncState(currentHeight, currentBlockId);
					purgeBattlesOlderThan(currentHeight - recentBattlesDepth + 1);
				}
				
				sqlite.commit();
			} catch (AccountsException | SQLException e) {
				sqlite.rollback();
				throw e;
			} finally {
				sqlite.setAutoCommit(true);
			}
		} catch (SQLException e) {
			String error = "Database error while attempting to update accounts";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private void updateAccountsTable(StateChangeFromBattle change) throws AccountsException {
		String updateWinnerSql =
				  "UPDATE accounts SET "
				+	"wins = wins + 1, "
				+	"rating = " + change.winner.updated.rating + ", "
				+	"streak = " + change.winner.updated.streak + " "
				+ "WHERE ardor_account_id = " + Long.toUnsignedString(change.winner.id);
		
		String updateLoserSql =
				  "UPDATE accounts SET "
				+	"losses = losses + 1, "
				+	"rating = " + change.loser.updated.rating + ", "
				+	"streak = " + change.loser.updated.streak + " "
				+ "WHERE ardor_account_id = " + Long.toUnsignedString(change.loser.id);
		
		try (Statement statement = sqlite.createStatement()) {
			statement.executeUpdate(updateWinnerSql);
			statement.executeUpdate(updateLoserSql);
		} catch (SQLException e) {
			String error = "Database error while attempting to update accounts " + Long.toUnsignedString(change.winner.id)
					+ " and " + Long.toUnsignedString(change.loser.id);
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private void insertIntoRecentBattlesTable(StateChangeFromBattle change) throws SQLException {
		String insertResultSql =
				  "INSERT INTO recent_battles "
				+	"(finish_height, finish_block_id, winner_id, winner_previous_rating, winner_updated_rating, winner_previous_streak, winner_updated_streak,"
				+	"loser_id, loser_previous_rating, loser_updated_rating, loser_previous_streak, loser_updated_streak) "
				+ "VALUES ("
				+	change.height + ", "
				+	change.blockId + ", "
				+	Long.toUnsignedString(change.winner.id) + ", "
				+	change.winner.previous.rating + ", "
				+	change.winner.updated.rating + ", "
				+	change.winner.previous.streak + ", "
				+	change.winner.updated.streak + ", "
				+	Long.toUnsignedString(change.loser.id) + ", "
				+	change.loser.previous.rating + ", "
				+	change.loser.updated.rating + ", "
				+	change.loser.previous.streak + ", "
				+	change.loser.updated.streak + ")";
		
		try (Statement statement = sqlite.createStatement()) {
			statement.executeUpdate(insertResultSql);
		}
	}
	
	private void purgeBattlesOlderThan(int height) throws SQLException {
		String deleteOldBattlesSql = "DELETE FROM recent_battles WHERE finish_height < " + height;
		try (Statement statement = sqlite.createStatement()) {
			statement.executeUpdate(deleteOldBattlesSql);
		}
	}
	
	public Account getById(long id) throws AccountNotFoundException, AccountsException {
		String getAccountSql = "SELECT * FROM accounts WHERE ardor_account_id = ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setString(1, Long.toUnsignedString(id));
			List<Account> match = executeQueryForAccounts(getAccount);
			if (match.isEmpty()) {
				throw new AccountNotFoundException("Account(s) not found");
			} else {
				return match.get(0);
			}
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve account " + id;
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public Map<Long, Account> getById(Set<Long> ids) throws AccountsException {
		if (ids.isEmpty()) {
			return new HashMap<Long, Account>();
		}
		
		String getAccountsSql = "SELECT * FROM accounts WHERE ardor_account_id IN (?)";
		try (PreparedStatement getAccounts = sqlite.prepareStatement(getAccountsSql)) {
			String idsString = ids.stream()
					.map(id -> Long.toUnsignedString(id))
					.reduce((result, next) -> result + ", " + next)
					.get();
			getAccounts.setString(1, idsString);
			return executeQueryForAccounts(getAccounts).stream()
					.collect(Collectors.toMap(a -> a.numericId(), a -> a));
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve multiple accounts";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public Account getByUsername(String username) throws AccountNotFoundException, AccountsException {
		String getAccountSql = "SELECT * FROM accounts WHERE username = ?";
		try (PreparedStatement getAccount = sqlite.prepareStatement(getAccountSql)) {
			getAccount.setString(1, username);
			List<Account> match = executeQueryForAccounts(getAccount);
			if (match.isEmpty()) {
				throw new AccountNotFoundException("Account(s) not found");
			} else {
				return match.get(0);
			}
		} catch (SQLException e) {
			String error = "Database error while attempting to retrieve account for player '" + username + "'";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private List<Account> executeQueryForAccounts(PreparedStatement getAccount) throws AccountsException, SQLException {
		ResultSet result = getAccount.executeQuery();
		List<Account> matchedAccounts = extractAccounts(result);
		
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
	
	public void updateSyncState(int height, long blockId) throws AccountsException {
		String updateSyncStateSql = "UPDATE sync_state SET height = " + height + ", block_id = " + blockId;
		try (Statement statement = sqlite.createStatement()) {
			statement.executeUpdate(updateSyncStateSql);
		} catch (SQLException e) {
			String error = "Database error while attempting to update sync state";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public BlockSyncInfo getSyncState() throws AccountsException {
		String currentSyncStateSql = "SELECT * FROM sync_state";
		int height = Constants.INITIAL_SYNC_HEIGHT;
		long blockId = Constants.INITIAL_SYNC_BLOCK_ID;
		
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(currentSyncStateSql);
			if (result.next()) {
				height = result.getInt("height");
				blockId = result.getLong("block_id");
			}
			return new BlockSyncInfo(height, blockId);
		} catch (SQLException e) {
			String error = "Database error while attempting to get current sync state";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public List<BlockSyncInfo> getSyncInfoFromRecentStateChanges() throws AccountsException {
		String syncInfoSql =
				  "SELECT DISTINCT finish_height, finish_block_id FROM recent_battles "
				+ "WHERE finish_block_id != 0 "
				+ "ORDER BY finish_height DESC";
		try (Statement statement = sqlite.createStatement()) {
			ResultSet result = statement.executeQuery(syncInfoSql);
			List<BlockSyncInfo> syncInfo = new ArrayList<>();
			while (result.next()) {
				syncInfo.add(new BlockSyncInfo(result.getInt("finish_height"), result.getLong("finish_block_id")));
			}
			return syncInfo;
		} catch (SQLException e) {
			String error = "Database error while attempting to get sync info from recent state changes";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	public void rollBackTo(int height, long blockId) throws AccountsException {
		String battlesToRollBackSql = "SELECT * FROM recent_battles WHERE finish_height > " + height + " ORDER BY finish_height DESC";
		String deleteBattlesSql = "DELETE FROM recent_battles WHERE finish_height > " + height;
		String updateSyncStateSql = "UPDATE sync_state SET height = " + height + ", block_id = " + blockId;
		
		try {
			sqlite.setAutoCommit(false);
			try (Statement statement = sqlite.createStatement()) {
				ResultSet result = statement.executeQuery(battlesToRollBackSql);
				List<StateChangeFromBattle> changes = extractStateChanges(result);
				
				for (StateChangeFromBattle change : changes) {
					undoChangesToAccountsTable(change);
				}
				statement.executeUpdate(deleteBattlesSql);
				statement.executeUpdate(updateSyncStateSql);
				
				sqlite.commit();
			} catch (SQLException e) {
				sqlite.rollback();
				throw e;
			} finally {
				sqlite.setAutoCommit(true);
			}
		} catch (SQLException e) {
			String error = "Database error while rolling back recent battles";
			logger.error(error, e);
			throw new AccountsException(error, e);
		}
	}
	
	private static List<StateChangeFromBattle> extractStateChanges(ResultSet queryResult) throws SQLException {
		List<StateChangeFromBattle> changes = new ArrayList<>();
		while (queryResult.next()) {
			PlayerChange winner = new PlayerChange(
					queryResult.getLong("winner_id"),
					queryResult.getInt("winner_previous_rating"),
					queryResult.getInt("winner_updated_rating"),
					queryResult.getInt("winner_previous_streak"),
					queryResult.getInt("winner_updated_streak")
			);
			
			PlayerChange loser = new PlayerChange(
					queryResult.getLong("loser_id"),
					queryResult.getInt("loser_previous_rating"),
					queryResult.getInt("loser_updated_rating"),
					queryResult.getInt("loser_previous_streak"),
					queryResult.getInt("loser_updated_streak")
			);
			
			changes.add(new StateChangeFromBattle(
					queryResult.getInt("finish_height"),
					queryResult.getLong("finish_block_id"),
					winner,
					loser
			));
		}
		return changes;
	}
	
	private void undoChangesToAccountsTable(StateChangeFromBattle change) throws AccountsException {
		String updateWinnerSql =
				  "UPDATE accounts SET "
				+	"wins = wins - 1,"
				+	"rating = " + change.winner.previous.rating + ", "
				+	"streak = " + change.winner.previous.streak + " "
				+ "WHERE ardor_account_id = " + Long.toUnsignedString(change.winner.id);
		
		String updateLoserSql =
				  "UPDATE accounts SET "
				+	"losses = losses - 1,"
				+	"rating = " + change.loser.previous.rating + ", "
				+	"streak = " + change.loser.previous.streak + " "
				+ "WHERE ardor_account_id = " + Long.toUnsignedString(change.loser.id);
		
		try (Statement statement = sqlite.createStatement()) {
			statement.executeUpdate(updateWinnerSql);
			statement.executeUpdate(updateLoserSql);
		} catch (SQLException e) {
			String error = "Database error while rolling back changes to accounts " + Long.toUnsignedString(change.winner.id)
					+ " and " + Long.toUnsignedString(change.loser.id);
			logger.error(error, e);
			throw new AccountsException(error, e);
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
