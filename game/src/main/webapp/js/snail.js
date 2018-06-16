var snail = (function(snail, $) {
	// private variables
	const pages = {
		$battleplan: $('#battleplan')
	};
	
	// public methods
	snail.init = function () {
		snail.model.init();
		snail.model.battleplan.init(snail.data);
		snail.model.battle.init();
		snail.battleplan.init(pages.$battleplan);
		pages.$battleplan.show();
	};
	
	return snail;
}(snail || {}, jQuery));
