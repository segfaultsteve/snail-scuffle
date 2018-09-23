var snail = (function (snail, $) {
	snail.model = snail.model || {};
	snail.model.battle = snail.model.battle || {};
	
	// private variables
	let ioLayer, skirmishId, round, skirmishPollInterval;
	let eventHandlers = [];
	
	// private methods
	const newBattleData = function (playerName, opponentName) {
		const battleData = {
			time: 0,
			playerName: playerName,
			playerHp: 100,
			playerAp: 0,
			playerSpeed: 0,
			opponentName: opponentName,
			opponentHp: 100,
			opponentAp: 0,
			opponentSpeed: 0
		};
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
		const battleData = newBattleData(skirmishResponse.playerName, skirmishResponse.opponentName);
		notifyEventHandlers('startBattle', battleData);
	};
	
	const putBattlePlanToMatchmaker = function (battlePlan) {
		return function (skirmishId) {
			return ioLayer.putBattlePlan(skirmishId, battlePlan);
		};
	};
	
	const getBattlePlansForAllRounds = function (state) {
		return function (skirmishResponse) {
			state.skirmishResponse = skirmishResponse;
			const bps = $.Deferred();
			if (skirmishResponse.battlePlans.length > 2*round) {
				bps.resolve(skirmishResponse.battlePlans);
			} else {
				skirmishPollInterval = setInterval(pollMatchmakerForBattlePlans, 2000, skirmishResponse.skirmishId, bps);
			}
			return bps;
		};
	};
	
	const pollMatchmakerForBattlePlans = function (skirmishId, deferredBattlePlans) {
		ioLayer.getSkirmish(skirmishId)
			.done(function (skirmishResponse) {
				if (skirmishResponse.battlePlans.length > 2*round) {
					clearInterval(skirmishPollInterval);
					deferredBattlePlans.resolve(skirmishResponse.battlePlans);
				}
			});
	};
	
	const saveBattlePlansToModel = function (bps) {
		snail.model.battleplan.playerBp.set(bps.slice(-2)[0]);
		snail.model.battleplan.opponentBp.set(bps.slice(-2)[1]);
	};
	
	const postBattlePlansToGameServer = function (state) {
		return function (bps) {
			state.battlePlans = bps;
			return ioLayer.postBattle(bps);
		}
	};
	
	const playNextRound = function (state) {
		return function (battleResult) {
			const args = {
				battleData: runBattleUpToLastRound(state.skirmishResponse, battleResult),
				events: battleResult.eventsByRound.slice(-1)
			};
			notifyEventHandlers('startRound', args);
		};
	};
	
	const runBattleUpToLastRound = function (skirmishResponse, battleResult) {
		const battleData = newBattleData(skirmishResponse.playerName, skirmishResponse.opponentName);
		for (let i = 0; i < battleResult.eventsByRound.length - 1; i++) {
			const eventsForThisRound = battleResult.eventsByRound[i];
			for (let j = 0; j < eventsForThisRound.length; j++) {
				const event = eventsForThisRound[j];
				applyEvent(event, battleData);
			}
		}
		return battleData;
	};
	
	const applyEvent = function (event, battleData) {
		if (event.action === 'attack') {
			for (let i = 0; i < event.effects.length; i++) {
				const effect = event.effects[i];
				if (effect.stat === 'hp') {
					if (effect.playerIndex === 0) {
						battleData.playerHp += effect.change;
					} else {
						battleData.opponentHp += effect.change;
					}
				}
			}
		} else if (event.action === 'use_item') {
			// TODO
		}
		
		if (battleData.playerHp < 0) {
			battleData.playerHp = 0;
		} else if (battleData.opponentHp < 0) {
			battleData.opponentHp = 0;
		}
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
		
		ioLayer.putSkirmish()
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
		// TODO: create 'cancel' endpoint on server (maybe DELETE handler?),
		// then send ajax here
	};
	
	snail.model.battle.submitBattlePlan = function (bp) {
		notifyEventHandlers('battlePlanSubmitted', bp);
		
		const state = {};
		
		skirmishId
			.then(putBattlePlanToMatchmaker(bp))
			.then(getBattlePlansForAllRounds(state))
			.then(saveBattlePlansToModel)
			.then(postBattlePlansToGameServer(state))
			.then(playNextRound(state));
	};
	
	return snail;
}(snail || {}, jQuery));
