var snail = (function(snail, $) {
	// private variables
	const pages = {
		home: $('#home'),
		battleplan: $('#battleplan')
	};
	
	// public methods
	snail.init = function () {
		snail.routing.init(pages);
		snail.io.init();
		snail.model.init();
		snail.model.battleplan.init(snail.io);
		snail.model.battle.init();
		snail.home.init(pages.home);
		snail.battleplan.init(pages.battleplan);
		
		snail.routing.switchTo('home');
	};
	
	return snail;
}(snail || {}, jQuery));
