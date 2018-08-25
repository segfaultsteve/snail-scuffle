package com.snailscuffle.game.battle;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.Action;
import com.snailscuffle.common.battle.BattleConfig;
import com.snailscuffle.common.battle.BattleEvent;
import com.snailscuffle.common.battle.BattleEventEffect;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.BattleResult;
import com.snailscuffle.common.battle.Instruction;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Stat;
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
	public void attackBoostWorks() {
		assertBoostWorks(Item.ATTACK, Stat.ATTACK);
	}
	
	@Test
	public void defenseBoostWorks() {
		assertBoostWorks(Item.DEFENSE, Stat.DEFENSE);
	}
	
	@Test
	public void speedBoostWorks() {
		assertBoostWorks(Item.SPEED, Stat.AP);
	}
	
	@Test
	public void player1WinsWhenBattlePlansAreIdentical() {
		BattleConfig config = new BattleConfig(bp, bp, bp, bp, bp, bp);
		BattleResult result = (new Battle(config)).getResult();
		assertEquals(0, result.winnerIndex);
	}
	
	@Test
	public void steroidsSwingBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.STEROIDS;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void snailMailSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.SNAIL_MAIL;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void caffeineSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.CAFFEINE;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void chargedAttackSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.CHARGED_ATTACK;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void adrenalineSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.ADRENALINE;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void thornsSwingBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.THORNS;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void defibrillatorSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.accessory = Accessory.DEFIBRILLATOR;
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void attackBoostSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.item1 = Item.ATTACK;
		bp2.instructions = Arrays.asList(Instruction.waitUntilApIs(30), Instruction.useItem(Item.ATTACK));
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp, bp, bp);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void defenseBoostSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.item1 = Item.DEFENSE;
		bp2.instructions = Arrays.asList(Instruction.waitUntilApIs(15), Instruction.useItem(Item.DEFENSE));
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp, bp, bp);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void speedBoostSwingsBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.item1 = Item.SPEED;
		bp2.instructions = Arrays.asList(Instruction.useItem(Item.SPEED));
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp, bp, bp);
		
		BattleResult result = (new Battle(config)).getResult();
		
		assertEquals(1, result.winnerIndex);
	}
	
	@Test
	public void playerCanOnlyChangeOnePieceOfEquipmentPerPeriod() {
		bp.weapon = Weapon.ROCKET;
		bp.shell = Shell.ALUMINUM;
		
		// change weapon *and* shell
		BattlePlan bp2 = clone(bp);
		bp2.weapon = Weapon.LASER;
		bp2.shell = Shell.STEEL;
		
		BattleConfig config = new BattleConfig(bp, bp, bp2, bp, bp, bp);	// player 1 makes two changes at second period
		BattleResult result = (new Battle(config)).getResult();
		
		// shell should not have changed, so damage should be the same across all periods
		double player1InitialDamage = 0;
		for (BattleEvent event : result.flattenEvents()) {
			BattleEventEffect effect = event.effects.get(0);
			if (effect.playerIndex == 0 && effect.stat == Stat.HP) {
				if (player1InitialDamage == 0) {
					player1InitialDamage = effect.change;
				} else {
					assertEquals(player1InitialDamage, effect.change, 0.001);
				}
			}
		}
	}
	
	@Test
	public void playerCanUseAtMostTwoItemsPerBattle() {
		BattlePlan bp2 = clone(bp);
		bp2.item1 = Item.ATTACK;
		bp2.item2 = Item.ATTACK;
		bp2.instructions = Arrays.asList(Instruction.useItem(Item.ATTACK), Instruction.useItem(Item.ATTACK));
		BattleConfig config = new BattleConfig(bp, bp2, bp, bp2, bp, bp2);
		
		BattleResult result = (new Battle(config)).getResult();
		
		int itemsUsed = 0;
		for (BattleEvent event : result.flattenEvents()) {
			if (event.playerIndex == 1 && event.itemUsed == Item.ATTACK) {
				itemsUsed++;
			}
		}
		assertEquals(Combatant.MAX_ITEMS_PER_BATTLE, itemsUsed);
	}
	
	private void assertBoostWorks(Item boostToUse, Stat statWhichShouldIncrease) {
		BattlePlan bp2 = clone(bp);
		bp2.item1 = boostToUse;
		bp2.instructions = Arrays.asList(Instruction.useItem(boostToUse));
		BattleConfig config = new BattleConfig(bp2, bp, bp, bp, bp, bp);
		
		BattleEvent firstEvent = (new Battle(config)).getResult().flattenEvents().get(0);
		BattleEventEffect firstEventEffect = firstEvent.effects.get(0);
		
		assertEquals(firstEvent.action, Action.USE_ITEM);
		assertEquals(firstEvent.itemUsed, boostToUse);
		assertEquals(firstEventEffect.stat, statWhichShouldIncrease);
		assertTrue(firstEventEffect.change > 0);
	}
	
	private static BattlePlan clone(BattlePlan bp) {
		BattlePlan clone = new BattlePlan(bp);
		clone.validate();
		return clone;
	}

}
