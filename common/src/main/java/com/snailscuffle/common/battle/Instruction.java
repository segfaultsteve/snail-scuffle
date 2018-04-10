package com.snailscuffle.common.battle;

import java.io.Serializable;

// represents an instruction in the sequence of instructions that the player
// configures for his or her snail; valid forms are:
//     attack
//     use [item]
//     wait until AP >= [threshold]
public class Instruction implements Serializable {
	
	public enum Type {
		ATTACK,
		USE,
		WAIT;
		
		@Override
		public String toString() {
			return this.name().toLowerCase();	// serialize as lowercase
		}
	}
	
	// if type is ATTACK, itemToUse and apThreshold are ignored
	// if type is USE, itemToUse must be non-null; apThreshold is ignored
	// if type is WAIT, itemToUse is ignored
	// note: the only allowed WAIT condition is of the form, "wait until I
	// have AP >= [threshold]"; this is why this condition is represented
	// simply as a single integer, apThreshold
	public Type type;
	public Item itemToUse;
	public int apThreshold;
	
	public static Instruction attack() {
		Instruction i = new Instruction();
		i.type = Type.ATTACK;
		return i;
	}
	
	public static Instruction useItem(Item toUse) {
		Instruction i = new Instruction();
		i.type = Type.USE;
		i.itemToUse = toUse;
		return i;
	}
	
	public static Instruction waitUntilApIs(int threshold) {
		Instruction i = new Instruction();
		i.type = Type.WAIT;
		i.apThreshold = threshold;
		return i;
	}
	
	private Instruction() {}
	
	public Instruction(Instruction other) {
		type = other.type;
		itemToUse = other.itemToUse;
		apThreshold = other.apThreshold;
	}
	
	public void validate() {
		if (type == null) {
			throw new InvalidBattleException("Instruction has unknown type");
		}
		
		if (type == Type.USE) {
			apThreshold = 0;
			if (itemToUse == null) {
				throw new InvalidBattleException("USE instruction is missing itemToUse");
			}
		}
		
		if (type == Type.WAIT) {
			itemToUse = null;
			if (apThreshold < 0) {
				apThreshold = 0;
			}
		}
	}
	
}
