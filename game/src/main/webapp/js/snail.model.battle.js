var snail = (function (snail) {
	snail.model = snail.model || {};
	snail.model.battle = snail.model.battle || {};
	
	// private variables
	let playerHp, playerAp;
	
	// public methods
	snail.model.battle.init = function () {
		playerHp = 100;
		playerAp = 0;
	};
	
	snail.model.battle.getHp = function () {
		return playerHp;
	};
	
	snail.model.battle.getAp = function () {
		return playerAp;
	};
	
	return snail;
}(snail || {}));
