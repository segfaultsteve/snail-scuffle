package com.snailscuffle.game.battle;

import static com.snailscuffle.common.battle.Constants.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.Action;
import com.snailscuffle.common.battle.BattleEvent;
import com.snailscuffle.common.battle.BattleEventEffect;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Inequality;
import com.snailscuffle.common.battle.Instruction;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.ItemRule;
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
	private Combatant player0;
	private Combatant player1;
	
	@Before
	public void setUp() throws Exception {
		bp = defaultBattlePlan();
		Battle battle = mock(Battle.class);
		recorder = new BattleRecorder(battle);
		player0 = new Combatant(recorder);
		player1 = new Combatant(recorder);
		
		when(battle.currentTime()).thenAnswer(i -> battleTime.intValue());
		when(battle.playerIndexOf(player0)).thenReturn(0);
		when(battle.playerIndexOf(player1)).thenReturn(1);
		
		player0.setOpponent(player1);
		player1.setOpponent(player0);
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
	public void attackEventsAreFilledOutCorrectly() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		
		BattleEvent firstEvent = runBattleUntilNextEvent();
		BattleEvent secondEvent = recorder.flatEvents().get(1);
		
		// player 1 attacks player 2
		assertEquals(0, firstEvent.playerIndex);
		assertEquals(Action.ATTACK, firstEvent.action);
		assertEquals(1, firstEvent.effects.get(0).playerIndex);
		assertEquals(Stat.HP, firstEvent.effects.get(0).stat);
		assertTrue(firstEvent.effects.get(0).change < 0);
		
		// player 2 attacks player 1
		assertEquals(1, secondEvent.playerIndex);
		assertEquals(Action.ATTACK, secondEvent.action);
		assertEquals(0, secondEvent.effects.get(0).playerIndex);
		assertEquals(Stat.HP, secondEvent.effects.get(0).stat);
		assertTrue(secondEvent.effects.get(0).change < 0);
	}
	
	@Test
	public void attackForCorrectDamage() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		
		BattleEvent firstEvent = runBattleUntilNextEvent();
		
		double expectedDamage = Combatant.DAMAGE_MULTIPLIER * baseAttackOf(bp) / baseDefenseOf(bp);
		double actualDamage = -firstEvent.effects.get(0).change;
		assertEquals(expectedDamage, actualDamage, REAL_TOLERANCE);
	}
	
	@Test
	public void attackAtCorrectTime() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		
		BattleEvent firstEvent = runBattleUntilNextEvent();
		
		int expectedTicks = Combatant.SCALE * bp.weapon.apCost / baseSpeedOf(bp) + 1;	// +1 to round up after integer division
		int actualTicks = firstEvent.time;
		assertTrue(Math.abs(expectedTicks - actualTicks) <= TIMING_TOLERANCE);
	}
	
	@Test
	public void waitUntilCorrectTime() {
		final int WAIT_AP = 2 * bp.weapon.apCost;
		bp.instructions = Arrays.asList(Instruction.waitUntilApIs(WAIT_AP));
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		
		BattleEvent firstEvent = runBattleUntilNextEvent();
		
		int expectedTicks = Combatant.SCALE * WAIT_AP / baseSpeedOf(bp) + 1;	// +1 to round up after integer division
		int actualTicks = firstEvent.time;
		assertTrue(Math.abs(expectedTicks - actualTicks) <= TIMING_TOLERANCE);
	}
	
	@Test
	public void steroidsIncreaseAttack() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		double initialAttack = baseAttackOf(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.STEROIDS;
		player0.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedIncrease = (initialAttack + STEROIDS_ATTACK) / initialAttack;
		double measuredIncrease = damageWith / damageWithout;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void snailMailIncreasesDefense() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		double initialDefense = baseDefenseOf(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.SNAIL_MAIL;
		player1.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedIncrease = (initialDefense + SNAIL_MAIL_DEFENSE) / initialDefense;
		double measuredIncrease = damageWithout / damageWith;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void caffeineIncreasesSpeed() {
		player0.setBattlePlan(bp);
		double initialSpeed = baseSpeedOf(bp);
		double ticksBefore = player0.ticksToNextEvent();
		
		bp.accessory = Accessory.CAFFEINE;
		player0.setBattlePlan(bp);
		double ticksAfter = player0.ticksToNextEvent();
		
		double expectedIncrease = (initialSpeed + CAFFEINE_SPEED ) / initialSpeed;
		double measuredIncrease = ticksBefore / ticksAfter;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void chargedAttackIncreasesAttack() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.CHARGED_ATTACK;
		player0.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedIncrease = 1 + 1.0 * bp.weapon.apCost / CHARGED_ATTACK_AP_DIVISOR;
		double measuredIncrease = damageWith / damageWithout;
		assertEquals(expectedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void adrenalineDecreasesAttackAtHighHp() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		double initialAttack = baseAttackOf(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		double hpAfterFirstHit = 100 - damageWithout;
		bp.accessory = Accessory.ADRENALINE;
		player0.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedChange = (initialAttack + (ADRENALINE_CROSSOVER - hpAfterFirstHit) / ADRENALINE_DIVISOR) / initialAttack;
		double measuredChange = damageWith / damageWithout;
		assertEquals(expectedChange, measuredChange, REAL_TOLERANCE);
		assertTrue(measuredChange < 1);
	}
	
	@Test
	public void adrenalineIncreasesAttackAtLowHp() {
		final int HIT_COUNT = 5;
		
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		double initialAttack = baseAttackOf(bp);
		
		for (int i = 0; i < HIT_COUNT; i++) {
			runBattleUntilNextEvent();
		}
		double damageWithout = -recorder.flatEvents().get(0).effects.get(0).change;
		double hpRemaining = 100 - HIT_COUNT * damageWithout;
		
		bp.accessory = Accessory.ADRENALINE;
		player0.setBattlePlan(bp);
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
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		bp.accessory = Accessory.SALTED_SHELL;
		player1.setBattlePlan(bp);
		double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
		
		double expectedChange = 1 / SALTED_SHELL_DEFENSE_MULTIPLIER;
		double measuredChange = damageWith / damageWithout;
		assertEquals(expectedChange, measuredChange, REAL_TOLERANCE);
	}
	
	@Test
	public void saltedShellIncreasesAttackInLaterPeriods() {
		final int BATTLE_DURATION_IN_PERIODS = 3;
		
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		double damageWithout = -runBattleUntilNextEvent().effects.get(0).change;
		
		bp.accessory = Accessory.SALTED_SHELL;
		player0.setBattlePlan(bp);		// first period
		
		// subsequent periods
		for (int i = 1; i < BATTLE_DURATION_IN_PERIODS; i++) {
			player0.setBattlePlan(bp); 		// next period; keep salted shell equipped
			double damageWith = -runBattleUntilNextEvent().effects.get(0).change;
			double measuredChange = damageWith / damageWithout;
			assertEquals(SALTED_SHELL_ATTACK_MULTIPLIER, measuredChange, REAL_TOLERANCE);
		}
	}
	
	@Test
	public void unequippingSaltedShellResetsTheEffect() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		double initialDamage = -runBattleUntilNextEvent().effects.get(0).change;
		
		// first period - equip it
		bp.accessory = Accessory.SALTED_SHELL;
		player1.setBattlePlan(bp);
		
		// second period - unequip it
		bp.accessory = Accessory.NONE;
		player1.setBattlePlan(bp);
		double damageAfterUnequipping = -runBattleUntilNextEvent().effects.get(0).change;
		
		// third period - re-equip it
		bp.accessory = Accessory.SALTED_SHELL;
		player1.setBattlePlan(bp);
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
		player0.setBattlePlan(bp);
		bp.accessory = Accessory.THORNS;
		player1.setBattlePlan(bp);
		
		runBattleUntilNextEvent();
		double damageDone = -recorder.flatEvents().get(0).effects.get(0).change;
		double damageTaken = -recorder.flatEvents().get(0).effects.get(1).change;
		int playerTakingDamage = recorder.flatEvents().get(0).effects.get(1).playerIndex;
		
		double expectedDamageTaken = damageDone * THORNS_DAMAGE_MULTIPLIER;
		assertEquals(expectedDamageTaken, damageTaken, REAL_TOLERANCE);
		assertEquals(0, playerTakingDamage);
	}
	
	@Test
	public void defibrillatorAllowsPlayerToTakeOneExtraHit() {
		player0.setBattlePlan(bp);
		bp.accessory = Accessory.DEFIBRILLATOR;
		player1.setBattlePlan(bp);
		
		int hits = 0;
		while (player1.isAlive()) {
			runBattleUntilNextEvent();
			hits++;
		}
		double damageEachHit = -recorder.flatEvents().get(0).effects.get(0).change;
		
		int hitsToKillplayer1WithoutDefibrillator = (int) Math.ceil(100 / damageEachHit);
		assertEquals(hitsToKillplayer1WithoutDefibrillator + 1, hits);	// +1 for defibrillator
	}
	
	@Test
	public void attackBoostEventIsFilledOutCorrectly() {
		assertBoostEventIsFilledOutCorrectly(Item.ATTACK, Stat.ATTACK);
	}
	
	@Test
	public void attackBoostIncreasesAttack() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		double initialAttack = baseAttackOf(bp);
		double damageBefore = -runBattleUntilNextEvent().effects.get(0).change;
		
		bp.items[0] = Item.ATTACK;
		bp.instructions = Arrays.asList(Instruction.useItem(Item.ATTACK));
		player0.setBattlePlan(bp);
		double reportedAttackBoost = runBattleUntilNextEvent().effects.get(0).change;
		double damageAfter = -runBattleUntilNextEvent().effects.get(0).change;
		
		double measuredIncrease = damageAfter / damageBefore;
		double reportedIncrease = (initialAttack + reportedAttackBoost) / initialAttack;
		assertEquals(ATTACK_BOOST_MULTIPLIER, measuredIncrease, REAL_TOLERANCE);
		assertEquals(reportedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void defenseBoostEventIsFilledOutCorrectly() {
		assertBoostEventIsFilledOutCorrectly(Item.DEFENSE, Stat.DEFENSE);
	}
	
	@Test
	public void defenseBoostIncreasesDefense() {
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		double initialDefense = baseDefenseOf(bp);
		double damageBefore = -runBattleUntilNextEvent().effects.get(0).change;
		
		bp.items[0] = Item.DEFENSE;
		bp.instructions = Arrays.asList(Instruction.useItem(Item.DEFENSE));
		player1.setBattlePlan(bp);
		double reportedDefenseBoost = runBattleUntilNextEvent().effects.get(0).change;
		double damageAfter = -runBattleUntilNextEvent().effects.get(0).change;
		
		double measuredIncrease = damageBefore / damageAfter;
		double reportedIncrease = (initialDefense + reportedDefenseBoost) / initialDefense;
		assertEquals(DEFENSE_BOOST_MULTIPLIER, measuredIncrease, REAL_TOLERANCE);
		assertEquals(reportedIncrease, measuredIncrease, REAL_TOLERANCE);
	}
	
	@Test
	public void speedBoostEventIsFilledOutCorrectly() {
		assertBoostEventIsFilledOutCorrectly(Item.SPEED, Stat.AP);
	}
	
	@Test
	public void speedBoostIncreasesAp() {
		bp.items[0] = Item.SPEED;
		bp.items[1] = Item.ATTACK;
		bp.instructions = Arrays.asList(
				Instruction.useItem(Item.SPEED),
				Instruction.waitUntilApIs(SPEED_BOOST_AP_INCREASE),		// speed boost should hit this threshold immediately...
				Instruction.useItem(Item.ATTACK));						// ...so attack boost should happen in same tick
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		
		player0.update(0);		// should fire both boosts
		
		// use speed boost
		BattleEvent firstEvent = recorder.flatEvents().get(0);
		assertEquals(Action.USE_ITEM, firstEvent.action);
		assertEquals(Item.SPEED, firstEvent.itemUsed);
		assertEquals(0, firstEvent.effects.get(0).playerIndex);
		assertEquals(Stat.AP, firstEvent.effects.get(0).stat);
		assertEquals(SPEED_BOOST_AP_INCREASE, firstEvent.effects.get(0).change, REAL_TOLERANCE);
		
		// use attack boost
		BattleEvent secondEvent = recorder.flatEvents().get(1);
		assertEquals(Action.USE_ITEM, secondEvent.action);
		assertEquals(Item.ATTACK, secondEvent.itemUsed);
		assertEquals(0, secondEvent.effects.get(0).playerIndex);
		assertEquals(Stat.ATTACK, secondEvent.effects.get(0).stat);
		
		// assert that player didn't have to wait
		assertEquals(firstEvent.time, secondEvent.time);
	}
	
	
	// Player 1 should always attack first given that player 2 has not used any speed modifiers.
	// This test runs a scenario where player 2 uses the stun item just before player 1 would have
	// attacked to ensure that the stun item is working correctly.
	@Test
	public void stunItemIncapacitatesOpponent() {
		player0.setBattlePlan(bp);
		
		bp.items[0] = Item.STUN;
		bp.instructions = Arrays.asList(
			Instruction.waitUntilApIs(ROCKET_AP_COST - 1),	// stun just before player0 attacks
			Instruction.useItem(Item.STUN)
		);
		player1.setBattlePlan(bp);
		
		runBattleUntilNextEvent();
		
		// player 2 used stun
		BattleEvent firstEvent = recorder.eventsByRound().get(0).get(0);
		assertEquals(Action.USE_ITEM, firstEvent.action);
		assertEquals(Item.STUN, firstEvent.itemUsed);
		assertEquals(1, firstEvent.playerIndex);
		
		runBattleUntilNextEvent();
		
        // because player 1 was stunned, player 2 got to attack first
		BattleEvent secondEvent = recorder.eventsByRound().get(0).get(1);
		assertEquals(Action.ATTACK, secondEvent.action);
		assertEquals(1, secondEvent.playerIndex);
		
		runBattleUntilNextEvent();
		
		// stun duration elapses
		BattleEvent thirdEvent = recorder.eventsByRound().get(0).get(2);
		assertEquals(Action.ITEM_DONE, thirdEvent.action);
		assertEquals(Item.STUN, thirdEvent.itemUsed);
		assertEquals(0, thirdEvent.playerIndex);
		
		runBattleUntilNextEvent();
		
		// player 1 can now attack
		BattleEvent fourthEvent = recorder.eventsByRound().get(0).get(3);
		assertEquals(Action.ATTACK, fourthEvent.action);
		assertEquals(0, fourthEvent.playerIndex);
	}
	
	@Test
	public void playerHasConditionTriggersCorrectly() {
		final int HP_THRESHOLD = 50;
		
		player0.setBattlePlan(bp);
		bp.items[0] = Item.DEFENSE;
		bp.itemRules[0] = ItemRule.useWhenIHave(Stat.HP, Inequality.LESS_THAN_OR_EQUALS, HP_THRESHOLD);
		player1.setBattlePlan(bp);
		
		while (player1.isAlive()) {
			int increment = player0.ticksToNextEvent();
			player0.update(increment);
		}
		
		List<Double> player1HpChanges = new ArrayList<>();
		for (BattleEvent event : recorder.flatEvents()) {
			if (event.action == Action.ATTACK) {
				assert(event.playerIndex == 0);
				player1HpChanges.add(event.effects.get(0).change);
			} else if (event.action == Action.USE_ITEM) {
				assert(event.playerIndex == 1);
				break;
			}
		}
		
		double hpThatTriggeredItem = player1HpChanges.stream().reduce(100.0, (x, y) -> x + y);
		double hpBeforeTrigger = player1HpChanges.stream().limit(player1HpChanges.size() - 1).reduce(100.0, (x, y) -> x + y);
		
		assertTrue(hpThatTriggeredItem < HP_THRESHOLD);
		assertTrue(hpBeforeTrigger > HP_THRESHOLD);
	}
	
	@Test
	public void enemyHasConditionTriggersCorrectly() {
		final int AP_THRESHOLD = 20;
		
		bp.items[0] = Item.DEFENSE;
		bp.itemRules[0] = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, AP_THRESHOLD);
		bp.instructions = Arrays.asList(Instruction.waitUntilApIs(2 * AP_THRESHOLD));	// don't attack
		player0.setBattlePlan(bp);
		
		bp.items[0] = null;
		bp.itemRules[0] = null;
		bp.instructions = Arrays.asList(Instruction.waitUntilApIs(AP_THRESHOLD + 1));
		player1.setBattlePlan(bp);
		
		BattleEvent boostEvent = runBattleUntilNextEvent();
		int ticksToplayer1sNextAp = player1.ticksToNextEvent();
		BattleEvent nextEvent = runBattleUntilNextEvent();
		
		assertEquals(0, boostEvent.playerIndex);
		assertEquals(Action.USE_ITEM, boostEvent.action);
		assertEquals(Item.DEFENSE, boostEvent.itemUsed);
		
		assertEquals(1, nextEvent.playerIndex);
		assertEquals(Action.ATTACK, nextEvent.action);
		assertEquals(boostEvent.time + ticksToplayer1sNextAp, nextEvent.time);		// player 2 was exactly 1 AP away from attacking
	}
	
	@Test
	public void enemyUsesItemConditionTriggersCorrectly() {
		bp.items[0] = Item.ATTACK;
		bp.instructions = Arrays.asList(Instruction.useItem(Item.ATTACK));
		player0.setBattlePlan(bp);
		
		bp.items[0] = Item.DEFENSE;
		bp.itemRules[0] = ItemRule.useWhenEnemyUses(Item.ATTACK);
		bp.instructions = null;
		player1.setBattlePlan(bp);
		
		BattleEvent firstEvent = runBattleUntilNextEvent();
		BattleEvent secondEvent = recorder.flatEvents().get(1);
		
		// player 1 uses attack boost
		assertEquals(0, firstEvent.playerIndex);
		assertEquals(Action.USE_ITEM, firstEvent.action);
		assertEquals(Item.ATTACK, firstEvent.itemUsed);
		
		// player 2 uses defense boost
		assertEquals(1, secondEvent.playerIndex);
		assertEquals(Action.USE_ITEM, secondEvent.action);
		assertEquals(Item.DEFENSE, secondEvent.itemUsed);
	}
	
	// This test verifies a fix for a bug that had caused infinite recursion in the
	// Combatant class when both players equipped thorns. One player would attack the
	// other, which would do damage to the first, which would do damage to the other,
	// and so on until a stack overflow resulted.
	@Test
	public void bothPlayersUseThorns() {
		bp.accessory = Accessory.THORNS;
		player0.setBattlePlan(bp);
		player1.setBattlePlan(bp);
		
		while (player0.isAlive() && player1.isAlive()) {
			runBattleUntilNextEvent();		// crashed before bug fix
		}
	}

	// NOTE: If multiple events fall on the same tick (e.g., players with identical battle
	// plans attack, or a player uses an item and attacks), then calling this method will
	// trigger *all* of those events. The next call to this function will return the event(s)
	// at the next recorded time.
	private BattleEvent runBattleUntilNextEvent() {
		int initialEventCount = recorder.flatEvents().size();
		
		while(recorder.flatEvents().size() == initialEventCount) {
			int p1Ticks = player0.ticksToNextEvent();
			int p2Ticks = player1.ticksToNextEvent();
			int increment = Math.min(p1Ticks, p2Ticks);
			
			battleTime += increment;
			player0.update(increment);
			player1.update(increment);
		}
		
		return recorder.flatEvents().get(initialEventCount);
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
	
	private void assertBoostEventIsFilledOutCorrectly(Item itemToTest, Stat statExpectedToIncrease) {
		player0.setBattlePlan(bp);
		bp.items[0] = itemToTest;
		bp.instructions = Arrays.asList(Instruction.useItem(itemToTest));
		player1.setBattlePlan(bp);
		
		BattleEvent boostEvent = runBattleUntilNextEvent();
		BattleEventEffect boostEffect = boostEvent.effects.get(0);
		
		assertEquals(1, boostEvent.playerIndex);
		assertEquals(Action.USE_ITEM, boostEvent.action);
		assertEquals(itemToTest, boostEvent.itemUsed);
		assertEquals(1, boostEffect.playerIndex);
		assertEquals(statExpectedToIncrease, boostEffect.stat);
		assertTrue(boostEffect.change > 0);
	}
	
}
