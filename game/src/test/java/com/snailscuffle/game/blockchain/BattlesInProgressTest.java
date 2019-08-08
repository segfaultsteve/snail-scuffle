package com.snailscuffle.game.blockchain;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Weapon;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.blockchain.data.BattlePlanCommitMessage;
import com.snailscuffle.game.blockchain.data.BattlePlanMessage;
import com.snailscuffle.game.blockchain.data.BattlePlanRevealMessage;
import com.snailscuffle.game.blockchain.data.OnChain;

public class BattlesInProgressTest {
	
	private static final int BLOCKS_PER_ROUND = 4;
	private static final long PLAYER_0_ID = 1;
	private static final long PLAYER_1_ID = 2;
	private static final String BATTLE_ID = "00000000-0000-0000-0000-000000000000";
	
	private Map<Long, Account> initialStateOfAccounts = new HashMap<>();
	private BattlePlan winningBp;
	private String winningBpHash;
	private BattlePlan losingBp;
	private String losingBpHash;
	private List<OnChain<? extends BattlePlanMessage>> player0Messages = new ArrayList<>();
	private List<OnChain<? extends BattlePlanMessage>> player1Messages = new ArrayList<>();
	
	@Before
	public void setUp() {
		initialStateOfAccounts.put(PLAYER_0_ID, new Account(PLAYER_0_ID, "player0", "pubKey0"));
		initialStateOfAccounts.put(PLAYER_1_ID, new Account(PLAYER_1_ID, "player1", "pubKey1"));
		
		winningBp = new BattlePlan();
		winningBp.snail = Snail.DALE;
		winningBp.weapon = Weapon.RIFLE;
		winningBp.shell = Shell.ALUMINUM;
		winningBp.accessory = Accessory.DEFIBRILLATOR;
		winningBp.validate();
		
		winningBpHash = BlockchainUtil.sha256Hash(winningBp);
		
		losingBp = new BattlePlan();
		losingBp.snail = Snail.DALE;
		losingBp.weapon = Weapon.RIFLE;
		losingBp.shell = Shell.ALUMINUM;
		// no accessory - this should guarantee a loss
		losingBp.validate();
		
		losingBpHash = BlockchainUtil.sha256Hash(losingBp);
	}
	
	@Test
	public void runOneCompleteBattle() {
		final int ROUNDS = 3;
		
		for (int i = 0; i < ROUNDS; i++) {
			player0Messages.add(newCommitMessage(winningBpHash, i, i * BLOCKS_PER_ROUND, PLAYER_0_ID));
			player1Messages.add(newCommitMessage(losingBpHash, i, i * BLOCKS_PER_ROUND + 1, PLAYER_1_ID));
			
			player0Messages.add(newRevealMessage(winningBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_0_ID));
			player1Messages.add(newRevealMessage(losingBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_1_ID));
		}
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(ROUNDS * BLOCKS_PER_ROUND);
		
		Account player0 = finalStateOfAccounts.get(PLAYER_0_ID);
		Account player1 = finalStateOfAccounts.get(PLAYER_1_ID);
		
		assertWinner(player0);
		assertLoser(player1);
	}
	
	@Test
	public void ignoreOngoingBattleWithOneHashAndNoBattlePlans() {
		final int CURRENT_HEIGHT = 1;
		
		player0Messages.add(newCommitMessage(winningBpHash, 0, 0, PLAYER_0_ID));
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(CURRENT_HEIGHT);
		
		assertNoChange(finalStateOfAccounts.get(PLAYER_0_ID));
		assertNoChange(finalStateOfAccounts.get(PLAYER_1_ID));
	}
	
	@Test
	public void ignoreOngoingBattleWithTwoHashesAndNoBattlePlans() {
		final int CURRENT_HEIGHT = 1;
		
		player0Messages.add(newCommitMessage(winningBpHash, 0, 0, PLAYER_0_ID));
		player1Messages.add(newCommitMessage(losingBpHash, 0, 1, PLAYER_1_ID));
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(CURRENT_HEIGHT);
		
		assertNoChange(finalStateOfAccounts.get(PLAYER_0_ID));
		assertNoChange(finalStateOfAccounts.get(PLAYER_1_ID));
	}
	
