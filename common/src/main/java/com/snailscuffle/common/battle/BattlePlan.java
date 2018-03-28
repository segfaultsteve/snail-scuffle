package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.List;

public class BattlePlan implements Serializable {
	
	public Snail snail;
	public Weapon weapon;
	public Shell shell;
	public Accessory accessory;
	public Item item1;
	public Item item2;
	public ItemRule item1Rule;
	public ItemRule item2Rule;
	public List<Instruction> instructions;
	
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
		
		if (item1Rule != null) {
			item1Rule.validate();
		}
		
		if (item2Rule != null) {
			item2Rule.validate();
		}
		
		if (instructions != null) {
			instructions.forEach(i -> i.validate());
		}
	}

}
