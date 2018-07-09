var snail = (function (snail) {
	snail.home = snail.home || {};
	snail.model = snail.model || {};
	
	// private variables
	let $playbutton, $modalbackground, $playmodal;
	
	// private methods
	const showPlayModal = function () {
		$modalbackground.removeClass('hidden');
		$playmodal.removeClass('hidden');
	};
	
	const hidePlayModal = function () {
		$modalbackground.addClass('hidden');
		$playmodal.addClass('hidden');
	};
	
	// public methods
	snail.home.init = function ($container) {
		$playbutton = $container.find('.mainmenu-play');
		$playmodal = $container.find('.modals-play');
		$modalbackground = $container.find('.modals-background');
		
		$playbutton.click(showPlayModal);
		$playmodal.find('.modals-play-closeicon').click(hidePlayModal);
		$modalbackground.click(hidePlayModal);
		
		hidePlayModal();
	};
	
	return snail;
}(snail || {}));