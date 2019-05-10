var snail = (function (snail) {
	snail.battle = snail.battle || {};
	
	// private variables
	let $hud, $waitMessage, $opponentForfeitedMessage, $resultMessage, lastRender, battleData, speeds, running, eventIndex;
	let combatants = [];
	let events = [];
	
	// private methods
	const showWaitMessage = function () {
		$hud.addClass('hidden');
		$opponentForfeitedMessage.addClass('hidden');
		$resultMessage.addClass('hidden');
		$waitMessage.removeClass('hidden');
	};
	
	const showOpponentForfeitedMessage = function () {
		$resultMessage.addClass('hidden');
		$waitMessage.addClass('hidden');
		$opponentForfeitedMessage.removeClass('hidden');
	};
	
	const startRound = function (args) {
		battleData = args.battleData;
		events = args.events;
		speeds = [snail.model.battleplan.playerBp.getSpeed(), snail.model.battleplan.enemyBp.getSpeed()];
		
		for (let i = 0; i < 2; i++) {
			combatants[i].set('name', battleData.names[i]);
		}
		updateHpAndAp();
		
		$waitMessage.addClass('hidden');
		$opponentForfeitedMessage.addClass('hidden');
		$resultMessage.addClass('hidden');
		$hud.removeClass('hidden');
		
		eventIndex = 0;
		running = true;
	};
	
	const updateHpAndAp = function () {
		for (let i = 0; i < 2; i++) {
			combatants[i].set('hp', snail.util.formatHp(battleData.hp[i]));
			combatants[i].set('ap', snail.util.formatAp(battleData.ap[i]));
			combatants[i].updateStats();
		}
	};
	
	const animationLoop = function (timestamp) {
		const delta = timestamp - lastRender;
		lastRender = timestamp;
		
		if (running) {
			if (eventIndex < events.length && battleData.time > events[eventIndex].time) {
				applyEvent(events[eventIndex]);
				eventIndex++;
			} else {
				const deltaTicks = delta/2;
				battleData.time += deltaTicks;
				for (let i = 0; i < 2; i++) {
					if (!battleData.effects[i].includes('stun')) {
						battleData.ap[i] += speeds[i] * deltaTicks / 1000;
					}
				}
				updateHpAndAp();
			}
			
			if (eventIndex == events.length && battleData.winnerIndex === 0) {
				running = false;
				$resultMessage.find('.result-text').text('You Won!');
				$resultMessage.removeClass('hidden');
			} else if (eventIndex == events.length && battleData.winnerIndex === 1) {
				running = false;
				$resultMessage.find('.result-text').text('You Lost!');
				$resultMessage.removeClass('hidden');
			} else if (battleData.time > battleData.endOfRound && battleData.winnerIndex < 0) {
				running = false;
				snail.model.battle.finishRound();
			}
		}
		
		window.requestAnimationFrame(animationLoop);
	};
	
	const applyEvent = function (battleEvent) {
		const bp = (battleEvent.playerIndex === 0) ? snail.model.battleplan.playerBp : snail.model.battleplan.enemyBp;
		if (battleEvent.action === 'attack') {
			battleData.ap[battleEvent.playerIndex] -= bp.getWeaponApCost();
		} else if (battleEvent.action === 'use_item') {
			if (battleEvent.itemUsed === 'stun') {
				const otherPlayer = (battleEvent.playerIndex === 0) ? 1 : 0;
				battleData.effects[otherPlayer].push(battleEvent.itemUsed);
			} else {
				battleData.effects[battleEvent.playerIndex].push(battleEvent.itemUsed);
			}
			bp.registerItemUsed(battleEvent.itemUsed);
		} else if (battleEvent.action === 'item_done') {
			const effects = battleData.effects[battleEvent.playerIndex];
			const index = effects.indexOf(battleEvent.itemUsed);
			if (index > -1) {
				effects.splice(index, 1);
			}
		}
		
		for (let i = 0; i < battleEvent.effects.length; i++) {
			const effect = battleEvent.effects[i];
			if (effect.stat === 'hp') {
				battleData.hp[effect.playerIndex] += effect.change;
			} else if (effect.stat === 'ap') {
				battleData.ap[effect.playerIndex] += effect.change;
			}
		}
		
		updateHpAndAp();
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
		$hud = $container.find('.hud');
		const playerInfo = snail.battle.sidebar.create($hud.find('.hud-player'), snail.model.battleplan.playerBp);
		const enemyInfo = snail.battle.sidebar.create($hud.find('.hud-enemy'), snail.model.battleplan.enemyBp);
		combatants.push(playerInfo);
		combatants.push(enemyInfo);
		
		$waitMessage = $container.find('.waitmessage');
		$opponentForfeitedMessage = $container.find('.opponentforfeitedmessage');
		$opponentForfeitedMessage.find('.opponentforfeitedmessage-okbutton').click(function () {
			snail.model.battle.finishBattle();
		});
		$resultMessage = $container.find('.result');
		$resultMessage.find('.result-okbutton').click(function () {
			snail.model.battle.finishBattle();
		});
		
		reset();
		window.requestAnimationFrame(animationLoop);
		snail.model.battle.addEventHandler(onBattleEvent);
	};
	
	return snail;
}(snail || {}));
