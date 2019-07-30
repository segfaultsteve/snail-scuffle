package com.snailscuffle.game.blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import com.snailscuffle.common.battle.BattleConfig;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.BattleResult;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.battle.Battle;
import com.snailscuffle.game.blockchain.data.BattlePlanCommitMessage;
import com.snailscuffle.game.blockchain.data.BattlePlanMessage;
import com.snailscuffle.game.blockchain.data.BattlePlanRevealMessage;
import com.snailscuffle.game.blockchain.data.OnChain;

class BattleInProgress {
	
	private static class BattleMessages {
		private final List<OnChain<String>> committedHashes = new ArrayList<>();
		private final List<OnChain<BattlePlan>> battlePlans = new ArrayList<>();
	}
	
	final String id;
	private final Map<Long, BattleMessages> messagesByPlayer = new HashMap<>();
	private long disqualified;
	private int disqualificationHeight = Integer.MAX_VALUE;
	
	BattleInProgress(OnChain<? extends BattlePlanMessage> firstMessage) throws IllegalMessageException {
		id = firstMessage.data.battleId;
		messagesByPlayer.put(firstMessage.sendingAccount, new BattleMessages());
		messagesByPlayer.put(firstMessage.receivingAccount, new BattleMessages());
		update(firstMessage);
	}

	void update(OnChain<? extends BattlePlanMessage> message) throws DisqualifyingMessageException, IllegalMessageException {
		throwOnIncompatibleProtocol(message.data.protocolMajorVersion, message.data.protocolMinorVersion);
		throwOnUnrecognizedPlayer(message.sendingAccount);
		throwOnUnrecognizedPlayer(message.receivingAccount);
		
		try {
			BattleMessages senderMessages = messagesByPlayer.get(message.sendingAccount);
			if (message.data instanceof BattlePlanCommitMessage) {
				addBattlePlanHash(message, senderMessages.committedHashes);
			} else if (message.data instanceof BattlePlanRevealMessage) {
				addBattlePlan(message, senderMessages.battlePlans, senderMessages.committedHashes);
			} else {
				throw new IllegalMessageException("Unrecognized message type");
			}
		} catch (DisqualifyingMessageException e) {
			disqualify(message.sendingAccount, message.height);
			throw e;
		}
	}
	
	private static void throwOnIncompatibleProtocol(int protocolMajorVersion, int protocolMinorVersion) throws IllegalMessageException {
		if (protocolMajorVersion != Constants.PROTOCOL_MAJOR_VERSION || protocolMinorVersion != Constants.PROTOCOL_MINOR_VERSION) {
			throw new IllegalMessageException("Incompatible protocol version");
		}
	}
	
	private void throwOnUnrecognizedPlayer(long accountId) throws IllegalMessageException {
		if (!messagesByPlayer.containsKey(accountId)) {
			throw new IllegalMessageException("Account " + accountId + " is not part of battle " + id);
		}
	}
	
	private static void addBattlePlanHash(OnChain<? extends BattlePlanMessage> message, List<OnChain<String>> committedHashes) throws DisqualifyingMessageException, IllegalMessageException {
		String theHash = ((BattlePlanCommitMessage)message.data).battlePlanHash;
		int round = message.data.round;
		if (round < committedHashes.size() && !committedHashes.get(round).data.equalsIgnoreCase(theHash)) {
			String error = "Committed hash '" + theHash + "' does not match previously committed hash '" + committedHashes.get(round).data + "'";
			throw new DisqualifyingMessageException(message.sendingAccount, error);
		} else if (round > committedHashes.size()) {
			throw new IllegalMessageException("Cannot commit hash for round " + round + " before committing hash for round " + (round - 1));
		} else if (round == committedHashes.size()) {
			OnChain<String> wrappedHash = new OnChain<>(message.height, message.transactionIndex, message.sendingAccount, message.receivingAccount, theHash);
			committedHashes.add(wrappedHash);
		}
	}
	
