package com.snailscuffle.simulator;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.snailscuffle.game.battle.Battle;


class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	private static final String urlToConnectTo = "http://localhost:8080/battle";
	private static SimulatorSettings simulatorSettings = new SimulatorSettings();

	public static void main(String[] args) {
		
		RandomBattlePlanGen battlePlanGenerator = new RandomBattlePlanGen();	
		List<BattlePlan> generatedBattlePlans = battlePlanGenerator.getGeneratedBattlePlans();
				
		PostAndResponseStructure httpClient = new PostAndResponseStructure();	
		submitBattlePlansToGameServer(generatedBattlePlans, httpClient);
	}
	
	public static void submitBattlePlansToGameServer(List<BattlePlan> generatedBattlePlans, PostAndResponseStructure httpClient) {

		DatabaseUtil database = new DatabaseUtil();
		List<BattlePlan> battlePlans = new ArrayList<BattlePlan>();
		List<BattleResult> battleResults = new ArrayList<BattleResult>();
		String response;
		String jsonToSend;
		BattleResult result = null;
		int matchesProcessedCounter = 0;
		int totalMatchesProcessed = 0;
		int battleId = 0;
		
		for(int i = 0; i < generatedBattlePlans.size(); i++)
		{
			for(int j = 0; j < generatedBattlePlans.size();j++) {				
				try {						
						//Build a list that contains enough battle plans to ensure a battle always ends
						battlePlans.addAll(Arrays.asList(generatedBattlePlans.get(i), generatedBattlePlans.get(j),
								generatedBattlePlans.get(i), generatedBattlePlans.get(j),
								generatedBattlePlans.get(i), generatedBattlePlans.get(j),
								generatedBattlePlans.get(i), generatedBattlePlans.get(j),
								generatedBattlePlans.get(i), generatedBattlePlans.get(j),
								generatedBattlePlans.get(i), generatedBattlePlans.get(j)));
						
						int winnerFound = -1;
						while(winnerFound < 0) {
							if (simulatorSettings.goOverNetwork) {									
								BattleConfig battleConfig = new BattleConfig(battlePlans.toArray(new BattlePlan[0]));
								jsonToSend = JsonUtil.serialize(battleConfig);
								response = httpClient.postMessage(urlToConnectTo, jsonToSend);
								result = JsonUtil.deserialize(BattleResult.class, response);									
								battleResults.add(result);
								winnerFound = result.winnerIndex;
								battlePlans.addAll(Arrays.asList(generatedBattlePlans.get(i), generatedBattlePlans.get(j)));
							} else {									
								//For now just keep sending the same one																
								BattleConfig battleConfig = new BattleConfig(battlePlans.toArray(new BattlePlan[0]));
								result = (new Battle(battleConfig)).getResult();
								battleResults.add(result);
								winnerFound = result.winnerIndex;
								battlePlans.addAll(Arrays.asList(generatedBattlePlans.get(i), generatedBattlePlans.get(j)));
							}							
						}

					BattleResultMetaData metaData = new BattleResultMetaData(battleResults);
					database.StoreResult(Arrays.asList(generatedBattlePlans.get(i), generatedBattlePlans.get(j)), metaData, battleId);

					totalMatchesProcessed++;
					matchesProcessedCounter++;
					battleId++;
					battlePlans.clear();
					battleResults.clear();
					
					if (matchesProcessedCounter >= 1000000) {
						logger.debug("Processed " + totalMatchesProcessed + "/" + (generatedBattlePlans.size() * generatedBattlePlans.size()) + " matches.");
						matchesProcessedCounter = 0;
					}
					
				} catch (Exception e) {
					logger.error("Error when trying to simulate battle. Error message: " + e.getMessage());
					for(BattlePlan battlePlan : battlePlans)
						logger.debug(battlePlan.toString());			
				}							
			}		
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
