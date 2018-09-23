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
		
		if (battleData.enemyName === 'Guest') {
			battleData.enemyName = 'Enemy';
		}
		
		hudData.playerName.text(battleData.playerName);
		hudData.enemyName.text(battleData.enemyName);
		hudData.playerHp.text(battleData.playerHp);
		hudData.playerAp.text(battleData.playerAp);
		hudData.enemyHp.text(battleData.enemyHp);
		hudData.enemyAp.text(battleData.enemyAp);
		
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
			enemyName: $container.find('.hud-enemy-name'),
			enemyHp: $container.find('.hud-enemy-hp'),
			enemyAp: $container.find('.hud-enemy-ap')
		};
		
		snail.model.battle.addEventHandler(onBattleEvent);
	};
	
	return snail;
}(snail || {}, PIXI));
