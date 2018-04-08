package com.snailscuffle.common.battle;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class BattleResultTest {

	private BattleResult result;
	
	@Before
	public void setUp() {
		BattleEvent event = BattleEvent.newAttackEvent(1, 0, 20);
		result = new BattleResult(new ArrayList<>(), 0);
		result.sequenceOfEvents.add(event);
	}
	
	@Test
	public void failValidationOnNullSequenceOfEvents() {
		result.sequenceOfEvents = null;
		assertValidateThrowsInvalidBattleException(result);
	}
	
	@Test
	public void failValidationOnEmptySequenceOfEvents() {
		result.sequenceOfEvents.clear();
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
		result.sequenceOfEvents.get(0).action = null;
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
