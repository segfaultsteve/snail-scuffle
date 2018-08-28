var snail = (function (snail) {
	snail.home = snail.home || {};
	snail.model = snail.model || {};
	
	// private variables
	let $playbutton, $modalbackground, $playmodal, $matchingmodal;
	
	// private methods
	const showPlayModal = function () {
		$modalbackground.removeClass('hidden');
		$playmodal.removeClass('hidden');
	};
	
	const hidePlayModal = function () {
		$modalbackground.addClass('hidden');
		$playmodal.addClass('hidden');
	};
	
	const showMatchingModal = function () {
		$modalbackground.removeClass('hidden');
		$matchingmodal.removeClass('hidden');
	};
	
	const hideMatchingModal = function () {
		$modalbackground.addClass('hidden');
		$matchingmodal.addClass('hidden');
	};
	
	// callbacks
	const onSkirmishClicked = function () {
		hidePlayModal();
		showMatchingModal();
		snail.model.battle.startSkirmish();
	}
	
	const onSkirmishCanceled = function () {
		hideMatchingModal();
		snail.model.battle.cancelSkirmish();
	}
	
	// public methods
	snail.home.init = function ($container) {
		$playbutton = $container.find('.mainmenu-play');
		$playmodal = $container.find('.modals-play');
		$matchingmodal = $container.find('.modals-matching');
		$modalbackground = $container.find('.modals-background');
		
		$playbutton.click(showPlayModal);
		$playmodal.find('.modals-play-closeicon').click(hidePlayModal);
		$playmodal.find('.modals-play-skirmish').click(onSkirmishClicked);
		
		$matchingmodal.find('.modals-matching-closeicon').click(onSkirmishCanceled);
		$matchingmodal.find('.modals-matching-cancel').click(onSkirmishCanceled);
		
		hidePlayModal();
	};
	
	return snail;
}(snail || {}));
