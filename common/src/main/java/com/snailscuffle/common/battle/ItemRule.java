package com.snailscuffle.common.battle;

import java.io.Serializable;

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
	
	public void validate() {
		if (hasCondition == null && enemyUsesCondition == null) {
			throw new InvalidBattleException("item rule is missing condition");
		}
		
		if (hasCondition != null && enemyUsesCondition != null) {
			throw new InvalidBattleException("item rule is overspecified");
		}
		
		if (hasCondition != null) {
			hasCondition.validate();
		}
	}

}
