package com.snailscuffle.game.accounts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.game.Constants;
import com.snailscuffle.game.blockchain.StateChangeFromBattle;
import com.snailscuffle.game.blockchain.StateChangeFromBattle.PlayerChange;
import com.snailscuffle.game.blockchain.data.Block;
import com.snailscuffle.game.ratings.RatingPair;
import com.snailscuffle.game.ratings.Ratings;

public class AccountsTest {
	
	private Accounts accounts;
	
	@Before
	public void setUp() throws Exception {
		accounts = new Accounts(":memory:", Constants.RECENT_BATTLES_DEPTH);
	}
	
	@Test
	public void storeAndRetrieveAccount() throws AccountsException {
		Account storedAccount = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
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
		Account account1 = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		Account account2 = new Account(2, "account2", "pubkey2", 2, 2, 1, 1000);
		Account account3 = new Account(3, "account3", "pubkey3", 0, 3, -3, 700);
		
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
		Account tiedForFirst = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500);
		Account alsoTiedForFirst = new Account(2, "account2", "pubkey2", 3, 0, 3, 1500);
		Account third = new Account(3, "account3", "pubkey3", 0, 6, -6, 300);
		accounts.addIfNotPresent(Arrays.asList(tiedForFirst, alsoTiedForFirst, third));
		
		int rank1 = accounts.getById(tiedForFirst.numericId()).rank;
		int rank2 = accounts.getById(alsoTiedForFirst.numericId()).rank;
		int rank3 = accounts.getById(third.numericId()).rank;
		
		assertEquals(1, rank1);
		assertEquals(1, rank2);
		assertEquals(3, rank3);
	}
	
	@Test
	public void orderCachedBlocksByHeight() throws AccountsException {
		Account account1 = new Account(1, "account1", "pubkey1");
		Account account2 = new Account(2, "account2", "pubkey2");
		accounts.addIfNotPresent(Arrays.asList(account1, account2));
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		
		for (int i = 0; i < 5; i++) {
			StateChangeFromBattle changes = changesFromBattle(account1, account2, ++currentHeight, i);
			accounts.update(Arrays.asList(changes));
		}
		List<Block> blocks = accounts.getAllBlocksInCache();
		
		for (int i = 1; i < blocks.size(); i++) {
			assertTrue(blocks.get(i - 1).height > blocks.get(i).height);
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
		
		List<Integer> cachedBlockHeights = accounts.getAllBlocksInCache().stream()
				.map(b -> b.height)
				.collect(Collectors.toList());
		List<Integer> expectedBlockHeights = Arrays.asList(
				Constants.INITIAL_SYNC_HEIGHT + 5,
				Constants.INITIAL_SYNC_HEIGHT + 4,
				Constants.INITIAL_SYNC_HEIGHT + 3
		);
		
		assertEquals(expectedBlockHeights, cachedBlockHeights);
	}
	
	@Test
	public void rollBackToCurrentHeight() throws AccountsException {
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
		
		accounts.rollBackTo(currentHeight);
		
		Account account1AfterRollback = accounts.getById(account1.numericId());
		Account account2AfterRollback = accounts.getById(account2.numericId());
		List<Block> cachedBlocks = accounts.getAllBlocksInCache();
		
		// Rolling back to the current height is a no-op, so the retrieved data for
		// these accounts should match their current states.
		assertEquals(account1.wins, account1AfterRollback.wins);
		assertEquals(account1.losses, account1AfterRollback.losses);
		assertEquals(account2.wins, account2AfterRollback.wins);
		assertEquals(account2.losses, account2AfterRollback.losses);
		
		assertEquals(currentHeight, accounts.getSyncHeight());
		assertEquals(currentHeight, cachedBlocks.get(0).height);
		assertEquals(currentHeight - Constants.INITIAL_SYNC_HEIGHT, cachedBlocks.size());
	}
	
	@Test
	public void rollBackToEarlierHeight() throws AccountsException {
		Account account1 = new Account(1, "account1", "pubkey1");
		Account account2 = new Account(2, "account2", "pubkey2");
		accounts.addIfNotPresent(Arrays.asList(account1, account2));
		
		int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
		int rollbackDepth = 3;
		
		for (int i = 0; i <= rollbackDepth; i++) {
			StateChangeFromBattle changes = changesFromBattle(account1, account2, ++currentHeight, i + 1);
			accounts.update(Arrays.asList(changes));
			account1 = accounts.getById(account1.numericId());
			account2 = accounts.getById(account2.numericId());
		}
		
		accounts.rollBackTo(currentHeight -= rollbackDepth);
		
		Account account1AfterRollback = accounts.getById(account1.numericId());
		Account account2AfterRollback = accounts.getById(account2.numericId());
		List<Block> cachedBlocks = accounts.getAllBlocksInCache();
		
		assertEquals(account1.wins - rollbackDepth, account1AfterRollback.wins);
		assertEquals(account2.losses - rollbackDepth, account2AfterRollback.losses);
		assertEquals(currentHeight, accounts.getSyncHeight());
		assertEquals(currentHeight, cachedBlocks.get(0).height);
		assertEquals(1, cachedBlocks.size());
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
		
		accounts.rollBackTo(Constants.INITIAL_SYNC_HEIGHT);
		
		List<Account> accountsAfterRollback = Arrays.asList(accounts.getById(account1.numericId()), accounts.getById(account2.numericId()));
		for (Account account : accountsAfterRollback) {
			assertEquals(0, account.wins);
			assertEquals(0, account.losses);
			assertEquals(1000, account.rating);
			assertEquals(0, account.streak);
		}
		assertEquals(Constants.INITIAL_SYNC_HEIGHT, accounts.getSyncHeight());
		assertEquals(0, accounts.getAllBlocksInCache().size());
	}
	
	private static StateChangeFromBattle changesFromBattle(Account winner, Account loser, int height, long blockId) {
		RatingPair newRatings = Ratings.compute(winner.rating, loser.rating);
		int newWinnerStreak = (winner.streak > 0) ? (winner.streak + 1) : 1;
		int newLoserStreak = (loser.streak < 0) ? (loser.streak - 1) : -1;
		
		return new StateChangeFromBattle(
			height,
			blockId,
			new PlayerChange(winner.numericId(), winner.rating, newRatings.winner, winner.streak, newWinnerStreak),
			new PlayerChange(loser.numericId(), loser.rating, newRatings.loser, loser.streak, newLoserStreak)
		);
	}
	
}
