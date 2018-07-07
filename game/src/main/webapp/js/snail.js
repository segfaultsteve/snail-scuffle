var snail = (function(snail, $) {
	// private variables
	const pages = {
		$home: $('#home'),
		$battleplan: $('#battleplan')
	};
	
	// public methods
	snail.init = function () {
		for (let key in pages) {
			pages[key].hide();
		}
		
		snail.model.init();
		snail.model.battleplan.init(snail.data);
		snail.model.battle.init();
		snail.home.init();
		snail.battleplan.init(pages.$battleplan);
		
		pages.$home.show();
	};
	
	return snail;
}(snail || {}, jQuery));
