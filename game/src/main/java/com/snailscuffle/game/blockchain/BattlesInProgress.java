package com.snailscuffle.game.blockchain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.blockchain.data.BattlePlanMessage;
import com.snailscuffle.game.blockchain.data.OnChain;
import com.snailscuffle.game.ratings.RatingPair;
import com.snailscuffle.game.ratings.Ratings;

class BattlesInProgress {
	
	private static class StartHeightComparator implements Comparator<BattleInProgress> {
		@Override
		public int compare(BattleInProgress b1, BattleInProgress b2) {
			return b1.startHeight() - b2.startHeight();
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(BattlesInProgress.class);
	private final Map<String, BattleInProgress> battlesById = new HashMap<>();
	
	void update(Iterable<OnChain<? extends BattlePlanMessage>> battlePlanMessages) {
		for (OnChain<? extends BattlePlanMessage> message : battlePlanMessages) {
			try {
				update(message);
			} catch (DisqualifyingMessageException e) {
				logger.error("Account " + Long.toUnsignedString(e.accountId) + " disqualified in battle " + message.data.battleId, e);
			} catch (IllegalMessageException e) {
				logger.error("Ignoring invalid message from account " + Long.toUnsignedString(message.sendingAccount), e);
			}
		}
	}
	
	private void update(OnChain<? extends BattlePlanMessage> message) throws DisqualifyingMessageException, IllegalMessageException {
		BattleInProgress theBattle = battlesById.get(message.data.battleId);
		if (theBattle == null) {
			theBattle = new BattleInProgress(message);
			battlesById.put(theBattle.id, theBattle);
		} else {
			theBattle.update(message);
		}
	}
	
	List<Long> getAllAccounts() {
		return battlesById.values().stream()
				.flatMap(b -> b.accounts().stream())
				.distinct()
				.collect(Collectors.toList());
	}
	
	Map<Long, Account> runAll(Map<Long, Account> initialStateOfAccounts, int lastSyncHeight, int currentHeight) {
		Map<Long, Account> updatedAccounts = copy(initialStateOfAccounts);
		List<BattleInProgress> battlesToRemove = new ArrayList<>();
		
		for (BattleInProgress battle : battlesOrderedByHeight()) {
			BattleInProgressResult result = battle.run(currentHeight);
			if (result.isFinished() && result.finishHeight > lastSyncHeight) {
				updateAccounts(result, updatedAccounts);
			}
			if (result.isFinished() || result.wasAborted()) {
				battlesToRemove.add(battle);
			}
		}
		
		for (BattleInProgress battle : battlesToRemove) {
			battlesById.remove(battle.id);
		}
		
		return updatedAccounts;
	}
	
	private static Map<Long, Account> copy(Map<Long, Account> accounts) {
		return accounts.entrySet()
				.stream()
				.collect(Collectors.toMap(kvp -> kvp.getKey(), kvp -> new Account(kvp.getValue())));
	}
	
	private Iterable<BattleInProgress> battlesOrderedByHeight() {
		PriorityQueue<BattleInProgress> battlesByHeight = new PriorityQueue<>(new StartHeightComparator());
		for (BattleInProgress battle : battlesById.values()) {
			battlesByHeight.add(battle);
		}
		return battlesByHeight;
	}
	
	private static void updateAccounts(BattleInProgressResult result, Map<Long, Account> accounts) {
		Account winner = accounts.get(result.winner);
		winner.wins++;
		winner.streak = (winner.streak > 0) ? (winner.streak + 1) : 1;
		
		Account loser = accounts.get(result.loser);
		loser.losses++;
		loser.streak = (loser.streak < 0) ? (loser.streak - 1) : -1;
		
		RatingPair revisedRatings = Ratings.compute(winner.rating, loser.rating);
		winner.rating = revisedRatings.winner;
		loser.rating = revisedRatings.loser;
	}
	
}
