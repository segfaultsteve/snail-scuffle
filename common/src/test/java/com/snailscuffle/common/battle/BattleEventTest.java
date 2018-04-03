package com.snailscuffle.common.battle;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class BattleEventTest {

	private BattleEvent event;
	
	@Before
	public void setUp() {
		BattleEventEffect effect = new BattleEventEffect();
		effect.playerIndex = 0;
		effect.stat = Stat.HP;
		effect.change = -20;
		
		event = new BattleEvent();
		event.time = 1;
		event.playerIndex = 1;
		event.action = Action.ATTACK;
		event.effects = new ArrayList<>();
		event.effects.add(effect);
	}
	
	@Test
	public void failValidationOnNegativeTimestamp() {
		event.time = -1;
		assertValidateThrowsInvalidBattleException(event);
	}
	
	@Test
	public void failValidationOnInvalidPlayerIndex() {
		event.playerIndex = -1;
		assertValidateThrowsInvalidBattleException(event);
		event.playerIndex = 2;
		assertValidateThrowsInvalidBattleException(event);
	}
	
	@Test
	public void failValidationOnNullAction() {
		event.action = null;
		assertValidateThrowsInvalidBattleException(event);
	}
	
	@Test
	public void failValidationOnMissingItem() {
		event.action = Action.USE_ITEM;
		event.itemUsed = null;
		assertValidateThrowsInvalidBattleException(event);
	}
	
	@Test
	public void failValidationOnNullEffects() {
		event.effects = null;
		assertValidateThrowsInvalidBattleException(event);
	}
	
	@Test
	public void failValidationOnEmptyEffects() {
		event.effects.clear();
		assertValidateThrowsInvalidBattleException(event);
	}
	
	@Test
	public void failValidationOnBattleEffectWithInvalidPlayerIndex() {
		BattleEventEffect effect = event.effects.get(0);
		effect.playerIndex = -1;
		assertValidateThrowsInvalidBattleException(event);
		effect.playerIndex = 2;
		assertValidateThrowsInvalidBattleException(event);
	}
	
	@Test
	public void failValidationOnBattleEffectWithNullStat() {
		BattleEventEffect effect = event.effects.get(0);
		effect.stat = null;
		assertValidateThrowsInvalidBattleException(event);
	}
	
	@Test
	public void failValidationOnBattleEffectWithZeroChange() {
		BattleEventEffect effect = event.effects.get(0);
		effect.change = 0;
		assertValidateThrowsInvalidBattleException(event);
	}
	
	private static void assertValidateThrowsInvalidBattleException(BattleEvent battleEvent) {
		try {
			battleEvent.validate();
		} catch (InvalidBattleException e) {
			return;
		}
		fail("Expected InvalidBattleException");
	}

}
