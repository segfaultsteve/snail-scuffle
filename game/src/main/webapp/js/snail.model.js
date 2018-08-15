var snail = (function (snail, $) {
	snail.model = snail.model || {};
	
	// private variables
	let playerName, skirmishId, playerIndex, opponentName;
	
	// public methods
	snail.model.init = function () {
		playerName = 'Guest';
		skirmishId = $.Deferred();
		playerIndex = $.Deferred();
		opponentName = $.Deferred();
	};
	
	snail.model.getPlayerName = function () {
		return playerName;
	};
	
	snail.model.startSkirmish = function () {
		snail.data.promiseServerInfo()
			.then(function (servers) {
				return $.ajax({
					url: servers.matchmaker + '/skirmishes',
					type: 'PUT',
					dataType: 'json',
					xhrFields: { withCredentials: true }
				}).promise();
			})
			.then(function (skirmishResponse) {
				skirmishId.resolve(skirmishResponse.skirmishId);
				playerIndex.resolve(skirmishResponse.indexOfRequestingPlayer);
				if (skirmishResponse.indexOfRequestingPlayer === 2) {
					opponentName.resolve(skirmishResponse.player1Name);
				}
			});
	};
	
	snail.model.cancelSkirmish = function () {
		
	};
	
	return snail;
}(snail || {}, jQuery));
