var snail = (function (snail, $) {
	snail.model = snail.model || {};
	snail.model.battle = snail.model.battle || {};
	
	// private variables
	let ioLayer, skirmishId, playerIndex, round, skirmishPollInterval;
	let eventHandlers = [];
	
	// private methods
	const newBattleData = function () {
		return {
			time: 0,
			playerName: '',
			playerHp: 100,
			playerAp: 0,
			playerSpeed: 0,
			opponentName: '',
			opponentHp: 100,
			opponentAp: 0,
			opponentSpeed: 0
		};
	};
	
	const pollMatchmakerUntilMatched = function () {
		skirmishId
			.then(function (id) {
				return ioLayer.getSkirmish(id);
			})
			.done(function (skirmishResponse) {
				if (skirmishResponse.player2Name) {
					clearInterval(skirmishPollInterval);
					signalStartOfBattle(skirmishResponse);
				}
			});
	};
	
	const signalStartOfBattle = function (skirmishResponse) {
		const battleData = newBattleData();
		setNames(battleData, skirmishResponse);
		notifyEventHandlers('startBattle', battleData);
	};
	
	const setNames = function (battleData, skirmishResponse) {
		if (skirmishResponse.indexOfRequestingPlayer === 1) {
			battleData.playerName = skirmishResponse.player1Name;
			battleData.opponentName = skirmishResponse.player2Name;
		} else {
			battleData.playerName = skirmishResponse.player2Name;
			battleData.opponentName = skirmishResponse.player1Name;
		}
	};
	
	const pollMatchmakerForBattlePlans = function () {
		skirmishId
			.then(function (id) {
				return ioLayer.getSkirmish(id);
			})
			.done(function (skirmishResponse) {
				if (skirmishResponse.battlePlans.length > 2*round) {
					clearInterval(skirmishPollInterval);
					nextRound(skirmishResponse.battlePlans);
				}
			});
	};
	
	const nextRound = function (battlePlans) {
		ioLayer.postBattle(battlePlans)
			.done(function (battleResult) {
				const thisRoundBps = battlePlans.slice(-2);
				const thisRoundResult = battleResult.eventsByRound.slice(-1);
			});
		notifyEventHandlers('startRound'); //TODO: pass battle data
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
		playerIndex = $.Deferred();
		
		ioLayer.putSkirmish()
			.done(function (skirmishResponse) {
				skirmishId.resolve(skirmishResponse.skirmishId);
				playerIndex.resolve(skirmishResponse.indexOfRequestingPlayer);
				if (skirmishResponse.player2Name) {
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
		
		skirmishId
			.then(function (id) {
				return ioLayer.putBattlePlan(id, bp);
			})
			.done(function () {
				skirmishPollInterval = setInterval(pollMatchmakerForBattlePlans, 2000);
			});
	};
	
	return snail;
}(snail || {}, jQuery));