	@Test
	public void ignoreOngoingBattleWithBattlePlans() {
		final int CURRENT_HEIGHT = 2;
		
		player0Messages.add(newCommitMessage(winningBpHash, 0, 0, PLAYER_0_ID));
		player1Messages.add(newCommitMessage(losingBpHash, 0, 1, PLAYER_1_ID));
		
		player0Messages.add(newRevealMessage(winningBp, 0, 2, PLAYER_0_ID));
		player1Messages.add(newRevealMessage(losingBp, 0, 2, PLAYER_1_ID));
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(CURRENT_HEIGHT);
		
		assertNoChange(finalStateOfAccounts.get(PLAYER_0_ID));
		assertNoChange(finalStateOfAccounts.get(PLAYER_1_ID));
	}
	
	@Test
	public void ignoreTimedOutBattleThatSecondMoverNeverAcknowledges() {
		final int CURRENT_HEIGHT = 2 * Constants.MAX_BLOCKS_PER_ROUND;
		
		player0Messages.add(newCommitMessage(winningBpHash, 0, 0, PLAYER_0_ID));
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(CURRENT_HEIGHT);
		
		assertNoChange(finalStateOfAccounts.get(PLAYER_0_ID));
		assertNoChange(finalStateOfAccounts.get(PLAYER_1_ID));
	}
	
	@Test
	public void forfeitBattleInFirstRoundByNotRevealingBattlePlan() {
		final int CURRENT_HEIGHT = 2 * Constants.MAX_BLOCKS_PER_ROUND;
		
		player0Messages.add(newCommitMessage(winningBpHash, 0, 0, PLAYER_0_ID));
		player1Messages.add(newCommitMessage(losingBpHash, 0, 1, PLAYER_1_ID));
		
		// player 0 doesn't reveal
		player1Messages.add(newRevealMessage(losingBp, 0, 2, PLAYER_1_ID));
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(CURRENT_HEIGHT);
		
		assertWinner(finalStateOfAccounts.get(PLAYER_1_ID));
		assertLoser(finalStateOfAccounts.get(PLAYER_0_ID));
	}
	
	@Test
	public void forfeitBattleInLaterRoundByNotCommittingHash() {
		final int ROUNDS = 3;
		
		for (int i = 0; i < ROUNDS; i++) {
			player0Messages.add(newCommitMessage(winningBpHash, i, i * BLOCKS_PER_ROUND, PLAYER_0_ID));
			player1Messages.add(newCommitMessage(losingBpHash, i, i * BLOCKS_PER_ROUND + 1, PLAYER_1_ID));
			
			if (i < ROUNDS - 1) {	// don't reveal battle plans during last round
				player0Messages.add(newRevealMessage(winningBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_0_ID));
				player1Messages.add(newRevealMessage(losingBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_1_ID));
			}
		}
		
		player0Messages.remove(player0Messages.size() - 1);		// remove last commit message
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(ROUNDS * BLOCKS_PER_ROUND + Constants.MAX_BLOCKS_PER_ROUND);
		
		assertWinner(finalStateOfAccounts.get(PLAYER_1_ID));
		assertLoser(finalStateOfAccounts.get(PLAYER_0_ID));
	}
	
	@Test
	public void forfeitBattleInLaterRoundByNotRevealingBattlePlan() {
		final int ROUNDS = 3;
		
		for (int i = 0; i < ROUNDS; i++) {
			player0Messages.add(newCommitMessage(winningBpHash, i, i * BLOCKS_PER_ROUND, PLAYER_0_ID));
			player1Messages.add(newCommitMessage(losingBpHash, i, i * BLOCKS_PER_ROUND + 1, PLAYER_1_ID));
			
			player0Messages.add(newRevealMessage(winningBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_0_ID));
			player1Messages.add(newRevealMessage(losingBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_1_ID));
		}
		
		player0Messages.remove(player0Messages.size() - 1);
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(ROUNDS * BLOCKS_PER_ROUND + Constants.MAX_BLOCKS_PER_ROUND);
		
		assertWinner(finalStateOfAccounts.get(PLAYER_1_ID));
		assertLoser(finalStateOfAccounts.get(PLAYER_0_ID));
	}
	
