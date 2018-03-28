package com.snailscuffle.common.battle;

import java.io.Serializable;

public class ItemRule implements Serializable {
	
	public Item item;
	public HasCondition hasCondition;
	public Item enemyUsesCondition;
	
	public void validate() {
		if (item == null) {
			throw new InvalidBattleException("item rule is missing item");
		}
		
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
