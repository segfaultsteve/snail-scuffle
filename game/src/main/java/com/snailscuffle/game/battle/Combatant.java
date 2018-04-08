package com.snailscuffle.game.battle;

import static com.snailscuffle.common.battle.Constants.ADRENALINE_CROSSOVER;
import static com.snailscuffle.common.battle.Constants.ADRENALINE_DIVISOR;
import static com.snailscuffle.common.battle.Constants.CHARGED_ATTACK_AP_DIVISOR;
import static com.snailscuffle.common.battle.Constants.INITIAL_AP;
import static com.snailscuffle.common.battle.Constants.INITIAL_HP;
import static com.snailscuffle.common.battle.Constants.SALTED_SHELL_ATTACK_MULTIPLIER;
import static com.snailscuffle.common.battle.Constants.SALTED_SHELL_DEFENSE_MULTIPLIER;
import static com.snailscuffle.common.battle.Constants.THORNS_DAMAGE_MULTIPLIER;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattlePlan;

public class Combatant {
	
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
	private BattleRecorder recorder;
	private Combatant opponent;
	private int hp;		// = HP * SCALE
	private int ap;		// = AP * SCALE
	private boolean secondHalf;

	public Combatant(BattlePlan firstHalfBattlePlan, int playerIndex, BattleRecorder recorder) {
		battlePlan = firstHalfBattlePlan;
		this.recorder = recorder;
		hp = INITIAL_HP * SCALE;
		ap = INITIAL_AP * SCALE;
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
		int attackCost = battlePlan.weapon.apCost * SCALE;
		if (ap >= attackCost) {
			attackOpponent();
			ap -= attackCost;
		}
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
		
		return defense;
	}
	
	private int speedStat() {
		return SCALE * (battlePlan.snail.speed
				+ battlePlan.weapon.speed
				+ battlePlan.shell.speed
				+ battlePlan.accessory.speed);
	}
	
	public boolean isAlive() {
		return hp > 0;
	}
	
	public void setSecondHalfBattlePlan(BattlePlan battlePlan) {
		this.battlePlan = battlePlan;
		secondHalf = true;
	}

}
