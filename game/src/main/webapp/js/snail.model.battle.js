var snail = (function (snail, $) {
	snail.model = snail.model || {};
	snail.model.battle = snail.model.battle || {};
	
	// private variables
	let ioLayer, skirmishId, skirmishPollInterval, round, endOfCurrentRoundBattleData;
	let eventHandlers = [];
	
	// private methods
	const newBattleData = function (skirmishResponse, stats) {
		let playerName = skirmishResponse.playerName;
		let enemyName = skirmishResponse.opponentName;
		
		if (playerName === 'Guest') {
			playerName = 'You';
		}
		
		if (enemyName === 'Guest') {
			enemyName = 'Enemy';
		}
		
		const battleData = {
			time: 0,
			names: [playerName, enemyName],
			hp: [100, 100],
			ap: [0, 0],
			endOfRound: 0
		};
		
		if (stats) {
			battleData.endOfRound = stats[0].time * (round+1);
			if (round > 0) {
				const lastRound = stats[round-1];
				battleData.time = lastRound.time;
				battleData.hp = [lastRound.player1Hp, lastRound.player2Hp];
				battleData.ap = [lastRound.player1Ap, lastRound.player2Ap];
			}
		}
		
		return battleData;
	};
	
	const pollMatchmakerUntilMatched = function () {
		skirmishId
			.then(function (id) {
				return ioLayer.getSkirmish(id);
			})
			.done(function (skirmishResponse) {
				if (skirmishResponse.opponentName) {
					clearInterval(skirmishPollInterval);
					signalStartOfBattle(skirmishResponse);
				}
			});
	};
	
	const signalStartOfBattle = function (skirmishResponse) {
		const battleData = newBattleData(skirmishResponse);
		notifyEventHandlers('battleStarted', battleData);
	};
	
	const putBattlePlanToMatchmaker = function (battlePlan) {
		return function (skirmishId) {
			return ioLayer.putBattlePlan(skirmishId, battlePlan);
		};
	};
	
	const getDataForNextRound = function (skirmishResponse) {
		const response = $.Deferred();
		if (skirmishResponse.battlePlans.length > 2*round) {
			response.resolve(skirmishResponse);
		} else {
			skirmishPollInterval = setInterval(pollMatchmakerForBattlePlans, 2000, skirmishResponse.skirmishId, response);
		}
		return response;
	};
	
	const pollMatchmakerForBattlePlans = function (skirmishId, deferredSkirmishResponse) {
		ioLayer.getSkirmish(skirmishId)
			.done(function (skirmishResponse) {
				if (skirmishResponse.battlePlans.length > 2*round) {
					clearInterval(skirmishPollInterval);
					deferredSkirmishResponse.resolve(skirmishResponse);
				}
			});
	};
	
	const saveSkirmishResponseToState = function (state) {
		return function (skirmishResponse) {
			state.skirmishResponse = skirmishResponse;
			return skirmishResponse;
		};
	}
	
	const saveBattlePlansToModel = function (skirmishResponse) {
		snail.model.battleplan.playerBp.set(skirmishResponse.battlePlans.slice(-2)[0]);
		snail.model.battleplan.enemyBp.set(skirmishResponse.battlePlans.slice(-2)[1]);
		return skirmishResponse;
	};
	
	const postBattlePlansToGameServer = function (skirmishResponse) {
		return ioLayer.postBattle(skirmishResponse.battlePlans, skirmishResponse.firstMover);
	};
	
	const playNextRound = function (state) {
		return function (battleResult) {
			const args = {
				battleData: newBattleData(state.skirmishResponse, battleResult.endOfRoundStats),
				events: battleResult.eventsByRound.slice(-1)[0]
			};
			notifyEventHandlers('nextRound', args);
			
			round += 1;
			endOfCurrentRoundBattleData = newBattleData(state.skirmishResponse, battleResult.endOfRoundStats);
		};
	};
	
	const notifyEventHandlers = function (event, args) {
		eventHandlers.forEach(handler => handler(event, args));
	};
	
	// public methods
	snail.model.battle.init = function (io) {
		ioLayer = io;
	};
	
	snail.model.battle.addEventHandler = function (handler) {
		eventHandlers.push(handler);
	};
	
	snail.model.battle.startSkirmish = function () {
		round = 0;
		skirmishId = $.Deferred();
		
		ioLayer.postSkirmish()
			.done(function (skirmishResponse) {
				skirmishId.resolve(skirmishResponse.skirmishId);
				if (skirmishResponse.opponentName) {
					signalStartOfBattle(skirmishResponse);
				} else {
					skirmishPollInterval = setInterval(pollMatchmakerUntilMatched, 2000);
				}
			});
	};
	
	snail.model.battle.cancelSkirmish = function () {
		clearInterval(skirmishPollInterval);
		ioLayer.deleteSkirmish();
	};
	
	snail.model.battle.submitBattlePlan = function (bp) {
		notifyEventHandlers('battlePlanSubmitted', bp);
		
		const state = {};
		
		skirmishId
			.then(putBattlePlanToMatchmaker(bp))
			.then(getDataForNextRound)
			.then(saveSkirmishResponseToState(state))
			.then(saveBattlePlansToModel)
			.then(postBattlePlansToGameServer)
			.then(playNextRound(state));
	};
	
	snail.model.battle.finishRound = function () {
		notifyEventHandlers('roundComplete', endOfCurrentRoundBattleData);
	};
	
	snail.model.battle.finishBattle = function () {
		notifyEventHandlers('battleComplete', endOfCurrentRoundBattleData);
	};
	
	return snail;
}(snail || {}, jQuery));
