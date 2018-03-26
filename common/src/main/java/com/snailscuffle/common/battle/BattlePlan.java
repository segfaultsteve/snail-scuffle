package com.snailscuffle.common.battle;

import java.io.Serializable;

public class BattlePlan implements Serializable {
	
	public Snail snail;
	public Weapon weapon;
	public Shell shell;
	public Accessory accessory;
	public Item item1;
	public Item item2;
	
	public void validate() {
		if (snail == null || weapon == null || shell == null || accessory == null || item1 == null || item2 == null) {
			String nullEquip = "Item2";
			if (snail == null) nullEquip = "Snail";
			else if (weapon == null) nullEquip = "Weapon";
			else if (shell == null) nullEquip = "Shell";
			else if (accessory == null) nullEquip = "Accessory";
			else if (item1 == null) nullEquip = "Item1";
			
			throw new InvalidBattleException(nullEquip + " not found");
		}
		
		if (snail == Snail.DOUG) {
			shell = Shell.NONE;
		}
	}

}