	private static void addBattlePlan(OnChain<? extends BattlePlanMessage> message, List<OnChain<BattlePlan>> battlePlans, List<OnChain<String>> committedHashes) throws IllegalMessageException {
		BattlePlan theBattlePlan = ((BattlePlanRevealMessage)message.data).battlePlan;
		int round = message.data.round;
		if (round >= committedHashes.size()) {
			throw new IllegalMessageException("Cannot find hash for round " + round);
		}
		
		String expectedHash = committedHashes.get(round).data;
		String actualHash = BlockchainUtil.sha256Hash(theBattlePlan);
		
		if (!actualHash.equalsIgnoreCase(expectedHash)) {
			String error = "Battle plan hash'" + actualHash + "' does not match previously committed hash '" + expectedHash + "'";
			throw new DisqualifyingMessageException(message.sendingAccount, error);
		} else if (round > battlePlans.size()) {
			throw new IllegalMessageException("Battle plan for round " + (round - 1) + " must precede the one for round " + round);
		} else if (round == battlePlans.size()) {
			OnChain<BattlePlan> wrappedBattlePlan = new OnChain<>(message.height, message.transactionIndex, message.sendingAccount, message.receivingAccount, theBattlePlan);
			battlePlans.add(wrappedBattlePlan);
		}
	}
	
	private void disqualify(long accountId, int height) {
		if (height < disqualificationHeight && messagesByPlayer.keySet().contains(accountId)) {
			disqualified = accountId;
			disqualificationHeight = height;
		}
	}
	
	List<Long> accounts() {
		return messagesByPlayer.keySet().stream().collect(Collectors.toList());
	}
	
	int startHeight() {
		return messagesByPlayer.get(firstMover()).committedHashes.get(0).height;
	}
	
	BattleInProgressResult run(int currentHeight) {
		if (disqualified != 0) {
			long winner = opponentOf(disqualified);
			return new BattleInProgressResult(winner, disqualified, disqualificationHeight);
		}
		
		long firstMoverId = firstMover();
		long secondMoverId = opponentOf(firstMover());
		Map<Long, BattleMessages> validatedMessagesByPlayer = removeLateMessages(messagesByPlayer, firstMoverId, secondMoverId);
		BattleMessages firstMover = validatedMessagesByPlayer.get(firstMoverId);
		BattleMessages secondMover = validatedMessagesByPlayer.get(secondMoverId);
		int rounds = Math.min(firstMover.battlePlans.size(), secondMover.battlePlans.size());
		
		List<BattlePlan> battlePlans = new ArrayList<>();
		for (int i = 0; i < rounds; i++) {
			battlePlans.add(firstMover.battlePlans.get(i).data);
			battlePlans.add(secondMover.battlePlans.get(i).data);
		}
		
		int startOfBattle = firstMover.committedHashes.get(0).height;
		if (battlePlans.size() == 0 && currentHeight - startOfBattle < Constants.MAX_BLOCKS_PER_ROUND + Constants.MAX_BLOCKS_AFTER_HASHES_COMMITTED) {
			return BattleInProgressResult.ONGOING;
		} else if (firstMover.battlePlans.isEmpty() && secondMover.battlePlans.isEmpty()) {
			return BattleInProgressResult.ABORTED;
		} else if (firstMover.battlePlans.isEmpty() || secondMover.battlePlans.isEmpty()) {
			long loser = (firstMover.battlePlans.isEmpty()) ? firstMoverId : secondMoverId;
			long winner = opponentOf(loser);
			return new BattleInProgressResult(winner, loser, startOfBattle + Constants.MAX_BLOCKS_PER_ROUND);
		}
		
		BattleConfig battleConfig = new BattleConfig(battlePlans, 0);
		battleConfig.validate();
		BattleResult result = (new Battle(battleConfig)).getResult();
		
		if (result.winnerIndex < 0) {
			return determineResultOfIncompleteBattle(firstMover, secondMover, rounds, currentHeight);
		} else {
			return resultOfCompleteBattle(firstMover, secondMover, result);
		}
	}

	private long opponentOf(long player) {
		return messagesByPlayer.keySet().stream().filter(id -> id != player).findFirst().get();
	}
	
