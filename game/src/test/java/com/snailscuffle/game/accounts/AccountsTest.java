package com.snailscuffle.game.accounts;

import static com.snailscuffle.game.testutil.AccountsTestUtil.changesFromBattle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.game.Constants;
import com.snailscuffle.game.blockchain.BlockSyncInfo;
import com.snailscuffle.game.blockchain.StateChangeFromBattle;

public class AccountsTest {
	
	private Accounts accounts;
	
	@Before
	public void setUp() throws Exception {
		accounts = new Accounts(":memory:", Constants.RECENT_BATTLES_DEPTH);
	}
	
	@Test
	public void storeAndRetrieveAccount() throws AccountsException {
		Account storedAccount = new Account(1, "account1", "pubkey1", 3, 1, 3, 1050);
		
		accounts.addIfNotPresent(Arrays.asList(storedAccount));
		Account retrievedAccount = accounts.getById(storedAccount.numericId());
		
		assertEquals(storedAccount.id, retrievedAccount.id);
		assertEquals(storedAccount.username, retrievedAccount.username);
		assertEquals(storedAccount.publicKey, retrievedAccount.publicKey);
		assertEquals(storedAccount.wins, retrievedAccount.wins);
		assertEquals(storedAccount.losses, retrievedAccount.losses);
		assertEquals(storedAccount.streak, retrievedAccount.streak);
		assertEquals(storedAccount.rating, retrievedAccount.rating);
		assertEquals(1, retrievedAccount.rank);
		assertEquals(0, retrievedAccount.balance, 0);	// BlockchainSubsystem fills this in
	}
	
	@Test
	public void doNotOverwritePreexistingAccount() throws AccountsException {
		Account preexistingAccount = new Account(1, "account1", "pubkey1", 3, 1, 3, 1050);
		Account sameIdWithNoBattles = new Account(preexistingAccount.numericId(), preexistingAccount.username, preexistingAccount.publicKey);
		
		accounts.addIfNotPresent(Arrays.asList(preexistingAccount));
		accounts.addIfNotPresent(Arrays.asList(sameIdWithNoBattles));
		Account retrievedAccount = accounts.getById(preexistingAccount.numericId());
		
		assertEquals(preexistingAccount.id, retrievedAccount.id);
		assertEquals(preexistingAccount.username, retrievedAccount.username);
		assertEquals(preexistingAccount.publicKey, retrievedAccount.publicKey);
		assertEquals(preexistingAccount.wins, retrievedAccount.wins);
		assertEquals(preexistingAccount.losses, retrievedAccount.losses);
		assertEquals(preexistingAccount.streak, retrievedAccount.streak);
		assertEquals(preexistingAccount.rating, retrievedAccount.rating);
	}
	
	@Test
	public void updateUsername() throws AccountsException {
		Account initial = new Account(1, "account1", "pubkey1", 3, 1, 3, 1050);
		accounts.addIfNotPresent(Arrays.asList(initial));
		
		Account updated = new Account(initial.numericId(), "account1Updated", initial.publicKey);
		accounts.updateUsernames(Arrays.asList(updated));
		
		Account retrieved = accounts.getByUsername(updated.username);
		boolean initialUsernameStillExists = true;
		try {
			accounts.getByUsername(initial.username);
		} catch (AccountsException e) {
			initialUsernameStillExists = false;
		}
		
		assertEquals(updated.username, retrieved.username);
		assertFalse(initialUsernameStillExists);
	}
		
	@Test
	public void updateAccount() throws AccountsException {
		Account account1 = new Account(1, "account1", "pubkey1");
		Account account2 = new Account(2, "account2", "pubkey2");
		accounts.addIfNotPresent(Arrays.asList(account1, account2));
		
		StateChangeFromBattle changes = changesFromBattle(account1, account2, Constants.INITIAL_SYNC_HEIGHT + 1, 1);
		accounts.update(Arrays.asList(changes));
		
		Account account1Updated = accounts.getById(account1.numericId());
		Account account2Updated = accounts.getById(account2.numericId());
		int expectedRatingChange = Constants.MAX_RATING_CHANGE / 2;		// equally rated opponents
		
		assertEquals(1, account1Updated.wins);
		assertEquals(0, account1Updated.losses);
		assertEquals(account1.rating + expectedRatingChange, account1Updated.rating);
		assertEquals(1, account1Updated.streak);
		
		assertEquals(0, account2Updated.wins);
		assertEquals(1, account2Updated.losses);
		assertEquals(account2.rating - expectedRatingChange, account2Updated.rating);
		assertEquals(-1, account2Updated.streak);
	}
	
