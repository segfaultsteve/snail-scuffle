package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
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
	
	public BattlePlan() {}
	
	public BattlePlan(BattlePlan other) {
		snail = other.snail;
		weapon = other.weapon;
		shell = other.shell;
		accessory = other.accessory;
		item1 = other.item1;
		item2 = other.item2;
		
		if (other.item1Rule != null) {
			item1Rule = new ItemRule(other.item1Rule);
		}
		
		if (other.item2Rule != null) {
			item2Rule = new ItemRule(other.item2Rule);
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
