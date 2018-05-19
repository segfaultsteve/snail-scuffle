var snail = (function($) {
	// private variables
	const pages = {
		$battleplan: $('#battleplan')
	};
	
	// public methods
	const init = function () {
		snail.battleplan.init(pages.$battleplan);
		pages.$battleplan.show();
	};
	
	return {
		init: init
	};
})(jQuery);
