package com.snailscuffle.common.battle;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class BattlePlanTest {
	
	private BattlePlan bp;
	
	@Before
	public void setUp() {
		bp = new BattlePlan();
		bp.snail = Snail.DALE;
		bp.weapon = Weapon.RIFLE;
		bp.shell = Shell.ALUMINUM;
		bp.accessory = Accessory.STEROIDS;
		bp.items[0] = Item.ATTACK;
		bp.items[1] = Item.DEFENSE;
	}
	
	@Test
	public void validateStripsShellFromDoug() {
		bp.snail = Snail.DOUG;
		bp.shell = Shell.ALUMINUM;
		bp.validate();
		assertEquals(Shell.NONE, bp.shell);
	}
	
	@Test
	public void failValidationOnNullSnail() {
		bp.snail = null;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnNullWeapon() {
		bp.weapon = null;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnEmptyItem0Rule() {
		bp.itemRules[0] = ItemRule.useWhenEnemyUses(null);
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnEmptyItem1Rule() {
		bp.itemRules[1] = ItemRule.useWhenEnemyUses(null);
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnOverspecifiedItem0Rule() {
		bp.itemRules[0] = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 10);
		bp.itemRules[0].enemyUsesCondition = Item.ATTACK;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnOverspecifiedItem1Rule() {
		bp.itemRules[1] = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 10);
		bp.itemRules[1].enemyUsesCondition = Item.ATTACK;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnItem0RuleWithInvalidHasCondition() {
		bp.itemRules[0] = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 10);
		bp.itemRules[0].hasCondition.stat = null;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnItem1RuleWithInvalidHasCondition() {
		bp.itemRules[1] = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 10);
		bp.itemRules[1].hasCondition.stat = null;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	private static void assertValidateThrowsInvalidBattleException(BattlePlan battlePlan) {
		try {
			battlePlan.validate();
		} catch (InvalidBattleException e) {
			return;
		}
		fail("Expected InvalidBattleException");
	}

}
