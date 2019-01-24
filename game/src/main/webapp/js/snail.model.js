var snail = (function (snail) {
	snail.model = snail.model || {};
	
	// private variables
	let playerName;
	
	// public methods
	snail.model.init = function (io) {
		playerName = 'Guest';
		
		snail.model.battle.init(io);
		snail.model.battleplan.init(io);
	};
	
	snail.model.getPlayerName = function () {
		return playerName;
	};
	
	return snail;
}(snail || {}));
