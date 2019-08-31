package com.snailscuffle.game.testutil;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U> {
	void accept(T t, U u) throws Exception;
}
