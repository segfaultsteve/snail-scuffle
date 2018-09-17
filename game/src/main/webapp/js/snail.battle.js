var snail = (function (snail, PIXI) {
	snail.battle = snail.battle || {};
	
	// private variables
	const WIDTH = 1000, HEIGHT = 500;
	let pixiApp, $waitMessage, $hud, hudData;
	
	// private methods
	const showWaitMessage = function () {
		$hud.hide();
		$waitMessage.show();
	};
	
	const startRound = function (args) {
		const battleData = args.battleData;
		
		if (battleData.playerName === 'Guest') {
			battleData.playerName = 'You';
		}
		
		if (battleData.opponentName === 'Guest') {
			battleData.opponentName = 'Opponent';
		}
		
		hudData.playerName.text(battleData.playerName);
		hudData.opponentName.text(battleData.opponentName);
		hudData.playerHp.text(battleData.playerHp);
		hudData.playerAp.text(battleData.playerAp);
		hudData.opponentHp.text(battleData.opponentHp);
		hudData.opponentAp.text(battleData.opponentAp);
		
		$waitMessage.hide();
		$hud.show();
	};
	
	// callbacks
	const onBattleEvent = function (event, args) {
		switch (event) {
			case 'battlePlanSubmitted':
				showWaitMessage();
				break;
			case 'startRound':
				startRound(args);
				break;
		}
	};

	// public methods
	snail.battle.init = function ($container) {
		const canvas = $container.find('#battleCanvas')[0];
		pixiApp = new PIXI.Application({
			view: canvas,
			width: WIDTH,
			height: HEIGHT,
			backgroundColor: 0xffffff
		});
		
		$waitMessage = $container.find('.waitmessage');
		$hud = $container.find('.hud');
		
		hudData = {
			playerName: $container.find('.hud-player-name'),
			playerHp: $container.find('.hud-player-hp'),
			playerAp: $container.find('.hud-player-ap'),
			opponentName: $container.find('.hud-opponent-name'),
			opponentHp: $container.find('.hud-opponent-hp'),
			opponentAp: $container.find('.hud-opponent-ap')
		};
		
		snail.model.battle.addEventHandler(onBattleEvent);
	};
	
	return snail;
}(snail || {}, PIXI));
