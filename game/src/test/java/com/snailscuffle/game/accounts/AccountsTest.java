package com.snailscuffle.game.accounts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.game.Constants;

public class AccountsTest {
	
	private Accounts accounts;
	
	@Before
	public void setUp() throws Exception {
		accounts = new Accounts(":memory:", Constants.MAX_SNAPSHOT_COUNT);
	}
	
	@Test
	public void initializeSnapshotTable() throws AccountsException {
		Map<String, AccountsSnapshot> snapshots = accounts.getAllSnapshots();
		
		assertEquals(1, snapshots.size());
		assertEquals("accounts", snapshots.get("accounts").name);
		assertEquals(Constants.INITIAL_SYNC_HEIGHT, snapshots.get("accounts").height);
		assertEquals(Constants.INITIAL_SYNC_BLOCK_ID, snapshots.get("accounts").blockId);
	}
	
	@Test
	public void storeAndRetrieveAccount() throws AccountsException {
		Account storedAccount = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		accounts.insertOrUpdate(storedAccount);
		Account retrievedAccount = accounts.getById(storedAccount.numericId());
		
		assertEquals(storedAccount.id, retrievedAccount.id);
		assertEquals(storedAccount.username, retrievedAccount.username);
		assertEquals(storedAccount.publicKey, retrievedAccount.publicKey);
		assertEquals(storedAccount.wins, retrievedAccount.wins);
		assertEquals(storedAccount.losses, retrievedAccount.losses);
		assertEquals(storedAccount.streak, retrievedAccount.streak);
		assertEquals(storedAccount.rating, retrievedAccount.rating);
		assertEquals(1, retrievedAccount.rank);
		assertEquals(0, retrievedAccount.balance, 0);		// BlockchainSubsystem fills this in
	}
	
	@Test
	public void updateAccount() throws AccountsException {
		Account storedAccount = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		accounts.insertOrUpdate(storedAccount);
		
		storedAccount.wins++;
		storedAccount.losses++;
		storedAccount.streak = -1;
		accounts.insertOrUpdate(storedAccount);
		Account retrievedAccount = accounts.getById(storedAccount.numericId());
		
		assertEquals(storedAccount.wins, retrievedAccount.wins);
		assertEquals(storedAccount.losses, retrievedAccount.losses);
		assertEquals(storedAccount.streak, retrievedAccount.streak);
	}
	
	@Test
	public void determineCorrectRanking() throws AccountsException {
		Account account1 = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		Account account2 = new Account(2, "account2", "pubkey2", 2, 2, 1, 1000);
		Account account3 = new Account(3, "account3", "pubkey3", 0, 3, -3, 700);
		
		// insertion order shouldn't matter
		accounts.insertOrUpdate(account3);
		accounts.insertOrUpdate(account1);
		accounts.insertOrUpdate(account2);
		
		int rank1 = accounts.getById(account1.numericId()).rank;
		int rank2 = accounts.getById(account2.numericId()).rank;
		int rank3 = accounts.getById(account3.numericId()).rank;
		
		assertEquals(1, rank1);
		assertEquals(2, rank2);
		assertEquals(3, rank3);
	}
	
	@Test
	public void determineCorrectRankingWithATie() throws AccountsException {
		Account tiedForFirst = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		Account alsoTiedForFirst = new Account(2, "account2", "pubkey2", 3, 0, 3, 1500);
		Account third = new Account(3, "account3", "pubkey3", 0, 6, -6, 300);
		accounts.insertOrUpdate(tiedForFirst);
		accounts.insertOrUpdate(alsoTiedForFirst);
		accounts.insertOrUpdate(third);
		
		int rank1 = accounts.getById(tiedForFirst.numericId()).rank;
		int rank2 = accounts.getById(alsoTiedForFirst.numericId()).rank;
		int rank3 = accounts.getById(third.numericId()).rank;
		
		assertEquals(1, rank1);
		assertEquals(1, rank2);
		assertEquals(3, rank3);
	}
	
	@Test
	public void takeSnapshot() throws AccountsException {
		accounts.takeSnapshot("test");
		Map<String, AccountsSnapshot> snapshots = accounts.getAllSnapshots();
		
		assertEquals(2, snapshots.size());
		assertEquals("accounts", snapshots.get("accounts").name);
		assertEquals("accounts_test", snapshots.get("accounts_test").name);
		for (AccountsSnapshot snapshot : snapshots.values()) {
			assertEquals(Constants.INITIAL_SYNC_HEIGHT, snapshot.height);
			assertEquals(Constants.INITIAL_SYNC_BLOCK_ID, snapshot.blockId);
		}
	}
	
