package com.snailscuffle.common;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Inequality;
import com.snailscuffle.common.battle.InvalidBattleException;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.ItemRule;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Stat;
import com.snailscuffle.common.battle.Weapon;

public class BattlePlanTest {
	
	private BattlePlan bp;
	
	@Before
	public void setUp() {
		bp = new BattlePlan();
		bp.snail = Snail.DALE;
		bp.weapon = Weapon.RIFLE;
		bp.shell = Shell.ALUMINUM;
		bp.accessory = Accessory.STEROIDS;
		bp.item1 = Item.ATTACK;
		bp.item2 = Item.DEFENSE;
	}
	
	@Test
	public void validateStripsShellFromDoug() {
		bp.snail = Snail.DOUG;
		bp.shell = Shell.ALUMINUM;
		bp.validate();
		assertEquals(null, bp.shell);
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
	public void failValidationOnEmptyItem1Rule() {
		bp.item1Rule = ItemRule.useWhenEnemyUses(null);
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnEmptyItem2Rule() {
		bp.item2Rule = ItemRule.useWhenEnemyUses(null);
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnOverspecifiedItem1Rule() {
		bp.item1Rule = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 10);
		bp.item1Rule.enemyUsesCondition = Item.ATTACK;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnOverspecifiedItem2Rule() {
		bp.item2Rule = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 10);
		bp.item2Rule.enemyUsesCondition = Item.ATTACK;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnItem1RuleWithInvalidHasCondition() {
		bp.item1Rule = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 10);
		bp.item1Rule.hasCondition.stat = null;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnItem2RuleWithInvalidHasCondition() {
		bp.item2Rule = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 10);
		bp.item2Rule.hasCondition.stat = null;
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
