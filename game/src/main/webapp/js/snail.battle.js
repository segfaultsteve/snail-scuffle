var snail = (function (snail) {
	snail.battle = snail.battle || {};
	
	// private variables
	let canvas;
	
	// private methods
	const resize = function () {
		canvas.width = canvas.clientWidth;
		canvas.height = canvas.clientHeight;
	};
	
	// public methods
	snail.battle.init = function ($container) {
		canvas = $container.find('#battleCanvas')[0];
	};
	
	snail.battle.reset = function () {
		resize();
	};
	
	return snail;
}(snail || {}));
