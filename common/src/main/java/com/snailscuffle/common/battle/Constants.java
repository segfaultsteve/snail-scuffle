package com.snailscuffle.common.battle;

public class Constants {
	
	// battle
	public static final int INITIAL_HP = 100;
	public static final int INITIAL_AP = 0;
	
	// snails
	public static final String DALE_DISPLAY_NAME = "Dale";
	public static final String DALE_DESCRIPTION = "The Speedy Snail";
	public static final int DALE_ATTACK = 6;
	public static final int DALE_DEFENSE = 6;
	public static final int DALE_SPEED = 8;
	
	public static final String GAIL_DISPLAY_NAME = "Gail";
	public static final String GAIL_DESCRIPTION = "The Upscale Snail";
	public static final int GAIL_ATTACK = 6;
	public static final int GAIL_DEFENSE = 8;
	public static final int GAIL_SPEED = 6;
	
	public static final String TODD_DISPLAY_NAME = "Todd";
	public static final String TODD_DESCRIPTION = "The Odd Gastropod";
	public static final int TODD_ATTACK = 6;
	public static final int TODD_DEFENSE = 7;
	public static final int TODD_SPEED = 7;
	
	public static final String DOUG_DISPLAY_NAME = "Doug";
	public static final String DOUG_DESCRIPTION = "The Thug Slug";
	public static final int DOUG_ATTACK = 9;
	public static final int DOUG_DEFENSE = 6;
	public static final int DOUG_SPEED = 7;
	
	// weapons
	public static final String RIFLE_DISPLAY_NAME = "A Salt Rifle";
	public static final int RIFLE_ATTACK = 4;
	public static final int RIFLE_AP_COST = 10;
	
	public static final String ROCKET_DISPLAY_NAME = "Rocket Launcher";
	public static final int ROCKET_ATTACK = 7;
	public static final int ROCKET_AP_COST = 15;
	
	public static final String LASER_DISPLAY_NAME = "Laser";
	public static final int LASER_ATTACK = 9;
	public static final int LASER_AP_COST = 18;
	
	public static final String LEAKY_RADIOACTIVE_WATERGUN_DISPLAY_NAME = "Irriadiated Water Gun";
	// Need to add descriptions to explain the effect
	public static final int LEAKY_RADIOACTIVE_WATERGUN_SELF_DAMAGE = 3;
	public static final int LEAKY_RADIOACTIVE_WATERGUN_ATTACK = 9;
	public static final int LEAKY_RADIOACTIVE_WATERGUN_AP_COST = 13;
	
	public static final String ENEMY_TEAR_COLLECTOR_DISPLAY_NAME = "Enemy Tear Collector";
	// Need to add descriptions to explain the effect
	public static final int ENEMY_TEAR_COLLECTOR_SELF_HEAL = 5;
	public static final int ENEMY_TEAR_COLLECTOR_ATTACK = 4;
	public static final int ENEMY_TEAR_COLLECTOR_AP_COST = 13;
	
	// shells
	public static final String ALUMINUM_DISPLAY_NAME = "Aluminum";
	public static final int ALUMINUM_DEFENSE = 3;
	public static final int ALUMINUM_SPEED = -1;
	
	public static final String STEEL_DISPLAY_NAME = "Steel";
	public static final int STEEL_DEFENSE = 6;
	public static final int STEEL_SPEED = -3;
	
	public static final String NO_SHELL_DISPLAY_NAME = "No Shell";
	
	// accessories
	public static final String STEROIDS_DISPLAY_NAME = "Steroids";
	public static final String STEROIDS_DESCRIPTION = "Increases Attack";
	public static final int STEROIDS_ATTACK = 5;
	
	public static final String SNAIL_MAIL_DISPLAY_NAME = "Snail Mail";
	public static final String SNAIL_MAIL_DESCRIPTION = "Increases Defense";
	public static final int SNAIL_MAIL_DEFENSE = 3;
	
