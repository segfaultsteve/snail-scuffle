package com.snailscuffle.game.battle;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattleConfig;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.BattleResult;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Weapon;

public class BattleTest {
	
	private BattlePlan bp;
	
	@Before
	public void setUp() {
		bp = new BattlePlan();
		bp.snail = Snail.DALE;
		bp.weapon = Weapon.ROCKET;
		bp.shell = Shell.ALUMINUM;
		bp.validate();
	}
	
	@Test
	public void player1WinsWhenBattlePlansAreIdentical() {
		BattleConfig config = new BattleConfig(bp, bp, bp, bp);
		BattleResult result = (new Battle(config)).getResult();
		assertEquals(0, result.winnerIndex);
	}
	
	@Test
	public void steroidsSwingBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.STEROIDS;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void snailMailSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.SNAIL_MAIL;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void caffeineSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.CAFFEINE;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void chargedAttackSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.CHARGED_ATTACK;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void adrenalineSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.ADRENALINE;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void thornsSwingBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.THORNS;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void defibrillatorSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.DEFIBRILLATOR;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	private static BattlePlan clone(BattlePlan bp) {
		BattlePlan clone = new BattlePlan(bp);
		clone.validate();
		return clone;
	}

}
