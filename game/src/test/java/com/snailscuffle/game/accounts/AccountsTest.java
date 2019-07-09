package com.snailscuffle.game.accounts;

import static org.junit.Assert.assertEquals;

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
		Account retrievedAccount = accounts.getById(storedAccount.id);
		
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
		Account retrievedAccount = accounts.getById(storedAccount.id);
		
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
		
		int rank1 = accounts.getById(account1.id).rank;
		int rank2 = accounts.getById(account2.id).rank;
		int rank3 = accounts.getById(account3.id).rank;
		
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
		
		int rank1 = accounts.getById(tiedForFirst.id).rank;
		int rank2 = accounts.getById(alsoTiedForFirst.id).rank;
		int rank3 = accounts.getById(third.id).rank;
		
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
	
}
