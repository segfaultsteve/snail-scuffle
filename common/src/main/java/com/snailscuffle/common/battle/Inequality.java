package com.snailscuffle.common.battle;

public enum Inequality {
	LESS_THAN_OR_EQUALS("leq"),
	GREATER_THAN_OR_EQUALS("geq");
	
	private String serialization;
	
	private Inequality(String serializeAs) {
		this.serialization = serializeAs;
	}
	
	public boolean evaluate(int lhs, int rhs) {
		if (this == LESS_THAN_OR_EQUALS) {
			return lhs <= rhs;
		}
		return lhs >= rhs;
	}
	
	@Override
	public String toString() {
		return serialization;
	}
}
