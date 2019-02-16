var snail = (function (snail, $) {
	snail.model = snail.model || {};
	snail.model.battle = snail.model.battle || {};
	
	// private variables
	let ioLayer, accessoryInfo, skirmishId, skirmishPollInterval, skirmishTimerInterval, round, endOfCurrentRoundBattleData;
	let eventHandlers = [];
	let playerBps = [];
	
	// private methods
	const newBattleData = function (skirmishResponse, battleResult) {
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
			effects: [[],[]],
			endOfRound: 0,
			winnerIndex: -1
		};
		
		if (battleResult) {
			battleData.endOfRound = battleResult.endOfRoundStats[0].time * (round+1);
			battleData.winnerIndex = battleResult.winnerIndex;
			if (round > 0) {
				const lastRound = battleResult.endOfRoundStats[round-1];
				battleData.time = lastRound.time;
				battleData.hp = lastRound.players.map(p => p.hp);
				battleData.ap = lastRound.players.map(p => p.ap);
				battleData.effects = lastRound.players.map(p => p.activeEffects);
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
		startTimer(skirmishResponse.timeRemaining);
	};
	
	const startTimer = function (initialMillis) {
		let timeRemaining = initialMillis;
		notifyEventHandlers('updateTimeRemaining', timeRemaining);
		skirmishTimerInterval = setInterval(function () {
			timeRemaining = Math.max(timeRemaining - 1000, 0);
			notifyEventHandlers('updateTimeRemaining', timeRemaining);
			if (timeRemaining === 0) {
				clearInterval(skirmishTimerInterval);
				notifyEventHandlers('outOfTime');
			}
		}, 1000);
	};
	
	const putBattlePlanToMatchmaker = function (battlePlan) {
		return function (skirmishId) {
			return ioLayer.putBattlePlan(skirmishId, battlePlan);
		};
	};
	
	const getDataForNextRound = function (skirmishResponse) {
		const response = $.Deferred();
		if (skirmishResponse.opponentHasForfeited || skirmishResponse.battlePlans.length > 2*round) {
			response.resolve(skirmishResponse);
		} else {
			skirmishPollInterval = setInterval(pollMatchmakerForBattlePlans, 2000, skirmishResponse.skirmishId, response);
		}
		return response;
	};
	
	const pollMatchmakerForBattlePlans = function (skirmishId, deferredSkirmishResponse) {
		ioLayer.getSkirmish(skirmishId)
			.done(function (skirmishResponse) {
				if (skirmishResponse.opponentHasForfeited || skirmishResponse.battlePlans.length > 2*round) {
					clearInterval(skirmishPollInterval);
					deferredSkirmishResponse.resolve(skirmishResponse);
				}
			});
	};
	
	const checkForForfeit = function (skirmishResponse) {
		if (skirmishResponse.opponentHasForfeited) {
			notifyEventHandlers('opponentHasForfeited');
			return $.Deferred().reject();
		} else {
			return skirmishResponse;
		}
	};
	
	const saveSkirmishResponseToState = function (state) {
		return function (skirmishResponse) {
			state.skirmishResponse = skirmishResponse;
			return skirmishResponse;
		};
	}
	
	const saveBattlePlansToModel = function (skirmishResponse) {
		playerBps.push(skirmishResponse.battlePlans.slice(-2)[0]);
		snail.model.battleplan.enemyBp.set(skirmishResponse.battlePlans.slice(-2)[1]);
		return skirmishResponse;
	};
	
	const postBattlePlansToGameServer = function (skirmishResponse) {
		return ioLayer.postBattle(skirmishResponse.battlePlans, skirmishResponse.firstMover);
	};
	
	const playNextRound = function (state) {
		return function (battleResult) {
			const args = {
				battleData: newBattleData(state.skirmishResponse, battleResult),
				events: battleResult.eventsByRound.slice(-1)[0]
			};
			notifyEventHandlers('nextRound', args);
			
			if (battleResult.winnerIndex < 0) {
				startTimer(state.skirmishResponse.timeRemaining);
			}
			
			round += 1;
			endOfCurrentRoundBattleData = newBattleData(state.skirmishResponse, battleResult);
		};
	};
	
	const notifyEventHandlers = function (event, args) {
		eventHandlers.forEach(handler => handler(event, args));
	};
	
	// public methods
	snail.model.battle.init = function (io) {
		ioLayer = io;
		ioLayer.promiseAccessoryInfo().done(info => accessoryInfo = info);
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
	
	snail.model.battle.saltedShellAttackMultiplier = function () {
		return (round === 0 || playerBps[round-1].accessory !== 'salted_shell') ? 1 : 2;
	};
	
	snail.model.battle.saltedShellDefenseMultiplier = function () {
		return (round === 0 || playerBps[round-1].accessory !== 'salted_shell') ? 0.5 : 1;
	};
	
	snail.model.battle.chargedAttackModifier = function () {
		if (round === 0 || !accessoryInfo) {
			return 0;
		} else {
			const currentAp = endOfCurrentRoundBattleData.ap[0];
			const divisor = accessoryInfo.filter(i => i.name === 'charged_attack')[0].other.divisor;
			return currentAp / divisor;
		}
	};
	
	snail.model.battle.adrenalineModifier = function () {
		if (!accessoryInfo) {
			return 0;
		} else {
			const currentHp = (round === 0) ? 100 : endOfCurrentRoundBattleData.hp[0];
			const crossover = accessoryInfo.filter(i => i.name === 'adrenaline')[0].other.crossover;
			const divisor = accessoryInfo.filter(i => i.name === 'adrenaline')[0].other.divisor;
			return (crossover - currentHp) / divisor;
		}
	};
	
	snail.model.battle.submitBattlePlan = function (bp) {
		notifyEventHandlers('battlePlanSubmitted', bp);
		clearInterval(skirmishTimerInterval);
		
		const state = {};
		
		skirmishId
			.then(putBattlePlanToMatchmaker(bp))
			.then(getDataForNextRound)
			.then(checkForForfeit)
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
