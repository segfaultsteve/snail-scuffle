var snail = (function(snail, $) {
	// private variables
	const pages = {
		home: $('#home'),
		battleplan: $('#battleplan'),
		battle: $('#battle')
	};
	
	// public methods
	snail.init = function () {
		snail.routing.init(pages);
		snail.io.init();
		snail.model.init(snail.io);
		snail.home.init(pages.home);
		snail.battleplan.init(pages.battleplan);
		snail.battle.init(pages.battle);
		
		snail.routing.switchTo('home');
	};
	
	return snail;
}(snail || {}, jQuery));
