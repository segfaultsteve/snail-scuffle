package com.snailscuffle.simulator;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.snailscuffle.common.battle.*;
import com.snailscuffle.common.util.JsonUtil;

class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	private static final String urlToConnectTo = "http://localhost:8080/battle";

	public static void main(String[] args) {
		
		RandomBattlePlanGen battlePlanGenerator = new RandomBattlePlanGen();	
		List<BattlePlan> generatedBattlePlans = battlePlanGenerator.getGeneratedBattlePlans();
				
		PostAndResponseStructure httpClient = new PostAndResponseStructure();	
		submitBattlePlansToGameServer(generatedBattlePlans, httpClient);
	}
	
	public static void submitBattlePlansToGameServer(List<BattlePlan> generatedBattlePlans, PostAndResponseStructure httpClient) {
		
		Collections.shuffle(generatedBattlePlans); // Shuffle to get more diverse match-ups
		
		try {
			for(int i = 0; i < generatedBattlePlans.size(); i+=2)
			{
				String response;
				try {
					String jsonToSend = JsonUtil.serialize(new BattleConfig(generatedBattlePlans.get(i), generatedBattlePlans.get(i+1),
																			generatedBattlePlans.get(i), generatedBattlePlans.get(i+1) ));
					response = httpClient.postMessage(urlToConnectTo, jsonToSend);
					logger.debug(response);
				} catch (Exception e) {
					logger.error("Error when trying to POST battle plans. Error message: " + e.getMessage());
				}
			}
		} catch (ArrayIndexOutOfBoundsException e){
			//log some kind of message
		}
	}
	
	public static class PostAndResponseStructure {
		final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
		
		OkHttpClient client = new OkHttpClient();
		
		String postMessage(String url, String json) throws Exception {
			RequestBody body = RequestBody.create(JSON, json);
			Request request = new Request.Builder()
			        .url(url)
			        .post(body)
			        .build();
			    try (Response response = client.newCall(request).execute()) {
			      return response.body().string();
			}
		}					
	}
}
