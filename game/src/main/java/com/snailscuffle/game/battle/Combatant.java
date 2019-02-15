package com.snailscuffle.game.battle;

import static com.snailscuffle.common.battle.Constants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Instruction;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.ItemRule;
import com.snailscuffle.common.battle.Player;
import com.snailscuffle.common.battle.Stat;

class Combatant {
	
	private static class ItemEffect {
		private Item type;
		private int ticksRemaining;
		
		private ItemEffect(Item type, int duration) {
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
	static final int SCALE = 1000;
	
	// Damage p1 does to p2 = SCALE * DAMAGE_MULTIPLIER * Attack_p1 / Defense_p2
	static final int DAMAGE_MULTIPLIER = 10;
	
	// maximum number of items that a player can use across all periods
	static final int MAX_ITEMS_PER_BATTLE = 2;
	
	private BattlePlan battlePlan;
	private final BattleRecorder recorder;
	private Combatant opponent;
	private MeteredStat hp;		// = HP * SCALE
	private MeteredStat ap;		// = AP * SCALE
	private int currentInstruction;
	private final List<ItemEffect> activeEffects = new ArrayList<>();
	private int itemsUsed;
	private int saltedShellCounter;		// 0 = not equipped; 1 = equipped this period; 2 = equipped last period and this period; etc.
	private boolean defibrillatorHasActivated = false;

	Combatant(BattleRecorder recorder) {
		this.recorder = recorder;
		hp = new MeteredStat(this, INITIAL_HP * SCALE);
		ap = new MeteredStat(this, INITIAL_AP * SCALE);
	}
	
	void setOpponent(Combatant opponent) {
		this.opponent = opponent;
		hp.registerOpponentForCallbacks(opponent);
		ap.registerOpponentForCallbacks(opponent);
	}
	
	// Each tick, the player gains Speed/SCALE action points. The number of ticks
	// until the next whole-number AP is therefore:
	//   ticks = ((next whole AP) - (current AP)) / (Speed/SCALE)    <-- normal division
	//         = ((ap/SCALE + 1)*SCALE - ap) * SCALE / speedStat()   <-- integer division
	int ticksToNextEvent() {
		int nextAp = (ap.get()/SCALE + 1) * SCALE;
		int ticksToNextEvent = (nextAp - ap.get()) * SCALE / speedStat() + 1;	// +1 to round up after integer division
		for (ItemEffect effect : activeEffects) {
			ticksToNextEvent = Math.min(ticksToNextEvent, effect.ticksRemaining);
		}
		assert(ticksToNextEvent >= 0);
		return Math.max(ticksToNextEvent, 0);
	}
	
	void update(int deltaTicks) {		
		decrementActiveEffectTimers(deltaTicks);
		
		if (isStunned()) {
			return;
		}
		
		ap.add(deltaTicks * speedStat() / SCALE);
		
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
	
	private boolean isStunned() {
		return activeEffects.stream().anyMatch(e -> e.type == Item.STUN);
	}
	
	private void decrementActiveEffectTimers(int ticks) {
		activeEffects.forEach(e -> e.ticksRemaining -= ticks);
		List<ItemEffect> toRemove = activeEffects.stream()
				.filter(e -> e.ticksRemaining <= 0)
				.collect(Collectors.toList());
		for (ItemEffect effect : toRemove) {
			recorder.recordItemDone(this, effect.type);
			activeEffects.remove(effect);
		}
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
		opponent.takeDamage(damage, true);
	}
	
	private void takeDamage(int damage, boolean applyThorns) {
		hp.subtract(damage);
		
		if (battlePlan.accessory == Accessory.THORNS && applyThorns) {
			int damageToAttacker = (int) (THORNS_DAMAGE_MULTIPLIER * damage);
			recorder.addEffectToLastEvent(opponent, Stat.HP, -1.0 * damageToAttacker / SCALE);
			opponent.takeDamage(damageToAttacker, false);
		} else if (hp.get() <= 0 && battlePlan.accessory == Accessory.DEFIBRILLATOR && !defibrillatorHasActivated) {
			hp.set(SCALE);		// 1 HP
			defibrillatorHasActivated = true;
		}
	}
	
	private boolean tryUseItem(Item item) {
		if (itemsUsed < MAX_ITEMS_PER_BATTLE) {
			for (int i = 0; i < battlePlan.items.length; i++) {
				if (battlePlan.items[i] == item) {
					applyItem(i);
					break;
				}
			}
		}
		currentInstruction++;
		return true;	// using an item has zero "cost"; always continue to next instruction
	}
	
	private void applyItem(int itemIndex) {
		Item item = battlePlan.items[itemIndex];
		battlePlan.items[itemIndex] = null;
		
		switch (item) {
		case ATTACK:
			int initialAttack = attackStat();
			activeEffects.add(new ItemEffect(Item.ATTACK, ATTACK_BOOST_DURATION));
			recorder.recordUseItem(this, Item.ATTACK, Stat.ATTACK, 1.0 * (attackStat() - initialAttack) / SCALE);
			break;
			
		case DEFENSE:
			int initialDefense = defenseStat();
			activeEffects.add(new ItemEffect(Item.DEFENSE, DEFENSE_BOOST_DURATION));
			recorder.recordUseItem(this, Item.DEFENSE, Stat.DEFENSE, 1.0 * (defenseStat() - initialDefense) / SCALE);
			break;
			
		case SPEED:
			ap.add(SCALE * SPEED_BOOST_AP_INCREASE);
			recorder.recordUseItem(this, Item.SPEED, Stat.AP, SPEED_BOOST_AP_INCREASE);
			break;
			
		case STUN:
			opponent.activeEffects.add(new ItemEffect(Item.STUN, STUN_DURATION));
			recorder.recordUseItem(this, Item.STUN, Stat.NONE, 0);
			break;
			
		case HEAL:
			hp.add(SCALE * HEAL_HP_INCREASE);
			recorder.recordUseItem(this, Item.HEAL, Stat.HP, HEAL_HP_INCREASE);
			break;
			
		case NONE:
			return;
			
		default:
			throw new RuntimeException("Unexpected item");
		}
		itemsUsed++;
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
			attack += attack * ap.get() / (CHARGED_ATTACK_AP_DIVISOR * SCALE);
		} else if (battlePlan.accessory == Accessory.ADRENALINE) {
			attack += (ADRENALINE_CROSSOVER * SCALE - hp.get()) / ADRENALINE_DIVISOR;
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
	
	int speedStat() {
		return SCALE * (battlePlan.snail.speed
				+ battlePlan.weapon.speed
				+ battlePlan.shell.speed
				+ battlePlan.accessory.speed);
	}
	
	int getHp() {
		return hp.get();
	}
	
	int getAp() {
		return ap.get();
	}
	
	List<Item> getActiveEffects() {
		return activeEffects.stream().map(e -> e.type).collect(Collectors.toList());
	};
	
	private double boostFactor(Item type) {
		double boost = 1.0;
		double multiplier = 1.0;
		if (type == Item.ATTACK) {
			multiplier = ATTACK_BOOST_MULTIPLIER;
		} else if (type == Item.DEFENSE) {
			multiplier = DEFENSE_BOOST_MULTIPLIER;
		}
		
		int activeCount = (int) activeEffects.stream().filter(e -> e.type == type).count();
		for (int i = 0; i < activeCount; i++) {
			boost *= multiplier;
		}
		
		return boost;
	}
	
	private void onOpponentUsedItem(Item item) {
		if (itemsUsed >= MAX_ITEMS_PER_BATTLE) {
			return;
		}
		
		for (int i = 0; i < battlePlan.items.length; i++) {
			if (battlePlan.items[i] != null && usesConditionIsSatisfied(battlePlan.itemRules[i], item)) {
				applyItem(i);
			}
		}
	}
	
	private static boolean usesConditionIsSatisfied(ItemRule itemRule, Item itemUsed) {
		return (itemRule != null && itemRule.triggersWhenEnemyUses(itemUsed));
	}

	void onStatChanged() {
		double myHp = 1.0 * hp.get() / SCALE;
		double myAp = 1.0 * ap.get() / SCALE;
		checkItemRuleHasConditions(Player.ME, myHp, myAp);
	}
	
	void onEnemyStatChanged() {
		double enemyHp = 1.0 * opponent.hp.get() / SCALE;
		double enemyAp = 1.0 * opponent.ap.get() / SCALE;
		checkItemRuleHasConditions(Player.ENEMY, enemyHp, enemyAp);
	}
	
	private void checkItemRuleHasConditions(Player subject, double subjectHp, double subjectAp) {
		for (int i = 0; i < battlePlan.items.length; i++) {
			if (battlePlan.items[i] != null && hasConditionIsSatisfied(battlePlan.itemRules[i], subject, subjectHp, subjectAp)) {
				applyItem(i);
			}
		}
	}
	
	private static boolean hasConditionIsSatisfied(ItemRule itemRule, Player subject, double subjectHp, double subjectAp) {
		if (itemRule == null || itemRule.hasCondition == null) {
			return false;
		}
		return itemRule.triggersWhenPlayerHas(subject, subjectHp, subjectAp);
	}
	
	boolean isAlive() {
		return hp.get() > 0;
	}
	
	void setBattlePlan(BattlePlan newPlan) {
		battlePlan = updateBattlePlan(newPlan, battlePlan, MAX_ITEMS_PER_BATTLE - itemsUsed);
		currentInstruction = 0;
		
		if (battlePlan.accessory == Accessory.SALTED_SHELL) {
			saltedShellCounter++;
		} else {
			saltedShellCounter = 0;
		}
	}

	private static BattlePlan updateBattlePlan(BattlePlan newPlan, BattlePlan currentPlan, int itemsToAllow) {
		if (currentPlan == null) {
			return new BattlePlan(newPlan);		// deep copy (item rules, instructions) so we don't modify original
		}
		
		BattlePlan validatedPlan = new BattlePlan(newPlan);		// deep copy
		
		// force snail to be the same and allow at most one of {weapon, shell, accessory} to change
		validatedPlan.snail = currentPlan.snail;
		validatedPlan.weapon = currentPlan.weapon;
		validatedPlan.shell = currentPlan.shell;
		validatedPlan.accessory = currentPlan.accessory;
		
		if (newPlan.weapon != currentPlan.weapon) {
			validatedPlan.weapon = newPlan.weapon;
		} else if (newPlan.shell != currentPlan.shell) {
			validatedPlan.shell = newPlan.shell;
		} else if (newPlan.accessory != currentPlan.accessory) {
			validatedPlan.accessory = newPlan.accessory;
		}
		
		return validatedPlan;
	}

}
