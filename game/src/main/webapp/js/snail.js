var snail = (function(snail, $) {
	// private variables
	const pages = {
		$battleplan: $('#battleplan')
	};
	
	// public methods
	snail.init = function () {
		snail.battleplan.init(pages.$battleplan);
		pages.$battleplan.show();
	};
	
	return snail;
}(snail || {}, jQuery));
