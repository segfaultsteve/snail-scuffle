var snail = (function (snail) {
	snail.model = snail.model || {};
	
	// private variables
	let playerName;
	
	// public methods
	snail.model.init = function () {
		playerName = 'player1';		// TODO: replace this
	};
	
	snail.model.getPlayerName = function () {
		return playerName;
	};
	
	return snail;
}(snail || {}));
