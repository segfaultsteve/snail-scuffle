package com.snailscuffle.game.battle;

import static com.snailscuffle.common.battle.Constants.*;

import java.util.ArrayList;
import java.util.List;
import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.HasCondition;
import com.snailscuffle.common.battle.Instruction;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.ItemRule;
import com.snailscuffle.common.battle.Stat;

public class Combatant {
	
	private static class ActiveBoost {
		private Item type;
		private int ticksRemaining;
		
		private ActiveBoost(Item type, int duration) {
			this.type = type;
			ticksRemaining = duration;
		}
	}
	
	// Attack, Defense, Speed, HP, and AP are all integer quantities. Their representations
	// in this class are multiplied by SCALE. For example, if SCALE=1000, then hp=52500
	// represents HP of 52.5. In this way, SCALE determines the precision of these quantities
	// and of arithmetic involving them.
	// 
	// SCALE also defines the size of one time tick. Every tick, the player gains Speed/SCALE
	// action points. Equivalently, after SCALE ticks, the player gains AP equal to Speed.
	public static final int SCALE = 1000;
	
	// Damage p1 does to p2 = SCALE * DAMAGE_MULTIPLIER * Attack_p1 / Defense_p2
	private static final int DAMAGE_MULTIPLIER = 10;
	
	private BattlePlan battlePlan;
	private final BattleRecorder recorder;
	private Combatant opponent;
	private MeteredStat hp;		// = HP * SCALE
	private MeteredStat ap;		// = AP * SCALE
	private int currentInstruction;
	private final List<ActiveBoost> activeBoosts;
	private int saltedShellCounter;		// 0 = not equipped; 1 = equipped this period; 2 = equipped last period and this period; etc.

	public Combatant(int playerIndex, BattleRecorder recorder) {
		this.recorder = recorder;
		hp = new MeteredStat(this, INITIAL_HP * SCALE);
		ap = new MeteredStat(this, INITIAL_AP * SCALE);
		activeBoosts = new ArrayList<>();
	}
	
	public void setOpponent(Combatant opponent) {
		this.opponent = opponent;
		hp.registerOpponentForCallbacks(opponent);
		ap.registerOpponentForCallbacks(opponent);
	}
	
	// Each tick, the player gains Speed/SCALE action points. The number of ticks
	// until the next whole-number AP is therefore:
	//   ticks = ((next whole AP) - (current AP)) / (Speed/SCALE)    <-- normal division
	//         = ((ap/SCALE + 1)*SCALE - ap) * SCALE / speedStat()   <-- integer division
	public int ticksToNextAp() {
		int nextAp = (ap.get()/SCALE + 1) * SCALE;
		return (nextAp - ap.get()) * SCALE / speedStat() + 1;		// +1 to round up after integer division
	}
	
	public void update(int deltaTicks) {
		ap.add(deltaTicks * speedStat() / SCALE);
		decrementBoostTimers(activeBoosts, deltaTicks);
		
		boolean continueTurn = true;
		while (continueTurn) {
			Instruction instruction = getCurrentInstruction();
			switch (instruction.type) {
			case ATTACK:
				continueTurn = tryAttack();
				break;
			case USE:
				continueTurn = tryUseItem(instruction.itemToUse);
				break;
			case WAIT:
				continueTurn = tryWait(instruction.apThreshold * SCALE);
				break;
			default:
				throw new RuntimeException("Unexpected instruction");
			}
		}
	}
	
	private Instruction getCurrentInstruction() {
		Instruction instruction = Instruction.attack();
		if (battlePlan.instructions != null && currentInstruction < battlePlan.instructions.size()) {
			instruction = battlePlan.instructions.get(currentInstruction);
		}
		return instruction;
	}
	
	private static void decrementBoostTimers(List<ActiveBoost> boosts, int ticks) {
		boosts.forEach(b -> b.ticksRemaining -= ticks);
		boosts.removeIf(b -> b.ticksRemaining <= 0);
	}
	
	private boolean tryAttack() {
		int attackCost = battlePlan.weapon.apCost * SCALE;
		if (ap.get() >= attackCost) {
			attackOpponent();
			ap.subtract(attackCost);
			currentInstruction++;
			return true;
		}
		return false;
	}
	
	private void attackOpponent() {
		int damage = SCALE * DAMAGE_MULTIPLIER * attackStat() / opponent.defenseStat();
		recorder.recordAttack(this, 1.0 * damage / SCALE);
		opponent.takeDamage(damage);
	}
	
	private void takeDamage(int damage) {
		hp.subtract(damage);
		
		if (battlePlan.accessory == Accessory.THORNS) {
			int damageToAttacker = (int) (THORNS_DAMAGE_MULTIPLIER * damage);
			recorder.recordAttack(this, 1.0 * damageToAttacker / SCALE);
			opponent.takeDamage(damageToAttacker);
		} else if (hp.get() <= 0 && battlePlan.accessory == Accessory.DEFIBRILLATOR) {
			hp.set(SCALE);		// 1 HP
			battlePlan.accessory = Accessory.NONE;	// only use defibrillator once
		}
	}
	
	private boolean tryUseItem(Item item) {
		if (battlePlan.item1 == item) {
			applyItem(battlePlan.item1);
			battlePlan.item1 = null;
		} else if (battlePlan.item2 == item) {
			applyItem(battlePlan.item2);
			battlePlan.item2 = null;
		}
		currentInstruction++;
		return true;	// using an item has zero "cost"; always continue to next instruction
	}
	
