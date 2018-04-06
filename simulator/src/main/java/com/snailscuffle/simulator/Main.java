package com.snailscuffle.simulator;

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
		SimulatedBattle battle = new SimulatedBattle();
		BattlePlan battlePlan1 = battle.parseBattlePlan();
		BattlePlan battlePlan2 = battle.parseBattlePlan();
		String response;
		try {
			String jsonToSend = JsonUtil.serialize(new BattleConfig(battlePlan1, battlePlan2));
			response = battle.postMessage(urlToConnectTo, jsonToSend);
			logger.debug(response);
		} catch (Exception e) {
			logger.error("Error when trying to POST battle plans. Error message: " + e.getMessage());
		}
	}
	
	public static class SimulatedBattle {
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
		
		public static BattlePlan parseBattlePlan() {
			BattlePlan battlePlan = new BattlePlan();
			
			battlePlan.snail = Snail.DALE;
			battlePlan.weapon = Weapon.ROCKET;
			battlePlan.shell = Shell.ALUMINUM;
			battlePlan.accessory = Accessory.ADRENALINE;
			battlePlan.item1 = Item.ATTACK;
			battlePlan.item2 = Item.DEFENSE;
			battlePlan.item1Rule = ItemRule.useWhenEnemyHas(Stat.AP, Inequality.GREATER_THAN_OR_EQUALS, 40);
			battlePlan.item2Rule = ItemRule.useWhenIHave(Stat.AP, Inequality.LESS_THAN_OR_EQUALS, 20);
			
			Instruction instruction1 = new Instruction();
			instruction1.type = Instruction.Type.ATTACK;

			Instruction instruction2 = new Instruction();
			instruction2.type = Instruction.Type.WAIT;
			instruction2.waitUntilCondition = new HasCondition(Player.ME, Stat.ATTACK, Inequality.LESS_THAN_OR_EQUALS, 30);
			
			return battlePlan;
		}			
	}
}