	@Test
	public void determineCorrectRanking() throws AccountsException {
		Account account1 = new Account(1, "account1", "pubkey1", 3, 0, 3, 1050);
		Account account2 = new Account(2, "account2", "pubkey2", 2, 2, 1, 1000);
		Account account3 = new Account(3, "account3", "pubkey3", 0, 3, -3, 950);
		
		// insertion order shouldn't matter
		accounts.addIfNotPresent(Arrays.asList(account3, account1, account2));
		
		int rank1 = accounts.getById(account1.numericId()).rank;
		int rank2 = accounts.getById(account2.numericId()).rank;
		int rank3 = accounts.getById(account3.numericId()).rank;
		
		assertEquals(1, rank1);
		assertEquals(2, rank2);
		assertEquals(3, rank3);
	}
	
	@Test
	public void determineCorrectRankingWithATie() throws AccountsException {
		Account tiedForFirst = new Account(1, "account1", "pubkey1", 3, 0, 3, 1050);
		Account alsoTiedForFirst = new Account(2, "account2", "pubkey2", 3, 0, 3, 1050);
		Account third = new Account(3, "account3", "pubkey3", 0, 6, -6, 900);
		accounts.addIfNotPresent(Arrays.asList(tiedForFirst, alsoTiedForFirst, third));
		
		int rank1 = accounts.getById(tiedForFirst.numericId()).rank;
		int rank2 = accounts.getById(alsoTiedForFirst.numericId()).rank;
		int rank3 = accounts.getById(third.numericId()).rank;
		
		assertEquals(1, rank1);
		assertEquals(1, rank2);
		assertEquals(3, rank3);
	}
	
	@Test
	public void orderStateChangesByHeight() throws AccountsException {
		Account account1 = new Account(1, "account1", "pubkey1");
		Account account2 = new Account(2, "account2", "pubkey2");
		accounts.addIfNotPresent(Arrays.asList(account1, account2));
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		
		for (int i = 0; i < 5; i++) {
			StateChangeFromBattle changes = changesFromBattle(account1, account2, ++currentHeight, i);
			accounts.update(Arrays.asList(changes));
		}
		List<BlockSyncInfo> blocks = accounts.getSyncInfoFromRecentStateChanges();
		
		for (int i = 1; i < blocks.size(); i++) {
			assertTrue(blocks.get(i - 1).height >= blocks.get(i).height);
		}
	}
	
	@Test
	public void removeOldBlocksFromCache() throws AccountsException {
		accounts = new Accounts(":memory:", 3);		// keep only the three most recent blocks
		
		Account account1 = new Account(1, "account1", "pubkey1");
		Account account2 = new Account(2, "account2", "pubkey2");
		accounts.addIfNotPresent(Arrays.asList(account1, account2));
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		
		for (int i = 0; i < 5; i++) {
			StateChangeFromBattle changes = changesFromBattle(account1, account2, ++currentHeight, i);
			accounts.update(Arrays.asList(changes));
		}
		
		List<Integer> stateChangeHeights = accounts.getSyncInfoFromRecentStateChanges().stream()
				.map(b -> b.height)
				.collect(Collectors.toList());
		List<Integer> expectedStateChangeHeights = Arrays.asList(
				Constants.INITIAL_SYNC_HEIGHT + 5,
				Constants.INITIAL_SYNC_HEIGHT + 4,
				Constants.INITIAL_SYNC_HEIGHT + 3
		);
		
		assertEquals(expectedStateChangeHeights, stateChangeHeights);
	}
	
