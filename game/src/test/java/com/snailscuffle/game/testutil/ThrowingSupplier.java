package com.snailscuffle.game.testutil;

@FunctionalInterface
public interface ThrowingSupplier<T> {
	T get() throws Exception;
}
