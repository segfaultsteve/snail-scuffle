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
	
	private ItemRule() {}		// needed for serialization via jackson-jr
	
	public ItemRule(ItemRule other) {
		if (other.hasCondition != null) {
			hasCondition = new HasCondition(other.hasCondition);
		}
		enemyUsesCondition = other.enemyUsesCondition;
	}
	
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
	
	public void validate() {
		if (hasCondition == null && enemyUsesCondition == null) {
			throw new InvalidBattleException("Item rule is missing condition");
		}
		
		if (hasCondition != null && enemyUsesCondition != null) {
			throw new InvalidBattleException("Item rule is overspecified");
		}
		
		if (hasCondition != null) {
			hasCondition.validate();
		}
	}

}