	private long firstMover() {
		BinaryOperator<OnChain<String>> earliestCommittedHash = (commitMessage1, commitMessage2) -> {
			if (commitMessage1.height == commitMessage2.height) {
				return (commitMessage1.transactionIndex < commitMessage2.transactionIndex) ? commitMessage1 : commitMessage2;
			} else {
				return (commitMessage1.height < commitMessage2.height) ? commitMessage1 : commitMessage2;
			}
		};
		
		return messagesByPlayer.values().stream()
				.filter(m -> m.committedHashes.size() > 0)
				.map(m -> m.committedHashes.get(0))
				.reduce(earliestCommittedHash)
				.get()
				.sendingAccount;
	}
	
	private static Map<Long, BattleMessages> removeLateMessages(Map<Long, BattleMessages> messages, long firstMoverId, long secondMoverId) {
		BattleMessages firstMover = messages.get(firstMoverId);
		BattleMessages secondMover = messages.get(secondMoverId);
		BattleMessages validatedFirstMover = new BattleMessages();
		BattleMessages validatedSecondMover = new BattleMessages();
		
		Map<Long, BattleMessages> validMessages = new HashMap<>();
		validMessages.put(firstMoverId, validatedFirstMover);
		validMessages.put(secondMoverId, validatedSecondMover);
		
		int rounds = Math.max(firstMover.committedHashes.size(), secondMover.committedHashes.size());
		int endOfPreviousRound = firstMover.committedHashes.get(0).height;
		
		for (int i = 0; i < rounds; i++) {
			OnChain<String> firstMoverHash = (firstMover.committedHashes.size() > i) ? firstMover.committedHashes.get(i) : null;
			OnChain<BattlePlan> firstMoverBp = (firstMover.battlePlans.size() > i) ? firstMover.battlePlans.get(i) : null;
			OnChain<String> secondMoverHash = (secondMover.committedHashes.size() > i) ? secondMover.committedHashes.get(i) : null;
			OnChain<BattlePlan> secondMoverBp = (secondMover.battlePlans.size() > i) ? secondMover.battlePlans.get(i) : null;
			
			int startOfThisRound = getStartingHeightOfRound(firstMoverHash, secondMoverHash);
			int lastHashOfThisRound = getLastHashHeightOfRound(firstMoverHash, secondMoverHash);
			
			if (startOfThisRound - endOfPreviousRound > Constants.MAX_BLOCKS_PER_ROUND) {
				return validMessages;	// both players timed out, so no subsequent messages are valid
			}
			
			boolean addedFirstMoverHash = addHashMessageIfCommittedInTime(firstMoverHash, validatedFirstMover, endOfPreviousRound);
			boolean addedSecondMoverHash = addHashMessageIfCommittedInTime(secondMoverHash, validatedSecondMover, endOfPreviousRound);
			boolean addedFirstMoverBp = addedFirstMoverHash && addBattlePlanMessageIfCommittedInTime(firstMoverBp, validatedFirstMover, lastHashOfThisRound);
			boolean addedSecondMoverBp = addedSecondMoverHash && addBattlePlanMessageIfCommittedInTime(secondMoverBp, validatedSecondMover, lastHashOfThisRound);
			
			if (!addedFirstMoverBp || !addedSecondMoverBp) {
				return validMessages;	// If either battle plan is null, we return here...
			}
			
			// ...so we know down here that both are non-null.
			endOfPreviousRound = Math.max(firstMoverBp.height, secondMoverBp.height);
		}
		
		return validMessages;
	}
	
	private static int getStartingHeightOfRound(OnChain<String> firstMoverHash, OnChain<String> secondMoverHash) {
		assert(firstMoverHash != null || secondMoverHash != null);
		int firstMoverHashHeight = (firstMoverHash == null) ? Integer.MAX_VALUE : firstMoverHash.height;
		int secondMoverHashHeight = (secondMoverHash == null) ? Integer.MAX_VALUE : secondMoverHash.height;
		return Math.min(firstMoverHashHeight, secondMoverHashHeight);
	}
	
