package com.snailscuffle.common;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.common.battle.BattleConfig;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.InvalidBattleException;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Weapon;

public class BattleConfigTest {

	private BattleConfig config;
	
	@Before
	public void setUp() {
		BattlePlan bp = new BattlePlan();
		bp.snail = Snail.DALE;
		bp.weapon = Weapon.RIFLE;
		config = new BattleConfig(bp, bp);
	}
	
	@Test
	public void failValidationOnNullBattlePlans() {
		config.battlePlans = null;
		assertValidateThrowsInvalidBattleException(config);
	}
	
	@Test
	public void failValidationOnEmptyBattlePlans() {
		config.battlePlans.clear();
		assertValidateThrowsInvalidBattleException(config);
	}
	
	@Test
	public void failValidationOnSingleBattlePlan() {
		config.battlePlans.remove(1);
		assertValidateThrowsInvalidBattleException(config);
	}
	
	@Test
	public void failValidationOnInvalidBattlePlan() {
		config.battlePlans.get(0).snail = null;
		assertValidateThrowsInvalidBattleException(config);
	}
	
	private static void assertValidateThrowsInvalidBattleException(BattleConfig battleConfig) {
		try {
			battleConfig.validate();
		} catch (InvalidBattleException e) {
			return;
		}
		fail("Expected InvalidBattleException");
	}

}