	@Test
	public void rollBackToCurrentHeight() throws AccountsException {
		Account account1 = new Account(1, "account1", "pubkey1");
		Account account2 = new Account(2, "account2", "pubkey2");
		accounts.addIfNotPresent(Arrays.asList(account1, account2));
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		long currentBlockId = Constants.INITIAL_SYNC_BLOCK_ID;
		
		for (int i = 0; i < 5; i++) {
			StateChangeFromBattle changes = changesFromBattle(account1, account2, ++currentHeight, ++currentBlockId);
			accounts.update(Arrays.asList(changes));
			account1 = accounts.getById(account1.numericId());
			account2 = accounts.getById(account2.numericId());
		}
		List<Account> accountsBeforeRollback = Arrays.asList(account1, account2);
		
		accounts.rollBackTo(currentHeight, currentBlockId);
		
		List<Account> accountsAfterRollback = Arrays.asList(accounts.getById(account1.numericId()), accounts.getById(account2.numericId()));
		List<BlockSyncInfo> blocks = accounts.getSyncInfoFromRecentStateChanges();
		
		// Rolling back to the current height is a no-op, so the retrieved data for
		// these accounts should match their current states.
		for (int i = 0; i < 2; i++) {
			assertEquals(accountsBeforeRollback.get(i).wins, accountsAfterRollback.get(i).wins);
			assertEquals(accountsBeforeRollback.get(i).losses, accountsAfterRollback.get(i).losses);
			assertEquals(accountsBeforeRollback.get(i).rating, accountsAfterRollback.get(i).rating);
			assertEquals(accountsBeforeRollback.get(i).streak, accountsAfterRollback.get(i).streak);
		}
		
		assertEquals(currentHeight, accounts.getSyncState().height);
		assertEquals(currentBlockId, accounts.getSyncState().blockId);
		assertEquals(currentHeight, blocks.get(0).height);
		assertEquals(currentHeight - Constants.INITIAL_SYNC_HEIGHT, blocks.size());
	}
	
	@Test
	public void rollBackToEarlierHeight() throws AccountsException {
		Account account1 = new Account(1, "account1", "pubkey1");
		Account account2 = new Account(2, "account2", "pubkey2");
		accounts.addIfNotPresent(Arrays.asList(account1, account2));
		
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		long currentBlockId = Constants.INITIAL_SYNC_BLOCK_ID;
		int rollbackDepth = 3;
		
		for (int i = 0; i <= rollbackDepth; i++) {
			StateChangeFromBattle changes = changesFromBattle(account1, account2, ++currentHeight, ++currentBlockId);
			accounts.update(Arrays.asList(changes));
			account1 = accounts.getById(account1.numericId());
			account2 = accounts.getById(account2.numericId());
		}
		
		// Roll back to a state that includes only the first battle.
		accounts.rollBackTo(currentHeight -= rollbackDepth, currentBlockId -= rollbackDepth);
		
		Account account1AfterRollback = accounts.getById(account1.numericId());
		Account account2AfterRollback = accounts.getById(account2.numericId());
		List<BlockSyncInfo> blocks = accounts.getSyncInfoFromRecentStateChanges();
		
		assertEquals(1, account1AfterRollback.wins);
		assertEquals(0, account1AfterRollback.losses);
		assertEquals(Constants.INITIAL_RATING + Constants.MAX_RATING_CHANGE / 2, account1AfterRollback.rating);
		assertEquals(1, account1AfterRollback.streak);
		
		assertEquals(0, account2AfterRollback.wins);
		assertEquals(1, account2AfterRollback.losses);
		assertEquals(Constants.INITIAL_RATING - Constants.MAX_RATING_CHANGE / 2, account2AfterRollback.rating);
		assertEquals(-1, account2AfterRollback.streak);
		
		assertEquals(currentHeight, accounts.getSyncState().height);
		assertEquals(currentBlockId, accounts.getSyncState().blockId);
		assertEquals(currentHeight, blocks.get(0).height);
		assertEquals(1, blocks.size());
	}
	
	@Test
	public void rollBackAllBlocks() throws AccountsException {
		Account account1 = new Account(1, "account1", "pubkey1");
		Account account2 = new Account(2, "account2", "pubkey2");
		accounts.addIfNotPresent(Arrays.asList(account1, account2));
		
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		for (int i = 0; i < 5; i++) {
			StateChangeFromBattle changes = changesFromBattle(account1, account2, ++currentHeight, i + 1);
			accounts.update(Arrays.asList(changes));
			account1 = accounts.getById(account1.numericId());
			account2 = accounts.getById(account2.numericId());
		}
		
		accounts.rollBackTo(Constants.INITIAL_SYNC_HEIGHT, Constants.INITIAL_SYNC_BLOCK_ID);
		
		List<Account> accountsAfterRollback = Arrays.asList(accounts.getById(account1.numericId()), accounts.getById(account2.numericId()));
		for (Account account : accountsAfterRollback) {
			assertEquals(0, account.wins);
			assertEquals(0, account.losses);
			assertEquals(1000, account.rating);
			assertEquals(0, account.streak);
		}
		assertEquals(Constants.INITIAL_SYNC_HEIGHT, accounts.getSyncState().height);
		assertEquals(Constants.INITIAL_SYNC_BLOCK_ID, accounts.getSyncState().blockId);
		assertEquals(1, accounts.getSyncInfoFromRecentStateChanges().size());	// includes data from sync_state table
	}
	
}
