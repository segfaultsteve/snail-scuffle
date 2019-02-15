package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BattlePlan implements Serializable {
	
	public Snail snail;
	public Weapon weapon;
	public Shell shell;
	public Accessory accessory;
	public Item[] items = new Item[2];
	public ItemRule[] itemRules = new ItemRule[2];
	public List<Instruction> instructions;
	
	public BattlePlan() {}
	
	public BattlePlan(BattlePlan other) {
		snail = other.snail;
		weapon = other.weapon;
		shell = other.shell;
		accessory = other.accessory;
		
		for (int i = 0; i < 2; i++) {
			items[i] = other.items[i];
			if (other.itemRules[i] != null) {
				itemRules[i] = new ItemRule(other.itemRules[i]);
			}
		}
		
		if (other.instructions != null) {
			instructions = new ArrayList<>();
			for (Instruction i : other.instructions) {
				instructions.add(new Instruction(i));
			}
		}
	}
	
	public void validate() {
		if (snail == null || weapon == null) {
			String nullEquip = "Weapon";
			if (snail == null) {
				nullEquip = "Snail";
			}
			throw new InvalidBattleException(nullEquip + " not found");
		}
		
		if (snail == Snail.DOUG) {
			shell = null;
		}
		
		if (shell == null) {
			shell = Shell.NONE;
		}
		
		if (accessory == null) {
			accessory = Accessory.NONE;
		}
		
		for (int i = 0; i < 2; i++) {
			if (items[i] == null) {
				items[i] = Item.NONE;
			}
			if (itemRules[i] != null) {
				itemRules[i].validate();
			}
		}
		
		if (instructions != null) {
			instructions.forEach(i -> i.validate());
		}
	}

}
