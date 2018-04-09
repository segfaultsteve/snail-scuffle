package com.snailscuffle.common.battle;

public class Constants {
	
	// battle
	public static final int INITIAL_HP = 100;
	public static final int INITIAL_AP = 0;
	
	// snails
	public static final int DALE_ATTACK = 6;
	public static final int DALE_DEFENSE = 6;
	public static final int DALE_SPEED = 9;
	
	public static final int GAIL_ATTACK = 7;
	public static final int GAIL_DEFENSE = 8;
	public static final int GAIL_SPEED = 6;
	
	public static final int TODD_ATTACK = 6;
	public static final int TODD_DEFENSE = 7;
	public static final int TODD_SPEED = 7;
	
	public static final int DOUG_ATTACK = 9;
	public static final int DOUG_DEFENSE = 4;
	public static final int DOUG_SPEED = 7;
	
	// weapons
	public static final int RIFLE_ATTACK = 4;
	public static final int ROCKET_ATTACK = 7;
	public static final int LASER_ATTACK = 9;
	
	public static final int RIFLE_AP_COST = 10;
	public static final int ROCKET_AP_COST = 15;
	public static final int LASER_AP_COST = 18;
	
	// shells
	public static final int ALUMINUM_DEFENSE = 2;
	public static final int ALUMINUM_SPEED = 2;
	public static final int STEEL_DEFENSE = 5;
	public static final int STEEL_SPEED = -3;
	
	// items
	public static final double ATTACK_BOOST_MULTIPLIER = 1.3;
	public static final int ATTACK_BOOST_DURATION = 3000;
	public static final double DEFENSE_BOOST_MULTIPLIER = 1.4;
	public static final int DEFENSE_BOOST_DURATION = 5000;
	public static final int SPEED_BOOST_AP_INCREASE = 10;
	
	// accessories
	public static final int STEROIDS_ATTACK = 4;
	public static final int SNAIL_MAIL_DEFENSE = 4;
	public static final int CAFFEINE_SPEED = 4;
	public static final int CHARGED_ATTACK_AP_DIVISOR = 50000;			// Attack += Attack * AP / CHARGED_ATTACK_AP_DIVISOR
	public static final int ADRENALINE_CROSSOVER = 70000;				// Attack += (ADRENALINE_CROSSOVER - HP) / ADRENALINE_DIVISOR
	public static final int ADRENALINE_DIVISOR = 10;
	public static final int SALTED_SHELL_ATTACK_MULTIPLIER = 2;			// Attack *= SALTED_SHELL_ATTACK_MULTIPLIER
	public static final double SALTED_SHELL_DEFENSE_MULTIPLIER = 0.5;	// Defense *= SALTED_SHELL_DEFENSE_MULTIPLIER
	public static final double THORNS_DAMAGE_MULTIPLIER = 0.1;			// damage to attacker = THORNS_DAMAGE_MULTIPLIER * (damage taken)

}