	public static final String CAFFEINE_DISPLAY_NAME = "Caffeine";
	public static final String CAFFEINE_DESCRIPTION = "Increases Speed";
	public static final int CAFFEINE_SPEED = 4;
	
	public static final String CHARGED_ATTACK_DISPLAY_NAME = "Charged Attack";
	public static final String CHARGED_ATTACK_DESCRIPTION = "Increases Attack as AP increases";
	public static final int CHARGED_ATTACK_AP_DIVISOR = 50;				// Attack += Attack * AP / CHARGED_ATTACK_AP_DIVISOR
	
	public static final String ADRENALINE_DISPLAY_NAME = "Adrenaline";
	public static final String ADRENALINE_DESCRIPTION = "Increases Attack as HP decreases";
	public static final int ADRENALINE_CROSSOVER = 70;					// Attack += (ADRENALINE_CROSSOVER - HP) / ADRENALINE_DIVISOR
	public static final int ADRENALINE_DIVISOR = 10;
	
	public static final String SALTED_SHELL_DISPLAY_NAME = "Salted Shell";
	public static final String SALTED_SHELL_DESCRIPTION = "Halves Defense for the first round it is equipped, then doubles Attack in subsequent rounds";
	public static final int SALTED_SHELL_ATTACK_MULTIPLIER = 2;			// Attack *= SALTED_SHELL_ATTACK_MULTIPLIER
	public static final double SALTED_SHELL_DEFENSE_MULTIPLIER = 0.5;	// Defense *= SALTED_SHELL_DEFENSE_MULTIPLIER
	
	public static final String THORNS_DISPLAY_NAME = "Thorns";
	public static final String THORNS_DESCRIPTION = "Does 20% of damage taken back to enemy";
	public static final double THORNS_DAMAGE_MULTIPLIER = 0.2;			// damage to attacker = THORNS_DAMAGE_MULTIPLIER * (damage taken)
	
	public static final String DEFIBRILLATOR_DISPLAY_NAME = "Defibrillator";
	public static final String DEFIBRILLATOR_DESCRIPTION = "Mortal blow leaves 1 HP remaining";
	
	public static final String NO_ACCESSORY_DISPLAY_NAME = "No Accessory";
	
	// items
	public static final double ATTACK_BOOST_MULTIPLIER = 1.4;
	public static final String ATTACK_BOOST_DISPLAY_NAME = "Attack Boost";
	public static final String ATTACK_BOOST_DESCRIPTION = "Temporarily increases Attack by " + Math.round((ATTACK_BOOST_MULTIPLIER - 1) * 100) + "%";		
	public static final int ATTACK_BOOST_DURATION = 5000;
	
	public static final double DEFENSE_BOOST_MULTIPLIER = 1.4;
	public static final String DEFENSE_BOOST_DISPLAY_NAME = "Defense Boost";
	public static final String DEFENSE_BOOST_DESCRIPTION = "Temporarily increases Defense by " + Math.round((DEFENSE_BOOST_MULTIPLIER - 1) * 100) + "%";	
	public static final int DEFENSE_BOOST_DURATION = 5000;
	
	public static final String SPEED_BOOST_DISPLAY_NAME = "Speed Boost";
	public static final String SPEED_BOOST_DESCRIPTION = "Increases AP by 20";
	public static final int SPEED_BOOST_AP_INCREASE = 20;
	
	public static final String STUN_DISPLAY_NAME = "Stun";
	public static final String STUN_DESCRIPTION = "Temporarily prevents enemy from attacking, defending, using an item, or accumulating AP";
	public static final int STUN_DURATION = 2000;
	
	public static final int HEAL_AMOUNT = 45;
	public static final String HEAL_DISPLAY_NAME = "Heal";	
	public static final String HEAL_DESCRIPTION = "Increases HP by " + HEAL_AMOUNT;
	
	
	public static final String NO_ITEM_DISPLAY_NAME = "No Item";

}
