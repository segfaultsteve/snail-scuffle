package com.snailscuffle.game.battle;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattleEvent;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Constants;
import com.snailscuffle.common.battle.Instruction;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Weapon;

public class CombatantTest {
	
	private static final int TIMING_TOLERANCE = 1;			// Combatant.ticksToNextAp() rounds up, so allow a slight tolerance
	private static final double REAL_TOLERANCE = 0.001;		// tolerance for real-valued arithmetic
	
	private BattlePlan bp;
	private Integer battleTime = new Integer(0);
	private BattleRecorder recorder;
	private Combatant player1;
	private Combatant player2;
	
	@Before
	public void setUp() throws Exception {
		bp = defaultBattlePlan();
		Battle battle = mock(Battle.class);
		recorder = new BattleRecorder(battle);
		player1 = new Combatant(recorder);
		player2 = new Combatant(recorder);
		
		when(battle.currentTime()).thenAnswer(i -> battleTime.intValue());
		when(battle.playerIndexOf(player1)).thenReturn(0);
		when(battle.playerIndexOf(player2)).thenReturn(1);
		
		player1.setOpponent(player2);
		player2.setOpponent(player1);
	}
	
	private static BattlePlan defaultBattlePlan() {
		BattlePlan battlePlan = new BattlePlan();
		battlePlan.snail = Snail.DALE;
		battlePlan.weapon = Weapon.ROCKET;
		battlePlan.shell = Shell.ALUMINUM;
		battlePlan.validate();
		return battlePlan;
	}
	
	@Test
	public void attackForCorrectDamage() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		
		BattleEvent firstEvent = runBattleUntilNextEvent();
		
		double expectedDamage = Combatant.DAMAGE_MULTIPLIER * attackOf(bp) / defenseOf(bp);
		double actualDamage = -firstEvent.effects.get(0).change;
		assertEquals(expectedDamage, actualDamage, REAL_TOLERANCE);
	}
	
	@Test
	public void attackAtCorrectTime() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		
		BattleEvent firstEvent = runBattleUntilNextEvent();
		
		int expectedTicks = Combatant.SCALE * bp.weapon.apCost / speedOf(bp) + 1;	// +1 to round up after integer division
		int actualTicks = firstEvent.time;
		assertTrue(Math.abs(expectedTicks - actualTicks) <= TIMING_TOLERANCE);
	}
	
	@Test
	public void waitUntilCorrectTime() {
		final int WAIT_AP = 2 * bp.weapon.apCost;
		bp.instructions = Arrays.asList(Instruction.waitUntilApIs(WAIT_AP));
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		
		BattleEvent firstEvent = runBattleUntilNextEvent();
		
		int expectedTicks = Combatant.SCALE * WAIT_AP / speedOf(bp) + 1;	// +1 to round up after integer division
		int actualTicks = firstEvent.time;
		assertTrue(Math.abs(expectedTicks - actualTicks) <= TIMING_TOLERANCE);
	}
	
	@Test
	public void steroidsIncreaseAttack() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double initialAttack = attackOf(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.STEROIDS;
		player1.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedIncrease = (initialAttack + Constants.STEROIDS_ATTACK) / initialAttack;
		double measuredIncrease = damageWith / damageWithout;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void snailMailIncreasesDefense() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double initialDefense = defenseOf(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.SNAIL_MAIL;
		player2.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedIncrease = (initialDefense + Constants.SNAIL_MAIL_DEFENSE) / initialDefense;
		double measuredIncrease = damageWithout / damageWith;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void caffeineIncreasesSpeed() {
		player1.setBattlePlan(bp);
		double initialSpeed = speedOf(bp);
		double ticksBefore = player1.ticksToNextAp();
		
		bp.accessory = Accessory.CAFFEINE;
		player1.setBattlePlan(bp);
		double ticksAfter = player1.ticksToNextAp();
		
		double expectedIncrease = (initialSpeed + Constants.CAFFEINE_SPEED ) / initialSpeed;
		double measuredIncrease = ticksBefore / ticksAfter;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	// NOTE: If multiple events fall on the same tick (e.g., players with identical battle
	// plans attack, or a player uses an item and attacks), then calling this method will
	// trigger *all* of those events. The next call to this function will return the event(s)
	// at the next recorded time.
	private BattleEvent runBattleUntilNextEvent() {
		int initialEventCount = recorder.battleEvents().size();
		
		while(recorder.battleEvents().size() == initialEventCount) {
			int p1Ticks = player1.ticksToNextAp();
			int p2Ticks = player2.ticksToNextAp();
			int increment = Math.min(p1Ticks, p2Ticks);
			
			battleTime += increment;
			player1.update(increment);
			player2.update(increment);
		}
		
		return recorder.battleEvents().get(initialEventCount);
	}
	
	private static double attackOf(BattlePlan battlePlan) {
		double attack = (battlePlan.snail.attack
				+ battlePlan.weapon.attack
				+ battlePlan.shell.attack
				+ battlePlan.accessory.attack);
		return attack;
	}
	
	private static double defenseOf(BattlePlan battlePlan) {
		double defense = (battlePlan.snail.defense
				+ battlePlan.weapon.defense
				+ battlePlan.shell.defense
				+ battlePlan.accessory.defense);
		return defense;
	}
	
	private static int speedOf(BattlePlan battlePlan) {
		return (battlePlan.snail.speed
				+ battlePlan.weapon.speed
				+ battlePlan.shell.speed
				+ battlePlan.accessory.speed);
	}

}