	private static int getLastHashHeightOfRound(OnChain<String> firstMoverHash, OnChain<String> secondMoverHash) {
		assert(firstMoverHash != null || secondMoverHash != null);
		int firstMoverHashHeight = (firstMoverHash == null) ? Integer.MIN_VALUE : firstMoverHash.height;
		int secondMoverHashHeight = (secondMoverHash == null) ? Integer.MIN_VALUE : secondMoverHash.height;
		return Math.max(firstMoverHashHeight, secondMoverHashHeight);
	}
	
	private static boolean addHashMessageIfCommittedInTime(OnChain<String> message, BattleMessages validatedMessages, int endOfPreviousRound) {
		if (message != null && message.height - endOfPreviousRound <= Constants.MAX_BLOCKS_PER_ROUND) {
			validatedMessages.committedHashes.add(message);
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean addBattlePlanMessageIfCommittedInTime(OnChain<BattlePlan> message, BattleMessages validatedMessages, int secondHashHeight) {
		if (message != null && message.height - secondHashHeight <= Constants.MAX_BLOCKS_AFTER_HASHES_COMMITTED) {
			validatedMessages.battlePlans.add(message);
			return true;
		} else {
			return false;
		}
	}
	
	private static BattleInProgressResult determineResultOfIncompleteBattle(BattleMessages firstMover, BattleMessages secondMover, int rounds, int currentHeight) {
		int endOfMostRecentRound = Math.max(firstMover.battlePlans.get(rounds - 1).height, secondMover.battlePlans.get(rounds - 1).height);
		if (currentHeight - endOfMostRecentRound < Constants.MAX_BLOCKS_PER_ROUND + Constants.MAX_BLOCKS_AFTER_HASHES_COMMITTED) {
			return BattleInProgressResult.ONGOING;
		}
		
		long firstMoverId = firstMover.committedHashes.get(0).sendingAccount;
		long secondMoverId = secondMover.committedHashes.get(0).sendingAccount;
		
		boolean firstMoverCommittedHashForNextRound = firstMover.committedHashes.size() >= rounds + 1;
		boolean secondMoverCommittedHashForNextRound = secondMover.committedHashes.size() >= rounds + 1;
		
		if (firstMoverCommittedHashForNextRound && secondMoverCommittedHashForNextRound) {
			long winner = (firstMover.battlePlans.size() > secondMover.battlePlans.size()) ? firstMoverId : secondMoverId;
			long loser = (winner == firstMoverId) ? secondMoverId : firstMoverId;
			int lastHashHeightOfNextRound = getLastHashHeightOfRound(firstMover.committedHashes.get(rounds), secondMover.committedHashes.get(rounds));
			return new BattleInProgressResult(winner, loser, lastHashHeightOfNextRound + Constants.MAX_BLOCKS_AFTER_HASHES_COMMITTED);
		} else if (firstMoverCommittedHashForNextRound || secondMoverCommittedHashForNextRound) {
			long winner = firstMoverCommittedHashForNextRound ? firstMoverId : secondMoverId;
			long loser = firstMoverCommittedHashForNextRound ? secondMoverId : firstMoverId;
			return new BattleInProgressResult(winner, loser, endOfMostRecentRound + Constants.MAX_BLOCKS_PER_ROUND);
		} else {
			return BattleInProgressResult.ABORTED;
		}
	}
	
	private static BattleInProgressResult resultOfCompleteBattle(BattleMessages firstMover, BattleMessages secondMover, BattleResult battleResult) {
		long firstMoverId = firstMover.committedHashes.get(0).sendingAccount;
		long secondMoverId = secondMover.committedHashes.get(0).sendingAccount;
		long winner = (battleResult.winnerIndex == 0) ? firstMoverId : secondMoverId;
		long loser = (battleResult.winnerIndex == 0) ? secondMoverId : firstMoverId;
		int lastRound = battleResult.eventsByRound.size() - 1;
		int finishHeight = Math.max(firstMover.battlePlans.get(lastRound).height, secondMover.battlePlans.get(lastRound).height);
		return new BattleInProgressResult(winner, loser, finishHeight);
	}
	
}
