package com.snailscuffle.game.battle;

import static com.snailscuffle.common.battle.Constants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Instruction;
import com.snailscuffle.common.battle.Item;
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
	private int hp;		// = HP * SCALE
	private int ap;		// = AP * SCALE
	private int currentInstruction;
	private final List<ActiveBoost> activeBoosts;
	private boolean secondHalf;

	public Combatant(BattlePlan firstHalfBattlePlan, int playerIndex, BattleRecorder recorder) {
		battlePlan = firstHalfBattlePlan;
		this.recorder = recorder;
		hp = INITIAL_HP * SCALE;
		ap = INITIAL_AP * SCALE;
		activeBoosts = new ArrayList<>();
	}
	
	public void setOpponent(Combatant opponent) {
		this.opponent = opponent;
	}
	
	// Each tick, the player gains Speed/SCALE action points. The number of ticks
	// until the next whole-number AP is therefore:
	//   ticks = ((next whole AP) - (current AP)) / (Speed/SCALE)    <-- normal division
	//         = ((ap/SCALE + 1)*SCALE - ap) * SCALE / speedStat()   <-- integer division
	public int ticksToNextAp() {
		int nextAp = (ap/SCALE + 1) * SCALE;
		return (nextAp - ap) * SCALE / speedStat() + 1;		// +1 to round up after integer division
	}
	
	public void update(int deltaTicks) {
		ap += deltaTicks * speedStat() / SCALE;
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
		if (ap >= attackCost) {
			attackOpponent();
			ap -= attackCost;
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
		hp -= damage;
		
		if (battlePlan.accessory == Accessory.THORNS) {
			int damageToAttacker = (int) (THORNS_DAMAGE_MULTIPLIER * damage);
			recorder.recordAttack(this, damageToAttacker);
			opponent.takeDamage(damageToAttacker);
		} else if (hp <= 0 && battlePlan.accessory == Accessory.DEFIBRILLATOR) {
			hp = SCALE;		// 1 HP
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
			applyBoost(Item.ATTACK, ATTACK_BOOST_DURATION, () -> attackStat());
			break;
		case DEFENSE:
			applyBoost(Item.DEFENSE, DEFENSE_BOOST_DURATION, () -> defenseStat());
			break;
		case SPEED:
			ap += SCALE * SPEED_BOOST_AP_INCREASE;
			recorder.recordUseItem(this, Item.SPEED, Stat.SPEED, SPEED_BOOST_AP_INCREASE);
			break;
		default:
			throw new RuntimeException("Unexpected item");
		}
	}
	
	private void applyBoost(Item type, int duration, IntSupplier statFunc) {
		Stat stat = null;
		if (type == Item.ATTACK) {
			stat = Stat.ATTACK;
		} else if (type == Item.DEFENSE) {
			stat = Stat.DEFENSE;
		} else {
			throw new RuntimeException("Unexpected boost");
		}
		
		int statBefore = statFunc.getAsInt();
		activeBoosts.add(new ActiveBoost(type, duration));
		double statChange = statFunc.getAsInt() - statBefore;
		recorder.recordUseItem(this, type, stat, statChange/SCALE);
	}
	
	private boolean tryWait(int threshold) {
		if (ap >= threshold) {
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
			attack += attack * ap / CHARGED_ATTACK_AP_DIVISOR;
		} else if (battlePlan.accessory == Accessory.ADRENALINE) {
			attack += (ADRENALINE_CROSSOVER - hp) / ADRENALINE_DIVISOR;
		} else if (secondHalf && battlePlan.accessory == Accessory.SALTED_SHELL) {
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
		
		if (!secondHalf && battlePlan.accessory == Accessory.SALTED_SHELL) {
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
	
	public boolean isAlive() {
		return hp > 0;
	}
	
	public void setSecondHalfBattlePlan(BattlePlan battlePlan) {
		this.battlePlan = battlePlan;
		secondHalf = true;
	}

}