	@Test
	public void forfeitBattleByCommittingHashTooLate() {
		final int ROUNDS = 3;
		
		for (int i = 0; i < ROUNDS; i++) {
			player0Messages.add(newCommitMessage(winningBpHash, i, i * BLOCKS_PER_ROUND, PLAYER_0_ID));
			player1Messages.add(newCommitMessage(losingBpHash, i, i * BLOCKS_PER_ROUND + 1, PLAYER_1_ID));
			
			player0Messages.add(newRevealMessage(winningBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_0_ID));
			player1Messages.add(newRevealMessage(losingBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_1_ID));
		}
		
		OnChain<BattlePlanCommitMessage> committedTooLate = newCommitMessage(winningBpHash, ROUNDS - 1, ROUNDS * BLOCKS_PER_ROUND, PLAYER_0_ID);
		player0Messages.set(2*ROUNDS - 2, committedTooLate);
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(ROUNDS * BLOCKS_PER_ROUND + Constants.MAX_BLOCKS_PER_ROUND);
		
		assertWinner(finalStateOfAccounts.get(PLAYER_1_ID));
		assertLoser(finalStateOfAccounts.get(PLAYER_0_ID));
	}
	
	@Test
	public void forfeitBattleInFirstRoundByRevealingBattlePlanTooLate() {
		final int COMMIT_HEIGHT = 0;
		final int REVEAL_IN_TIME_HEIGHT = Constants.MAX_BLOCKS_AFTER_HASHES_COMMITTED;
		final int REVEAL_TOO_LATE_HEIGHT = Constants.MAX_BLOCKS_AFTER_HASHES_COMMITTED + 1;
		
		player0Messages.add(newCommitMessage(winningBpHash, 0, COMMIT_HEIGHT, PLAYER_0_ID));
		player1Messages.add(newCommitMessage(losingBpHash, 0, COMMIT_HEIGHT, PLAYER_1_ID));
		
		player0Messages.add(newRevealMessage(winningBp, 0, REVEAL_TOO_LATE_HEIGHT, PLAYER_0_ID));
		player1Messages.add(newRevealMessage(losingBp, 0, REVEAL_IN_TIME_HEIGHT, PLAYER_1_ID));
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(2 * Constants.MAX_BLOCKS_PER_ROUND);
		
		assertWinner(finalStateOfAccounts.get(PLAYER_1_ID));
		assertLoser(finalStateOfAccounts.get(PLAYER_0_ID));
	}
	
	@Test
	public void forfeitBattleInLaterRoundByRevealingBattlePlanTooLate() {
		final int ROUNDS = 3;
		
		for (int i = 0; i < ROUNDS; i++) {
			player0Messages.add(newCommitMessage(winningBpHash, i, i * BLOCKS_PER_ROUND, PLAYER_0_ID));
			player1Messages.add(newCommitMessage(losingBpHash, i, i * BLOCKS_PER_ROUND + 1, PLAYER_1_ID));
			
			player0Messages.add(newRevealMessage(winningBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_0_ID));
			player1Messages.add(newRevealMessage(losingBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_1_ID));
		}
		
		OnChain<BattlePlanRevealMessage> revealedTooLate = newRevealMessage(winningBp, ROUNDS - 1, ROUNDS * BLOCKS_PER_ROUND, PLAYER_0_ID);
		player0Messages.set(2*ROUNDS - 1, revealedTooLate);
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(ROUNDS * BLOCKS_PER_ROUND + Constants.MAX_BLOCKS_PER_ROUND);
		
		assertWinner(finalStateOfAccounts.get(PLAYER_1_ID));
		assertLoser(finalStateOfAccounts.get(PLAYER_0_ID));
	}
	
