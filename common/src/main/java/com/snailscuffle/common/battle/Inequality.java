package com.snailscuffle.common.battle;

public enum Inequality {
	LESS_THAN_OR_EQUALS("leq"),
	GREATER_THAN_OR_EQUALS("geq");
	
	private String serialization;
	
	private Inequality(String serializeAs) {
		this.serialization = serializeAs;
	}
	
	@Override
	public String toString() {
		return serialization;
	}
}
