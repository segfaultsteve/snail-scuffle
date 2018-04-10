package com.snailscuffle.simulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.snailscuffle.common.battle.*;

public class RandomBattlePlanGen {
	
	private List<List<Integer>> battlePlans = new ArrayList<List<Integer>>();
	private List<BattlePlan> generatedBattlePlans = new ArrayList<BattlePlan>();
	private List<String> permutations = new ArrayList<String>();
	
	public RandomBattlePlanGen() {
		
		List<Integer> weaponOrdinalValues = new ArrayList<Integer>();
		for(Weapon w : Weapon.values()) {
			weaponOrdinalValues.add(w.ordinal());
		}
		battlePlans.add(weaponOrdinalValues);
		
		List<Integer> accessoryOrdinalValues = new ArrayList<Integer>();
		for(Accessory a : Accessory.values()) {
			accessoryOrdinalValues.add(a.ordinal());
		}
		battlePlans.add(accessoryOrdinalValues);
		
		List<Integer> item1OrdinalValues = new ArrayList<Integer>();
		for(Item i : Item.values()) {
			item1OrdinalValues.add(i.ordinal());
		}
		battlePlans.add(item1OrdinalValues);
		
		List<Integer> item2OrdinalValues = new ArrayList<Integer>();
		for(Item i : Item.values()) {
			item2OrdinalValues.add(i.ordinal());
		}
		battlePlans.add(item2OrdinalValues);

		List<Integer> shellOrdinalValues = new ArrayList<Integer>();
		for(Shell sh : Shell.values()) {
			shellOrdinalValues.add(sh.ordinal());
		}
		battlePlans.add(shellOrdinalValues);
		
		List<Integer> snailOrdinalValues = new ArrayList<Integer>();
		for(Snail sn : Snail.values()) {
			snailOrdinalValues.add(sn.ordinal());
		}
		battlePlans.add(snailOrdinalValues);
		
		generateBattlePlans();
	}
	
	public RandomBattlePlanGen(Weapon weapon, Accessory accessory, Item item1, Item item2, Shell shell, Snail snail) {
		
		// Fill out list containing what we want to simulate
		if (weapon != null) {
			List<Integer> weaponOrdinalValues = new ArrayList<Integer>();
			for(Weapon w : Weapon.values()) {
				weaponOrdinalValues.add(w.ordinal());
			}
			battlePlans.add(weaponOrdinalValues);
		}
		
		if (accessory != null) {
			List<Integer> accessoryOrdinalValues = new ArrayList<Integer>();
			for(Accessory a : Accessory.values()) {
				accessoryOrdinalValues.add(a.ordinal());
			}
			battlePlans.add(accessoryOrdinalValues);
		}
		
		if (item1 != null) {
			List<Integer> itemOrdinalValues = new ArrayList<Integer>();
			for(Item i : Item.values()) {
				itemOrdinalValues.add(i.ordinal());
			}
			battlePlans.add(itemOrdinalValues);
		}
		
		if (item2 != null) {
			List<Integer> itemOrdinalValues = new ArrayList<Integer>();
			for(Item i : Item.values()) {
				itemOrdinalValues.add(i.ordinal());
			}
			battlePlans.add(itemOrdinalValues);
		}
			
		if (shell != null) {
			List<Integer> shellOrdinalValues = new ArrayList<Integer>();
			for(Shell sh : Shell.values()) {
				shellOrdinalValues.add(sh.ordinal());
			}
			battlePlans.add(shellOrdinalValues);
		}
		
		if (snail != null) {
			List<Integer> snailOrdinalValues = new ArrayList<Integer>();
			for(Snail sn : Snail.values()) {
				snailOrdinalValues.add(sn.ordinal());
			}
			battlePlans.add(snailOrdinalValues);
		}	
		
		generateBattlePlans();
	}
	
	public List<BattlePlan> getGeneratedBattlePlans(){
		return generatedBattlePlans;
	}

	private void generateBattlePlans(){
		generateBattlePlanPermutations(battlePlans, permutations, 0, "");
		buildBattlePlans(permutations);
	}

	private void generateBattlePlanPermutations(List<List<Integer>> listOfBattlePlanElements, List<String> permutations, int depth, String current) {
		
		if (depth != 0 && depth != listOfBattlePlanElements.size())
			current += ",";
		
		if(depth == listOfBattlePlanElements.size())
	    {
			permutations.add(current);
	       return;
	     }

	    for(int i = 0; i < listOfBattlePlanElements.get(depth).size(); ++i)
	    {    	
	    	generateBattlePlanPermutations(listOfBattlePlanElements, permutations, depth + 1, current + listOfBattlePlanElements.get(depth).get(i));
	    }	
	}
	
	private void buildBattlePlans(List<String> permutationStrings) {
		
		for(String permutation : permutationStrings) {
			
			BattlePlan bp = new BattlePlan();
			int[] parsedString = parsePermutationString(permutation);
			
			bp.weapon = Weapon.values()[parsedString[0]];
			bp.accessory = Accessory.values()[parsedString[1]];
			bp.item1 = Item.values()[parsedString[2]];
			bp.item2 = Item.values()[parsedString[3]];
			bp.shell = Shell.values()[parsedString[4]];
			bp.snail = Snail.values()[parsedString[5]];
					
			//TODO: Figure out how we want to implement these. Below is just placeholder data 
			bp.item1Rule = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 40);
			bp.item2Rule = ItemRule.useWhenIHave(Stat.AP, Inequality.LESS_THAN_OR_EQUALS, 20);
			
			Instruction instruction1 = Instruction.attack();		
			bp.instructions = new ArrayList<Instruction>();
			bp.instructions.add(instruction1);
						
			generatedBattlePlans.add(bp);
		}
	}
	
	private int[] parsePermutationString(String permutationString){
		int[] numbers = Arrays.stream(permutationString.split(",")).mapToInt(Integer::parseInt).toArray(); 
		return numbers;
	}
}
