var snail = (function(snail, $) {
	// private variables
	const pages = {
		home: $('#home'),
		battleplan: $('#battleplan'),
		battle: $('#battle')
	};
	
	const onBattleEvent = function (event) {
		switch (event) {
			case 'startBattle':
				snail.routing.switchTo('battleplan');
				break;
			case 'battlePlanSubmitted':
				snail.routing.switchTo('battle');
				break;
			case 'endRound':
				snail.routing.switchTo('battleplan');
				break;
			case 'endBattle':
				snail.routing.switchTo('home');
				break;
		}
	};
	
	// public methods
	snail.init = function () {
		snail.routing.init(pages);
		snail.io.init();
		snail.model.init(snail.io);
		snail.home.init(pages.home);
		snail.battleplan.init(pages.battleplan);
		snail.battle.init(pages.battle);
		
		snail.model.battle.addEventHandler(onBattleEvent);
		
		snail.routing.switchTo('home');
	};
	
	return snail;
}(snail || {}, jQuery));
