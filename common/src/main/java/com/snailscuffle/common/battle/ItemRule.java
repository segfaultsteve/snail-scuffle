package com.snailscuffle.common.battle;

import java.io.Serializable;

// example usage:
// BattlePlan bp = new BattlePlan();
// [...]
// bp.item1 = Item.ATTACK;
// bp.item1Rule = ItemRule.useWhenIHave(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 30);
// bp.item2 = Item.DEFENSE;
// bp.item2Rule = ItemRule.useWhenEnemyUses(Item.ATTACK);
public class ItemRule implements Serializable {
	
	public HasCondition hasCondition;
	public Item enemyUsesCondition;
	
	public static ItemRule useWhenIHave(Stat stat, Inequality inequality, int threshold) {
		ItemRule rule = new ItemRule();
		rule.hasCondition = new HasCondition(Player.ME, stat, inequality, threshold);
		return rule;
	}
	
	public static ItemRule useWhenEnemyHas(Stat stat, Inequality inequality, int threshold) {
		ItemRule rule = new ItemRule();
		rule.hasCondition = new HasCondition(Player.ENEMY, stat, inequality, threshold);
		return rule;
    }
   
	public static ItemRule useWhenEnemyUses(Item item) {
		ItemRule rule = new ItemRule();
		rule.enemyUsesCondition = item;
		return rule;
    }
	
	private ItemRule() {}
	
	public ItemRule(ItemRule other) {
		if (other.hasCondition != null) {
			hasCondition = new HasCondition(other.hasCondition);
		}
		enemyUsesCondition = other.enemyUsesCondition;
	}
	
	public void validate() {
		if (hasCondition == null && (enemyUsesCondition == null || enemyUsesCondition == Item.NONE)) {
			throw new InvalidBattleException("Item rule is missing condition");
		}
		
		if (hasCondition != null && (enemyUsesCondition != null || enemyUsesCondition == Item.NONE)) {
			throw new InvalidBattleException("Item rule is overspecified");
		}
		
		if (hasCondition != null) {
			hasCondition.validate();
		}
	}
	
	public boolean triggersWhenPlayerHas(Player subject, double hp, double ap) {
		if (hasCondition == null || hasCondition.player != subject) {
			return false;
		}
		
		double statValue;
		if (hasCondition.stat == Stat.HP) {
			statValue = hp;
		} else if (hasCondition.stat == Stat.AP) {
			statValue = ap;
		} else {
			throw new RuntimeException("Unexpected stat");
		}
		
		return hasCondition.inequality.evaluate(statValue, hasCondition.threshold);
	}
	
	public boolean triggersWhenEnemyUses(Item item) {
		return (enemyUsesCondition != null && enemyUsesCondition != Item.NONE && item == enemyUsesCondition);
	}
	
}
