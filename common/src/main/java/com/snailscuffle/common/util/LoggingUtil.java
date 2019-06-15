package com.snailscuffle.common.util;

import org.eclipse.jetty.server.NCSARequestLog;

public class LoggingUtil {
	
	public static NCSARequestLog createRequestLog() {
		NCSARequestLog requestLog = new NCSARequestLog("yyyy_mm_dd.request.log");
		requestLog.setAppend(true);
		requestLog.setExtended(false);
		requestLog.setLogTimeZone("GMT");
		requestLog.setLogLatency(true);
		requestLog.setRetainDays(90);
		return requestLog;
	}
	
}
