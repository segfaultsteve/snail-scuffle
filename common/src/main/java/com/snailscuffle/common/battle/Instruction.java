package com.snailscuffle.common.battle;

import java.io.Serializable;

// represents an instruction in the sequence of instructions that the player
// configures for his or her snail; valid forms are:
//     attack
//     use [item]
//     wait until [stat] [inequality] [threshold]
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
	
	// if type is ATTACK, itemToUse and waitUntilCondition are ignored
	// if type is USE, itemToUse must be non-null; waitUntilCondition is ignored
	// if type is WAIT, waitUntilCondition must be non-null; itemToUse is ignored
	// note: for a WAIT instruction, the "player" field of waitUntilConditoin is
	// ignored, since only Player.ME is allowed; in other words, this condition
	// always has the form: "wait until I have [stat] [inequaltiy] [threshold]"
	public Type type;
	public Item itemToUse;
	public HasCondition waitUntilCondition;
	
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
	
	public static Instruction wait(Stat stat, Inequality inequality, int threshold) {
		Instruction i = new Instruction();
		i.type = Type.WAIT;
		i.waitUntilCondition = new HasCondition(Player.ME, stat, inequality, threshold);
		return i;
	}
	
	private Instruction() {}
	
	public Instruction(Instruction other) {
		type = other.type;
		itemToUse = other.itemToUse;
		
		if (other.waitUntilCondition != null) {
			waitUntilCondition = new HasCondition(other.waitUntilCondition);
		}
	}
	
	public void validate() {
		if (type == null) {
			throw new InvalidBattleException("Instruction has unknown type");
		}
		
		if (type == Type.USE) {
			waitUntilCondition = null;
			if (itemToUse == null) {
				throw new InvalidBattleException("USE instruction is missing itemToUse");
			}
		}
		
		if (type == Type.WAIT) {
			itemToUse = null;
			if (waitUntilCondition == null) {
				throw new InvalidBattleException("WAIT instruction is missing waitUntilCondition");
			}
			waitUntilCondition.player = Player.ME;
			waitUntilCondition.validate();
		}
	}
	
}
