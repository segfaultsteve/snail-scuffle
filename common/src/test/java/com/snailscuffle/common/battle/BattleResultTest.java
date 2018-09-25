package com.snailscuffle.common.battle;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class BattleResultTest {

	private BattleResult result;
	
	@Before
	public void setUp() {
		List<BattleEvent> round1 = new ArrayList<>();
		round1.add(BattleEvent.attack(1, 0, 20));
		
		List<List<BattleEvent>> eventsByRound = new ArrayList<>();
		eventsByRound.add(round1);
		
		List<BattleSnapshot> endOfRoundStats = new ArrayList<>();
		endOfRoundStats.add(new BattleSnapshot(1, 1, 1, 1, 1));
		
		result = new BattleResult(eventsByRound, endOfRoundStats, 0);
	}
	
	@Test
	public void failValidationOnNullEventsByRound() {
		result.eventsByRound = null;
		assertValidateThrowsInvalidBattleException(result);
	}
	
	@Test
	public void failValidationOnEmptyEventsByRound() {
		result.eventsByRound.clear();
		assertValidateThrowsInvalidBattleException(result);
	}
	
	@Test
	public void failValidationOnNullEndOfRoundStats() {
		result.endOfRoundStats = null;
		assertValidateThrowsInvalidBattleException(result);
	}
	
	@Test
	public void failValidationOnEmptyEndOfRoundStats() {
		result.endOfRoundStats.clear();
		assertValidateThrowsInvalidBattleException(result);
	}
	
	@Test
	public void failValidationOnInconsistentNumberOfRounds() {
		result.endOfRoundStats.add(new BattleSnapshot(1, 1, 1, 1, 1));
		assertValidateThrowsInvalidBattleException(result);
	}
	
	@Test
	public void failValidationOnInvalidWinnerIndex() {
		result.winnerIndex = -1;
		assertValidateThrowsInvalidBattleException(result);
		result.winnerIndex = 2;
		assertValidateThrowsInvalidBattleException(result);
	}
	
	@Test
	public void failValidationOnInvalidBattleEvent() {
		result.eventsByRound.get(0).get(0).action = null;
		assertValidateThrowsInvalidBattleException(result);
	}
	
	private static void assertValidateThrowsInvalidBattleException(BattleResult battleResult) {
		try {
			battleResult.validate();
		} catch (InvalidBattleException e) {
			return;
		}
		fail("Expected InvalidBattleException");
	}

}