	private void applyItem(Item item) {
		switch (item) {
		case ATTACK:
			int initialAttack = attackStat();
			activeBoosts.add(new ActiveBoost(Item.ATTACK, ATTACK_BOOST_DURATION));
			recorder.recordUseItem(this, Item.ATTACK, Stat.ATTACK, 1.0 * (attackStat() - initialAttack) / SCALE);
			break;
			
		case DEFENSE:
			int initialDefense = defenseStat();
			activeBoosts.add(new ActiveBoost(Item.DEFENSE, DEFENSE_BOOST_DURATION));
			recorder.recordUseItem(this, Item.DEFENSE, Stat.DEFENSE, 1.0 * (defenseStat() - initialDefense) / SCALE);
			break;
			
		case SPEED:
			ap.add(SCALE * SPEED_BOOST_AP_INCREASE);
			recorder.recordUseItem(this, Item.SPEED, Stat.AP, SPEED_BOOST_AP_INCREASE);
			break;
			
		default:
			throw new RuntimeException("Unexpected item");
		}
		
		opponent.onOpponentUsedItem(item);
	}
	
	private boolean tryWait(int threshold) {
		if (ap.get() >= threshold) {
			currentInstruction++;
			return true;
		}
		return false;
	}
	
	private int attackStat() {
		int attack = SCALE * (battlePlan.snail.attack
				+ battlePlan.weapon.attack
				+ battlePlan.shell.attack
				+ battlePlan.accessory.attack);
		
		if (battlePlan.accessory == Accessory.CHARGED_ATTACK) {
			attack += attack * ap.get() / CHARGED_ATTACK_AP_DIVISOR;
		} else if (battlePlan.accessory == Accessory.ADRENALINE) {
			attack += (ADRENALINE_CROSSOVER - hp.get()) / ADRENALINE_DIVISOR;
		} else if (saltedShellCounter > 1 && battlePlan.accessory == Accessory.SALTED_SHELL) {
			attack *= SALTED_SHELL_ATTACK_MULTIPLIER;
		}
		attack *= boostFactor(Item.ATTACK);
		
		return attack;
	}
	
	private int defenseStat() {
		int defense = SCALE * (battlePlan.snail.defense
				+ battlePlan.weapon.defense
				+ battlePlan.shell.defense
				+ battlePlan.accessory.defense);
		
		if (saltedShellCounter <= 1 && battlePlan.accessory == Accessory.SALTED_SHELL) {
			defense *= SALTED_SHELL_DEFENSE_MULTIPLIER;
		}
		defense *= boostFactor(Item.DEFENSE);
		
		return defense;
	}
	
	private int speedStat() {
		return SCALE * (battlePlan.snail.speed
				+ battlePlan.weapon.speed
				+ battlePlan.shell.speed
				+ battlePlan.accessory.speed);
	}
	
	private double boostFactor(Item type) {
		double boost = 1.0;
		double multiplier = 1.0;
		if (type == Item.ATTACK) {
			multiplier = ATTACK_BOOST_MULTIPLIER;
		} else if (type == Item.DEFENSE) {
			multiplier = DEFENSE_BOOST_MULTIPLIER;
		}
		
		int activeCount = (int) activeBoosts.stream().filter(b -> b.type == type).count();
		for (int i = 0; i < activeCount; i++) {
			boost *= multiplier;
		}
		
		return boost;
	}
	
	private void onOpponentUsedItem(Item item) {
		if (battlePlan.item1 != null && usesConditionIsSatisfied(battlePlan.item1Rule, item)) {
			applyItem(battlePlan.item1);
			battlePlan.item1 = null;
		}
		
		if (battlePlan.item2 != null && usesConditionIsSatisfied(battlePlan.item2Rule, item)) {
			applyItem(battlePlan.item2);
			battlePlan.item2 = null;
		}
	}
	
	private static boolean usesConditionIsSatisfied(ItemRule itemRule, Item itemUsed) {
		return (itemRule != null && itemRule.enemyUsesCondition != null && itemRule.enemyUsesCondition == itemUsed);
	}

	public void onStatChanged() {
		checkItemRuleHasConditions(this);
	}
	
	public void onEnemyStatChanged() {
		checkItemRuleHasConditions(opponent);
	}
	
	private void checkItemRuleHasConditions(Combatant subject) {
		if (battlePlan.item1 != null && hasConditionIsSatisfied(battlePlan.item1Rule, subject)) {
			applyItem(battlePlan.item1);
			battlePlan.item1 = null;
		}
		
		if (battlePlan.item2 != null && hasConditionIsSatisfied(battlePlan.item2Rule, subject)) {
			applyItem(battlePlan.item2);
			battlePlan.item2 = null;
		}
	}
	
	private static boolean hasConditionIsSatisfied(ItemRule itemRule, Combatant subject) {
		if (itemRule == null || itemRule.hasCondition == null) {
			return false;
		}
		
		HasCondition condition = itemRule.hasCondition;
		int statValue = subject.getValue(itemRule.hasCondition.stat);
		return condition.inequality.evaluate(statValue, condition.threshold);
	}
	
	private int getValue(Stat stat) {
		if (stat == Stat.HP) {
			return hp.get();
		} else if (stat == Stat.AP) {
			return ap.get();
		} else {
			throw new RuntimeException("Unexpected stat");
		}
	}
	
	public boolean isAlive() {
		return hp.get() > 0;
	}
	
	public void setBattlePlan(BattlePlan battlePlan) {
		this.battlePlan = battlePlan;
		currentInstruction = 0;
		
		if (battlePlan.accessory == Accessory.SALTED_SHELL) {
			saltedShellCounter++;
		} else {
			saltedShellCounter = 0;
		}
	}

}