	@Test
	public void disqualifyPlayerWhenBattlePlanDoesNotMatchHash() {
		final int CURRENT_HEIGHT = 2;	// note that we don't even have to play a full battle
		
		player0Messages.add(newCommitMessage(winningBpHash, 0, 0, PLAYER_0_ID));
		player1Messages.add(newCommitMessage("not the right hash!", 0, 1, PLAYER_1_ID));
		
		player0Messages.add(newRevealMessage(winningBp, 0, 2, PLAYER_0_ID));
		player1Messages.add(newRevealMessage(losingBp, 0, 2, PLAYER_1_ID));
		
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(CURRENT_HEIGHT);
		
		assertWinner(finalStateOfAccounts.get(PLAYER_0_ID));
		assertLoser(finalStateOfAccounts.get(PLAYER_1_ID));
	}
	
	@Test
	public void doNotRecordBattleThatConcludesBeforeInitialHeight() {
		final int ROUNDS = 3;
		
		for (int i = 0; i < ROUNDS; i++) {
			player0Messages.add(newCommitMessage(winningBpHash, i, i * BLOCKS_PER_ROUND, PLAYER_0_ID));
			player1Messages.add(newCommitMessage(losingBpHash, i, i * BLOCKS_PER_ROUND + 1, PLAYER_1_ID));
			
			player0Messages.add(newRevealMessage(winningBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_0_ID));
			player1Messages.add(newRevealMessage(losingBp, i, i * BLOCKS_PER_ROUND + 2, PLAYER_1_ID));
		}
		
		int finishHeight = player0Messages.get(2 * ROUNDS - 1).height;
		Map<Long, Account> finalStateOfAccounts = runBattleAndUpdateAccounts(finishHeight, finishHeight + 10);	// initial height is finishHeight
		
		// First mover won, but we shouldn't record any change because the battle concluded
		// on a block (finishHeight) that we said we had already synced.
		assertNoChange(finalStateOfAccounts.get(PLAYER_0_ID));
		assertNoChange(finalStateOfAccounts.get(PLAYER_1_ID));
	}
	
	@Test
	public void rollBack() {
		final int ROUNDS = 3;
		
		IntFunction<List<OnChain<? extends BattlePlanMessage>>> messagesForRound = (round) -> {
			return Arrays.asList(
				newCommitMessage(winningBpHash, round, round * BLOCKS_PER_ROUND, PLAYER_0_ID),
				newCommitMessage(losingBpHash, round, round * BLOCKS_PER_ROUND + 1, PLAYER_1_ID),
				newRevealMessage(winningBp, round, round * BLOCKS_PER_ROUND + 2, PLAYER_0_ID),
				newRevealMessage(losingBp, round, round * BLOCKS_PER_ROUND + 2, PLAYER_1_ID)
			);
		};
		
		List<OnChain<? extends BattlePlanMessage>> messages = new ArrayList<>();
		for (int i = 0; i < ROUNDS; i++) {
			messages.addAll(messagesForRound.apply(i));
		}
		
		BattlesInProgress battles = new BattlesInProgress(Constants.RECENT_BATTLES_DEPTH);
		battles.update(messages);
		
		// Run a full battle.
		Collection<StateChangeFromBattle> changes = battles.runAll(initialStateOfAccounts, 0, ROUNDS * BLOCKS_PER_ROUND);
		Map<Long, Account> accountsBeforeRollback = updateAccounts(changes, initialStateOfAccounts);
		
		// Roll back the final round.
		int justBeforeFinalRound = (ROUNDS - 1) * BLOCKS_PER_ROUND - 1;
		battles.rollBackTo(justBeforeFinalRound);
		
		changes = battles.runAll(initialStateOfAccounts, 0, justBeforeFinalRound);
		Map<Long, Account> accountsAfterRollback = updateAccounts(changes, initialStateOfAccounts);
		
		// Redo *just* the final round.
		List<OnChain<? extends BattlePlanMessage>> messagesForFinalRound = messagesForRound.apply(ROUNDS - 1);
		battles.update(messagesForFinalRound);
		
		changes = battles.runAll(initialStateOfAccounts, 0, ROUNDS * BLOCKS_PER_ROUND);
		Map<Long, Account> accountsAfterRedo = updateAccounts(changes, initialStateOfAccounts);
		
		// We should observe the result of the battle before rolling back and after redoing the
		// final round, but not immediately after the rollback.
		assertWinner(accountsBeforeRollback.get(PLAYER_0_ID));
		assertLoser(accountsBeforeRollback.get(PLAYER_1_ID));
		
		assertNoChange(accountsAfterRollback.get(PLAYER_0_ID));
		assertNoChange(accountsAfterRollback.get(PLAYER_1_ID));
		
		assertWinner(accountsAfterRedo.get(PLAYER_0_ID));
		assertLoser(accountsAfterRedo.get(PLAYER_1_ID));
	}
	
