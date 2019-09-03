package com.snailscuffle.game.testutil;

import static java.lang.System.currentTimeMillis;

public class SyncUtil {
	
	public static <T> T waitForValue(int timeoutMillis, ThrowingSupplier<T> func) {
		T value = null;
		long startTime = currentTimeMillis();
		while (value == null && currentTimeMillis() - startTime < timeoutMillis) {
			try {
				Thread.sleep(10);
				value = func.get();
			} catch (Exception e) {
				value = null;
			}
		}
		return value;
	}
	
}
