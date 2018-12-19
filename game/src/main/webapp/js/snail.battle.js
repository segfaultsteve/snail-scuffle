var snail = (function (snail, PIXI) {
	snail.battle = snail.battle || {};
	
	// private variables
	const WIDTH = 1000, HEIGHT = 500;
	let pixiApp, $hud, $waitMessage, $resultMessage, battleData, hudData, running, time, eventIndex;
	let events = [];
	
	// private methods
	const showWaitMessage = function () {
		$hud.hide();
		$waitMessage.show();
	};
	
	const startRound = function (args) {
		battleData = args.battleData;
		updateHud();
		
		$waitMessage.hide();
		$resultMessage.hide();
		$hud.show();
		
		events = args.events;
		eventIndex = 0;
		running = true;
	};
	
	const updateHud = function () {
		hudData.playerName.text(battleData.playerName);
		hudData.playerHp.text(formatNumber(battleData.playerHp));
		hudData.playerAp.text(formatNumber(battleData.playerAp));
		hudData.enemyName.text(battleData.enemyName);
		hudData.enemyHp.text(formatNumber(battleData.enemyHp));
		hudData.enemyAp.text(formatNumber(battleData.enemyAp));
	};
	
	const formatNumber = function (num) {
		return Math.max(Math.round(100*num)/100, 0);
	};
	
	const animationLoop = function (delta) {
		if (running) {
			let prev = Math.floor(time/6000);
			time += delta*16;
			let curr = Math.floor(time/6000);
			
			if (curr > prev) {
				running = false;
				snail.model.battle.finishRound();
			}
			
			if (eventIndex < events.length && time > events[eventIndex].time) {
				applyEvent(events[eventIndex]);
				eventIndex++;
			}
			
			if (battleData.enemyHp <= 0) {
					running = false;
					$resultMessage.find('.result-text').text("You Won!");
					$resultMessage.show();
				} else if (battleData.playerHp <= 0) {
					running = false;
					$resultMessage.find('.result-text').text("You Lost!");
					$resultMessage.show();
				}
		}
	};
	
	const applyEvent = function (battleEvent) {
		for (let i = 0; i < battleEvent.effects.length; i++) {
			let effect = battleEvent.effects[i];
			if (effect.stat === "hp") {
				if (effect.playerIndex == 0) {
					battleData.playerHp += effect.change;
				} else {
					battleData.enemyHp += effect.change;
				}
			}
		}
		updateHud();
	};
	
	// callbacks
	const onBattleEvent = function (event, args) {
		switch (event) {
			case 'battlePlanSubmitted':
				showWaitMessage();
				break;
			case 'nextRound':
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
		pixiApp.ticker.add(animationLoop);
		
		$hud = $container.find('.hud');
		$waitMessage = $container.find('.waitmessage');
		$resultMessage = $container.find('.result');
		$resultMessage.find('.result-okbutton').click(function () {
			snail.model.battle.finishBattle();
		});
		
		hudData = {
			playerName: $container.find('.hud-player-name'),
			playerHp: $container.find('.hud-player-hp'),
			playerAp: $container.find('.hud-player-ap'),
			enemyName: $container.find('.hud-enemy-name'),
			enemyHp: $container.find('.hud-enemy-hp'),
			enemyAp: $container.find('.hud-enemy-ap')
		};
		
		running = false;
		time = 0;
		eventIndex = 0;
		
		snail.model.battle.addEventHandler(onBattleEvent);
	};
	
	return snail;
}(snail || {}, PIXI));
