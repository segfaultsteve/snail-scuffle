var snail = (function (snail, PIXI) {
	snail.battle = snail.battle || {};
	
	// private variables
	const WIDTH = 1000, HEIGHT = 500, TOLERANCE = 1e-5;
	let pixiApp, $hud, $waitMessage, $opponentForfeitedMessage, $resultMessage, battleData, speeds, hudData, running, eventIndex;
	let events = [];
	
	// private methods
	const showWaitMessage = function () {
		$hud.hide();
		$opponentForfeitedMessage.hide();
		$resultMessage.hide();
		$waitMessage.show();
	};
	
	const showOpponentForfeitedMessage = function () {
		$hud.hide();
		$resultMessage.hide();
		$waitMessage.hide();
		$opponentForfeitedMessage.show();
	};
	
	const startRound = function (args) {
		battleData = args.battleData;
		events = args.events;
		speeds = [snail.model.battleplan.playerBp.getSpeed(), snail.model.battleplan.enemyBp.getSpeed()];
		
		updateHud();
		$waitMessage.hide();
		$opponentForfeitedMessage.hide();
		$resultMessage.hide();
		$hud.show();
		
		eventIndex = 0;
		running = true;
	};
	
	const updateHud = function () {
		hudData.playerName.text(battleData.names[0]);
		hudData.enemyName.text(battleData.names[1]);
		hudData.playerHp.text(formatHp(battleData.hp[0]));
		hudData.enemyHp.text(formatHp(battleData.hp[1]));
		hudData.playerAp.text(formatAp(battleData.ap[0]));
		hudData.enemyAp.text(formatAp(battleData.ap[1]));
	};
	
	const formatHp = function (hp) {
		if (hp > 0.99999 || hp < 0.00001) {
			return Math.max(Math.round(10*hp)/10, 0);
		} else {
			return '< 1';
		}
	};
	
	const formatAp = function (ap) {
		return Math.max(Math.floor(ap), 0);
	};
	
	const animationLoop = function (delta) {
		if (running) {
			if (eventIndex < events.length && battleData.time > events[eventIndex].time) {
				applyEvent(events[eventIndex]);
				eventIndex++;
			} else {
				const deltaTicks = 8*delta;
				battleData.time += deltaTicks;
				for (let i = 0; i < 2; i++) {
					battleData.ap[i] += speeds[i] * deltaTicks / 1000;
				}
				updateHud();
			}
			
			if (battleData.hp[1] <= TOLERANCE && eventIndex == events.length) {
				running = false;
				$resultMessage.find('.result-text').text('You Won!');
				$resultMessage.show();
			} else if (battleData.hp[0] <= TOLERANCE && eventIndex == events.length) {
				running = false;
				$resultMessage.find('.result-text').text('You Lost!');
				$resultMessage.show();
			} else if (battleData.time > battleData.endOfRound) {
				running = false;
				snail.model.battle.finishRound();
			}
		}
	};
	
	const applyEvent = function (battleEvent) {
		if (battleEvent.action === 'attack') {
			const bp = (battleEvent.playerIndex === 0) ? snail.model.battleplan.playerBp : snail.model.battleplan.enemyBp;
			battleData.ap[battleEvent.playerIndex] -= bp.getWeaponApCost();
		}
		
		for (let i = 0; i < battleEvent.effects.length; i++) {
			const effect = battleEvent.effects[i];
			if (effect.stat === 'hp') {
				battleData.hp[effect.playerIndex] += effect.change;
			} else if (effect.stat === 'ap') {
				battleData.ap[effect.playerIndex] += effect.change;
			}
		}
		
		updateHud();
	};
	
	const reset = function () {
		running = false;
		eventIndex = 0;
	};
	
	// callbacks
	const onBattleEvent = function (event, args) {
		switch (event) {
			case 'battleStarted':
				reset();
				break;
			case 'battlePlanSubmitted':
				showWaitMessage();
				break;
			case 'nextRound':
				startRound(args);
				break;
			case 'opponentHasForfeited':
				showOpponentForfeitedMessage();
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
		$opponentForfeitedMessage = $container.find('.opponentforfeitedmessage');
		$opponentForfeitedMessage.find('.opponentforfeitedmessage-okbutton').click(function () {
			snail.model.battle.finishBattle();
		});
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
		
		reset();
		
		snail.model.battle.addEventHandler(onBattleEvent);
	};
	
	return snail;
}(snail || {}, PIXI));
