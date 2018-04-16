package com.snailscuffle.game.battle;

import static com.snailscuffle.common.battle.Constants.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.Action;
import com.snailscuffle.common.battle.BattleEvent;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Instruction;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Stat;
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
		
		double expectedDamage = Combatant.DAMAGE_MULTIPLIER * baseAttackOf(bp) / baseDefenseOf(bp);
		double actualDamage = -firstEvent.effects.get(0).change;
		assertEquals(expectedDamage, actualDamage, REAL_TOLERANCE);
	}
	
	@Test
	public void attackAtCorrectTime() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		
		BattleEvent firstEvent = runBattleUntilNextEvent();
		
		int expectedTicks = Combatant.SCALE * bp.weapon.apCost / baseSpeedOf(bp) + 1;	// +1 to round up after integer division
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
		
		int expectedTicks = Combatant.SCALE * WAIT_AP / baseSpeedOf(bp) + 1;	// +1 to round up after integer division
		int actualTicks = firstEvent.time;
		assertTrue(Math.abs(expectedTicks - actualTicks) <= TIMING_TOLERANCE);
	}
	
	@Test
	public void steroidsIncreaseAttack() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double initialAttack = baseAttackOf(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.STEROIDS;
		player1.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedIncrease = (initialAttack + STEROIDS_ATTACK) / initialAttack;
		double measuredIncrease = damageWith / damageWithout;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void snailMailIncreasesDefense() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double initialDefense = baseDefenseOf(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.SNAIL_MAIL;
		player2.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedIncrease = (initialDefense + SNAIL_MAIL_DEFENSE) / initialDefense;
		double measuredIncrease = damageWithout / damageWith;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void caffeineIncreasesSpeed() {
		player1.setBattlePlan(bp);
		double initialSpeed = baseSpeedOf(bp);
		double ticksBefore = player1.ticksToNextAp();
		
		bp.accessory = Accessory.CAFFEINE;
		player1.setBattlePlan(bp);
		double ticksAfter = player1.ticksToNextAp();
		
		double expectedIncrease = (initialSpeed + CAFFEINE_SPEED ) / initialSpeed;
		double measuredIncrease = ticksBefore / ticksAfter;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void chargedAttackIncreasesAttack() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.CHARGED_ATTACK;
		player1.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedIncrease = 1 + 1.0 * bp.weapon.apCost / CHARGED_ATTACK_AP_DIVISOR;
		double measuredIncrease = damageWith / damageWithout;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void adrenalineDecreasesAttackAtHighHp() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double initialAttack = baseAttackOf(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		double hpAfterFirstHit = 100 - damageWithout;
		bp.accessory = Accessory.ADRENALINE;
		player1.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedChange = (initialAttack + (ADRENALINE_CROSSOVER - hpAfterFirstHit) / ADRENALINE_DIVISOR) / initialAttack;
		double measuredChange = damageWith / damageWithout;
		assertEquals(expectedChange, measuredChange, REAL_TOLERANCE);
		assertTrue(measuredChange < 1);
	}
	
	@Test
	public void adrenalineIncreasesAttackAtLowHp() {
		final int HIT_COUNT = 5;
		
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double initialAttack = baseAttackOf(bp);
		
		for (int i = 0; i < HIT_COUNT; i++) {
			runBattleUntilNextEvent();
		}
		double damageWithout = -recorder.battleEvents().get(0).effects.get(0).change;
		double hpRemaining = 100 - HIT_COUNT * damageWithout;
		
		bp.accessory = Accessory.ADRENALINE;
		player1.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedChange = (initialAttack + (ADRENALINE_CROSSOVER - hpRemaining) / ADRENALINE_DIVISOR) / initialAttack;
		double measuredChange = damageWith / damageWithout;
		assertEquals(expectedChange, measuredChange, REAL_TOLERANCE);
		assertTrue(measuredChange > 1);
	}
	
	// NOTE: To understand the tests related to the salted shell, it is important to count
	// how many times Combatant.setBattlePlan() is called. The first call to this method
	// sets the player's battle plan for the first period, the second call sets it for the
	// second period, and so on. In the first period, the salted shell should reduce the
	// player's defense by SALTED_SHELL_DEFENSE_MULTIPLIER, while in later periods it should
	// increase the player's attack by SALTED_SHELL_ATTACK_MULTIPLIER (as long as the player
	// plays it in consecutive periods).
	@Test
	public void saltedShellReducesDefenseInFirstPeriod() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.SALTED_SHELL;
		player2.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedChange = 1 / SALTED_SHELL_DEFENSE_MULTIPLIER;
		double measuredChange = damageWith / damageWithout;
		assertEquals(expectedChange, measuredChange, REAL_TOLERANCE);
	}
	
	@Test
	public void saltedShellIncreasesAttackInLaterPeriods() {
		final int BATTLE_DURATION_IN_PERIODS = 4;
		
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		
		bp.accessory = Accessory.SALTED_SHELL;
		player1.setBattlePlan(bp);		// first period
		
		// subsequent periods
		for (int i = 1; i < BATTLE_DURATION_IN_PERIODS; i++) {
			player1.setBattlePlan(bp); 		// next period; keep salted shell equipped
			double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
			double measuredChange = damageWith / damageWithout;
			assertEquals(SALTED_SHELL_ATTACK_MULTIPLIER, measuredChange, REAL_TOLERANCE);
		}
	}
	
	@Test
	public void unequippingSaltedShellResetsTheEffect() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double initialDamage = -runBattleUntilNextEvent().effects.get(0).change;
		
		// first period - equip it
		bp.accessory = Accessory.SALTED_SHELL;
		player2.setBattlePlan(bp);
		
		// second period - unequip it
		bp.accessory = Accessory.NONE;
		player2.setBattlePlan(bp);
		double damageAfterUnequipping = -runBattleUntilNextEvent().effects.get(0).change;
		
		// third period - re-equip it
		bp.accessory = Accessory.SALTED_SHELL;
		player2.setBattlePlan(bp);
		double damageAfterReEquipping = -runBattleUntilNextEvent().effects.get(0).change;
		
		// expect defense decrease instead of attack increase, since player did not hold
		// the salted shell for consecutive periods
		double expectedChange = 1 / SALTED_SHELL_DEFENSE_MULTIPLIER;
		double measuredChange = damageAfterReEquipping / initialDamage;
		assertEquals(expectedChange, measuredChange, REAL_TOLERANCE);
		
		// make sure it was actually unequipped
		assertEquals(initialDamage, damageAfterUnequipping, REAL_TOLERANCE);
	}
	
	@Test
	public void thornsDamageAttacker() {
		player1.setBattlePlan(bp);
		bp.accessory = Accessory.THORNS;
		player2.setBattlePlan(bp);
		
		runBattleUntilNextEvent();
		double damageDone = -recorder.battleEvents().get(0).effects.get(0).change;
		double damageTaken = -recorder.battleEvents().get(0).effects.get(1).change;
		int playerTakingDamage = recorder.battleEvents().get(0).effects.get(1).playerIndex;
		
		double expectedDamageTaken = damageDone * THORNS_DAMAGE_MULTIPLIER;
		assertEquals(expectedDamageTaken, damageTaken, REAL_TOLERANCE);
		assertEquals(0, playerTakingDamage);
	}
	
	@Test
	public void defibrillatorAllowsPlayerToTakeOneExtraHit() {
		player1.setBattlePlan(bp);
		bp.accessory = Accessory.DEFIBRILLATOR;
		player2.setBattlePlan(bp);
		
		int hits = 0;
		while (player2.isAlive()) {
			runBattleUntilNextEvent();
			hits++;
		}
		double damageEachHit = -recorder.battleEvents().get(0).effects.get(0).change;
		
		int hitsToKillPlayer2WithoutDefibrillator = (int) Math.ceil(100 / damageEachHit);
		assertEquals(hitsToKillPlayer2WithoutDefibrillator + 1, hits);	// +1 for defibrillator
	}
	
	@Test
	public void attackBoostIncreasesAttack() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double initialAttack = baseAttackOf(bp);
		double damageBefore = -runBattleUntilNextEvent().effects.get(0).change;
		
		bp.item1 = Item.ATTACK;
		bp.instructions = Arrays.asList(Instruction.useItem(Item.ATTACK));
		player1.setBattlePlan(bp);
		double reportedAttackBoost = runBattleUntilNextEvent().effects.get(0).change;
		double damageAfter = -runBattleUntilNextEvent().effects.get(0).change;
		
		double measuredIncrease = damageAfter / damageBefore;
		double reportedIncrease = (initialAttack + reportedAttackBoost) / initialAttack;
		assertEquals(ATTACK_BOOST_MULTIPLIER, measuredIncrease, REAL_TOLERANCE);
		assertEquals(reportedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void defenseBoostIncreasesDefense() {
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		double initialDefense = baseDefenseOf(bp);
		double damageBefore = -runBattleUntilNextEvent().effects.get(0).change;
		
		bp.item1 = Item.DEFENSE;
		bp.instructions = Arrays.asList(Instruction.useItem(Item.DEFENSE));
		player2.setBattlePlan(bp);
		double reportedDefenseBoost = runBattleUntilNextEvent().effects.get(0).change;
		double damageAfter = -runBattleUntilNextEvent().effects.get(0).change;
		
		double measuredIncrease = damageBefore / damageAfter;
		double reportedIncrease = (initialDefense + reportedDefenseBoost) / initialDefense;
		assertEquals(DEFENSE_BOOST_MULTIPLIER, measuredIncrease, REAL_TOLERANCE);
		assertEquals(reportedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void speedBoostIncreasesAp() {
		bp.item1 = Item.SPEED;
		bp.item2 = Item.ATTACK;
		bp.instructions = Arrays.asList(
				Instruction.useItem(Item.SPEED),
				Instruction.waitUntilApIs(SPEED_BOOST_AP_INCREASE),		// speed boost should hit this threshold immediately...
				Instruction.useItem(Item.ATTACK));						// ...so attack boost should happen in same tick
		player1.setBattlePlan(bp);
		player2.setBattlePlan(bp);
		
		player1.update(0);		// should fire both boosts
		
		// use speed boost
		BattleEvent firstEvent = recorder.battleEvents().get(0);
		assertEquals(Action.USE_ITEM, firstEvent.action);
		assertEquals(Item.SPEED, firstEvent.itemUsed);
		assertEquals(0, firstEvent.effects.get(0).playerIndex);
		assertEquals(Stat.AP, firstEvent.effects.get(0).stat);
		assertEquals(SPEED_BOOST_AP_INCREASE, firstEvent.effects.get(0).change, REAL_TOLERANCE);
		
		// use attack boost
		BattleEvent secondEvent = recorder.battleEvents().get(1);
		assertEquals(Action.USE_ITEM, secondEvent.action);
		assertEquals(Item.ATTACK, secondEvent.itemUsed);
		assertEquals(0, secondEvent.effects.get(0).playerIndex);
		assertEquals(Stat.ATTACK, secondEvent.effects.get(0).stat);
		
		// assert that player didn't have to wait
		assertEquals(firstEvent.time, secondEvent.time);
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
	
	private static double baseAttackOf(BattlePlan battlePlan) {
		return (battlePlan.snail.attack
				+ battlePlan.weapon.attack
				+ battlePlan.shell.attack);
	}
	
	private static double baseDefenseOf(BattlePlan battlePlan) {
		return (battlePlan.snail.defense
				+ battlePlan.weapon.defense
				+ battlePlan.shell.defense);
	}
	
	private static int baseSpeedOf(BattlePlan battlePlan) {
		return (battlePlan.snail.speed
				+ battlePlan.weapon.speed
				+ battlePlan.shell.speed);
	}
	
}
