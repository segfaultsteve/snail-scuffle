package com.snailscuffle.game.accounts;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class AccountsTest {
	
	private Accounts accounts;
	
	@Before
	public void setUp() throws Exception {
		accounts = new Accounts(":memory:");
	}
	
	@Test
	public void storeAndRetrieveAccount() throws Exception {
		Account storedAccount = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		accounts.insertOrUpdate(storedAccount);
		Account retrievedAccount = accounts.get(AccountQuery.forId(storedAccount.id));
		
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
	public void updateAccount() throws Exception {
		Account storedAccount = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		accounts.insertOrUpdate(storedAccount);
		
		storedAccount.wins++;
		storedAccount.losses++;
		storedAccount.streak = -1;
		accounts.insertOrUpdate(storedAccount);
		Account retrievedAccount = accounts.get(AccountQuery.forId(storedAccount.id));
		
		assertEquals(storedAccount.wins, retrievedAccount.wins);
		assertEquals(storedAccount.losses, retrievedAccount.losses);
		assertEquals(storedAccount.streak, retrievedAccount.streak);
	}
	
	@Test
	public void determineCorrectRanking() throws Exception {
		Account account1 = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		Account account2 = new Account(2, "account2", "pubkey2", 2, 2, 1, 1000);
		Account account3 = new Account(3, "account3", "pubkey3", 0, 3, -3, 700);
		
		// insertion order shouldn't matter
		accounts.insertOrUpdate(account3);
		accounts.insertOrUpdate(account1);
		accounts.insertOrUpdate(account2);
		
		int rank1 = getRank(account1);
		int rank2 = getRank(account2);
		int rank3 = getRank(account3);
		
		assertEquals(1, rank1);
		assertEquals(2, rank2);
		assertEquals(3, rank3);
	}
	
	@Test
	public void determineCorrectRankingWithATie() throws Exception {
		Account tiedForFirst = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		Account alsoTiedForFirst = new Account(2, "account2", "pubkey2", 3, 0, 3, 1500);
		Account third = new Account(3, "account3", "pubkey3", 0, 6, -6, 300);
		accounts.insertOrUpdate(tiedForFirst);
		accounts.insertOrUpdate(alsoTiedForFirst);
		accounts.insertOrUpdate(third);
		
		int rank1 = getRank(tiedForFirst);
		int rank2 = getRank(alsoTiedForFirst);
		int rank3 = getRank(third);
		
		assertEquals(1, rank1);
		assertEquals(1, rank2);
		assertEquals(3, rank3);
	}
	
	private int getRank(Account account) throws Exception {
		return accounts.get(AccountQuery.forId(account.id)).rank;
	}
	
}
