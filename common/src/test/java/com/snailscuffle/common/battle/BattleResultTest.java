package com.snailscuffle.common.battle;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class BattleResultTest {

	private BattleResult result;
	
	@Before
	public void setUp() {
		BattleEventEffect effect = new BattleEventEffect();
		effect.playerIndex = 0;
		effect.stat = Stat.HP;
		effect.change = -20;
		
		BattleEvent event = new BattleEvent();
		event.time = 1;
		event.playerIndex = 1;
		event.action = Action.ATTACK;
		event.effects = new ArrayList<>();
		event.effects.add(effect);
		
		result = new BattleResult();
		result.sequenceOfEvents = new ArrayList<>();
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
