package com.snailscuffle.common;

import static org.junit.Assert.*;

import org.junit.Test;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.InvalidBattleException;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Weapon;

public class BattlePlanTest {
	
	@Test
	public void validateStripsShellFromDoug() {
		BattlePlan bp = defaultBattlePlan();
		bp.snail = Snail.DOUG;
		bp.shell = Shell.ALUMINUM;
		bp.validate();
		assertEquals(null, bp.shell);
	}
	
	@Test
	public void failValidationOnNullSnail() {
		BattlePlan bp = defaultBattlePlan();
		bp.snail = null;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	@Test
	public void failValidationOnNullWeapon() {
		BattlePlan bp = defaultBattlePlan();
		bp.weapon = null;
		assertValidateThrowsInvalidBattleException(bp);
	}
	
	private static BattlePlan defaultBattlePlan() {
		BattlePlan bp = new BattlePlan();
		bp.snail = Snail.DALE;
		bp.weapon = Weapon.RIFLE;
		bp.shell = Shell.ALUMINUM;
		bp.accessory = Accessory.STEROIDS;
		bp.item1 = Item.ATTACK;
		bp.item2 = Item.DEFENSE;
		return bp;
	}
	
	private static void assertValidateThrowsInvalidBattleException(BattlePlan bp) {
		try {
			bp.validate();
		} catch (InvalidBattleException e) {
			return;
		}
		fail("Expected InvalidBattleException");
	}

}