	private static OnChain<BattlePlanCommitMessage> newCommitMessage(String hash, int round, int height, long sender) {
		long recipient = (sender == PLAYER_0_ID) ? PLAYER_1_ID : PLAYER_0_ID;
		BattlePlanCommitMessage commitMessage = new BattlePlanCommitMessage(BATTLE_ID, round, hash);
		return new OnChain<BattlePlanCommitMessage>(0, height, 0, sender, recipient, commitMessage);
	}
	
	private static OnChain<BattlePlanRevealMessage> newRevealMessage(BattlePlan battlePlan, int round, int height, long sender) {
		long recipient = (sender == PLAYER_0_ID) ? PLAYER_1_ID : PLAYER_0_ID;
		BattlePlanRevealMessage revealMessage = new BattlePlanRevealMessage(BATTLE_ID, round, battlePlan);
		return new OnChain<BattlePlanRevealMessage>(0, height, 0, sender, recipient, revealMessage);
	}
	
	private Map<Long, Account> runBattleAndUpdateAccounts(int currentHeight) {
		return runBattleAndUpdateAccounts(0, currentHeight);
	}
	
	private Map<Long, Account> runBattleAndUpdateAccounts(int initialHeight, int currentHeight) {
		Collection<StateChangeFromBattle> changes = runBattle(initialHeight, currentHeight);
		return updateAccounts(changes, initialStateOfAccounts);
	}
	
	private Collection<StateChangeFromBattle> runBattle(int initialHeight, int currentHeight) {
		BattlesInProgress battles = new BattlesInProgress(Constants.RECENT_BATTLES_DEPTH);
		battles.update(player0Messages);
		battles.update(player1Messages);
		
		return battles.runAll(initialStateOfAccounts, initialHeight, currentHeight);
	}
	
	private static Map<Long, Account> updateAccounts(Collection<StateChangeFromBattle> changes, Map<Long, Account> initialStateOfAccounts) {
		Map<Long, Account> updatedAccounts = copy(initialStateOfAccounts);
		for (StateChangeFromBattle change : changes) {
			Account winner = updatedAccounts.get(change.winner.id);
			winner.wins++;
			winner.rating = change.winner.updated.rating;
			winner.streak = change.winner.updated.streak;
			
			Account loser = updatedAccounts.get(change.loser.id);
			loser.losses++;
			loser.rating = change.loser.updated.rating;
			loser.streak = change.loser.updated.streak;
		}
		return updatedAccounts;
	}
	
	private static Map<Long, Account> copy(Map<Long, Account> accounts) {
		return accounts.entrySet()
				.stream()
				.collect(Collectors.toMap(kvp -> kvp.getKey(), kvp -> new Account(kvp.getValue())));
	}
	
	private static void assertWinner(Account player) {
		assertEquals(1, player.wins);
		assertEquals(0, player.losses);
		assertEquals(1, player.streak);
		assertEquals(Constants.INITIAL_RATING + Constants.MAX_RATING_CHANGE/2, player.rating);
	}
	
	private static void assertLoser(Account player) {
		assertEquals(0, player.wins);
		assertEquals(1, player.losses);
		assertEquals(-1, player.streak);
		assertEquals(Constants.INITIAL_RATING - Constants.MAX_RATING_CHANGE/2, player.rating);
	}
	
	private static void assertNoChange(Account player) {
		assertEquals(0, player.wins);
		assertEquals(0, player.losses);
		assertEquals(0, player.streak);
		assertEquals(Constants.INITIAL_RATING, player.rating);
	}
	
}
