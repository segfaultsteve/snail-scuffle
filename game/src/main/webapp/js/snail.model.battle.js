var snail = (function (snail, $) {
	snail.model = snail.model || {};
	snail.model.battle = snail.model.battle || {};
	
	// private variables
	let ioLayer, battleData, skirmishId, playerIndex, opponentName, skirmishPollInterval;
	
	// private methods
	const newBattleData = function () {
		return {
			round: 0,
			playerHp: 100,
			playerAp: 0,
			opponentHp: 100,
			opponentAp: 0
		};
	};
	
	const pollMatchmakerUntilMatched = function () {
		skirmishId
			.then(function (id) {
				return ioLayer.getSkirmish(id);
			})
			.done(function (skirmishResponse) {
				if (skirmishResponse.player2Name) {
					setOpponentName(skirmishResponse);
					clearInterval(skirmishPollInterval);
					snail.routing.switchTo('battleplan');
				}
			});
	};
	
	const setOpponentName = function (skirmishResponse) {
		if (skirmishResponse.indexOfRequestingPlayer === 1) {
			opponentName.resolve(skirmishResponse.player2Name);
		} else {
			opponentName.resolve(skirmishResponse.player1Name);
		}
	};
	
	// public methods
	snail.model.battle.init = function (io) {
		ioLayer = io;
		battleData = newBattleData();
	};
	
	snail.model.battle.startSkirmish = function () {
		battleData = newBattleData();
		
		skirmishId = $.Deferred();
		playerIndex = $.Deferred();
		opponentName = $.Deferred();
		
		ioLayer.putSkirmish()
			.done(function (skirmishResponse) {
				skirmishId.resolve(skirmishResponse.skirmishId);
				playerIndex.resolve(skirmishResponse.indexOfRequestingPlayer);
				if (skirmishResponse.player2Name) {
					setOpponentName(skirmishResponse);
					snail.routing.switchTo('battleplan');
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
		snail.routing.switchTo('battle');
		snail.battle.reset();
		
		skirmishId.then(function (id) {
			return ioLayer.putBattlePlan(id, bp);
		})
		.done(function (skirmishResponse) {
			if (skirmishResponse.battlePlans.length > 2*battleData.round) {
				// transition to battle screen
			} else {
				// poll until we receive battle plan
			}
		});
	};
	
	snail.model.battle.getHp = function () {
		return battleData.playerHp;
	};
	
	snail.model.battle.getAp = function () {
		return battleData.playerAp;
	};
	
	return snail;
}(snail || {}, jQuery));
