package com.snailscuffle.game.testutil;

import static java.lang.System.currentTimeMillis;

public class SyncUtil {
	
	public static <T> T waitForValue(int timeoutMillis, ThrowingSupplier<T> func) throws InterruptedException {
		T value = null;
		long startTime = currentTimeMillis();
		while (value == null && currentTimeMillis() - startTime < timeoutMillis) {
			Thread.sleep(10);
			try {
				value = func.get();
			} catch (Exception e) {
				value = null;
			}
		}
		return value;
	}
	
}