	@Test
	public void deleteOldSnapshots() throws AccountsException {
		accounts = new Accounts(":memory:", 3);		// keep only the three most recent snapshots
		
		accounts.takeSnapshot("1");
		accounts.takeSnapshot("2");
		accounts.takeSnapshot("3");
		accounts.takeSnapshot("4");
		accounts.takeSnapshot("5");
		Map<String, AccountsSnapshot> snapshots = accounts.getAllSnapshots();
		
		assertEquals(4, snapshots.size());
		assertEquals("accounts", snapshots.get("accounts").name);
		assertEquals("accounts_3", snapshots.get("accounts_3").name);
		assertEquals("accounts_4", snapshots.get("accounts_4").name);
		assertEquals("accounts_5", snapshots.get("accounts_5").name);
	}
	
	@Test
	public void rollBackToCurrentHeight() throws AccountsException {
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		Account account = new Account(1, "account1", "pubkey1", 0, 0, 0, 1000);
		accounts.insertOrUpdate(account);
		accounts.updateSyncHeight(++currentHeight, 0);
		accounts.takeSnapshot("1");
		
		account.wins++;
		accounts.insertOrUpdate(account);
		accounts.updateSyncHeight(++currentHeight, 0);
		
		long rollbackHeight = accounts.rollBack(currentHeight);
		Account retrieved = accounts.getById(account.numericId());
		Map<String, AccountsSnapshot> snapshots = accounts.getAllSnapshots();
		
		// Rolling back to the current height is a no-op, so the retrieved data for this
		// account should match the current state.
		assertEquals(account.wins, retrieved.wins);
		assertEquals(2, snapshots.size());		// "accounts" and "accounts_1"
		assertEquals(currentHeight, rollbackHeight);
		assertEquals(currentHeight, snapshots.get("accounts").height);
	}
	
	@Test
	public void rollBackToSnapshot() throws AccountsException {
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		int rollbackDepth = 3;
		Account account = new Account(1, "account1", "pubkey1", 0, 0, 0, 1000);
		
		for (int i = 1; i <= rollbackDepth + 1; i++) {
			account.wins++;
			accounts.insertOrUpdate(account);
			accounts.updateSyncHeight(++currentHeight, 0);
			accounts.takeSnapshot(String.valueOf(i));
		}
		
		long rollbackHeight = accounts.rollBack(currentHeight -= rollbackDepth);
		Account retrieved = accounts.getById(account.numericId());
		Map<String, AccountsSnapshot> snapshots = accounts.getAllSnapshots();
		
		assertEquals(account.wins - rollbackDepth, retrieved.wins);
		assertEquals(2, snapshots.size());
		assertEquals(currentHeight, rollbackHeight);
		assertEquals(currentHeight, snapshots.get("accounts").height);
	}
	
	@Test
	public void rollBackAllSnapshots() throws AccountsException {
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		for (int i = 0; i < 3; i++) {
			accounts.updateSyncHeight(++currentHeight, 0);
			accounts.takeSnapshot(String.valueOf(i));
		}
		
		long rollbackHeight = accounts.rollBack(Constants.INITIAL_SYNC_HEIGHT);
		Map<String, AccountsSnapshot> snapshots = accounts.getAllSnapshots();
		
		assertEquals(1, snapshots.size());
		assertEquals(Constants.INITIAL_SYNC_HEIGHT, rollbackHeight);
		assertEquals(Constants.INITIAL_SYNC_HEIGHT, snapshots.get("accounts").height);
	}
	
	@Test
	public void rollingBackDeletesNewAccounts() throws AccountsException {
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		Account account1 = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		Account account2 = new Account(2, "account2", "pubkey2", 2, 2, 1, 1000);
		
		accounts.insertOrUpdate(account1);
		accounts.updateSyncHeight(++currentHeight, 0);
		accounts.takeSnapshot("1");
		accounts.insertOrUpdate(account2);
		accounts.updateSyncHeight(++currentHeight, 0);
		
		Account account2BeforeRollback = getAccountOrNull(account2.numericId());
		accounts.rollBack(--currentHeight);
		Account account2AfterRollback = getAccountOrNull(account2.numericId());
		
		assertNotNull(account2BeforeRollback);
		assertNull(account2AfterRollback);
	}
	
	private Account getAccountOrNull(long id) {
		try {
			return accounts.getById(id);
		} catch (AccountsException e) {
			return null;
		}
	}
	
}
