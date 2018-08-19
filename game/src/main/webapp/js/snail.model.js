var snail = (function (snail, $) {
	snail.model = snail.model || {};
	
	// private variables
	let playerName, skirmishId, playerIndex, opponentName, skirmishPollInterval;
	
	// private methods
	const pollMatchmakerForSkirmish = function () {
		skirmishId
			.then(function (id) {
				return snail.io.getSkirmish(id);
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
	snail.model.init = function () {
		playerName = 'Guest';
	};
	
	snail.model.getPlayerName = function () {
		return playerName;
	};
	
	snail.model.startSkirmish = function () {
		skirmishId = $.Deferred();
		playerIndex = $.Deferred();
		opponentName = $.Deferred();
		
		snail.io.putSkirmish()
			.done(function (skirmishResponse) {
				skirmishId.resolve(skirmishResponse.skirmishId);
				playerIndex.resolve(skirmishResponse.indexOfRequestingPlayer);
				if (skirmishResponse.player2Name) {
					setOpponentName(skirmishResponse);
					snail.routing.switchTo('battleplan');
				} else {
					skirmishPollInterval = setInterval(pollMatchmakerForSkirmish, 2000);
				}
			});
	};
	
	snail.model.cancelSkirmish = function () {
		clearInterval(skirmishPollInterval);
		// TODO: create 'cancel' endpoint on server (maybe DELETE handler?),
		// then send ajax here
	};
	
	return snail;
}(snail || {}, jQuery));
