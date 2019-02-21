var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.stats = {};
	
	// private variables
	let $playerName, $playerHp, $playerAp, $playerAttack, $playerDefense, $playerSpeed;
	let $enemyName, $enemyHp, $enemyAp, $enemyAttack, $enemyDefense, $enemySpeed;
	
	// private methods
	const updateStat = function ($stat, newValue) {
		let oldValue = $stat.text();
		if (oldValue === '?') {
			oldValue = 0;
		} else {
			oldValue = parseInt(oldValue);
		}
		
		$stat.text(newValue);
		
		const $row = $stat.siblings().addBack();
		if (newValue > oldValue) {
			$row.removeClass('stat-increased stat-decreased');
			$row.addClass('stat-increased');
		} else if (newValue < oldValue) {
			$row.removeClass('stat-increased stat-decreased');
			$row.addClass('stat-decreased');
		}
	};
	
	const removeAnimationClasses = function (e) {
		e.target.classList.remove('stat-increased', 'stat-decreased');
	};
	
	const updateHpAndAp = function (battleData) {
		$playerHp.text(snail.util.formatHp(battleData.hp[0]));
		$playerAp.text(snail.util.formatAp(battleData.ap[0]));
		$enemyHp.text(snail.util.formatHp(battleData.hp[1]));
		$enemyAp.text(snail.util.formatAp(battleData.ap[1]));
	};
	
	// callbacks
	const onBattleStarted = function (battleData) {
		$playerName.text(battleData.names[0]);
		$enemyName.text(battleData.names[1]);
		updateHpAndAp(battleData);
		$enemyAttack.text('?');
		$enemyDefense.text('?');
		$enemySpeed.text('?');
	};
	
	const onBattlePlanUpdated = function (bp, $attack, $defense, $speed) {
		return function () {
			updateStat($attack, snail.util.formatNumber(bp.getAttack(), 1));
			updateStat($defense, snail.util.formatNumber(bp.getDefense(), 1));
			updateStat($speed, snail.util.formatNumber(bp.getSpeed(), 1));
		}
	};
	
	const onRoundComplete = function (battleData) {
		updateHpAndAp(battleData);
	};
	
	// public methods
	snail.battleplan.stats.init = function ($container) {
		// init code
		$playerName = $container.find('.info-playerstats-name');
		$playerHp = $container.find('.info-playerstats-hp');
		$playerAp = $container.find('.info-playerstats-ap');
		$playerAttack = $container.find('.info-playerstats-attack');
		$playerDefense = $container.find('.info-playerstats-defense');
		$playerSpeed = $container.find('.info-playerstats-speed');
		
		$enemyName = $container.find('.info-enemystats-name');
		$enemyHp = $container.find('.info-enemystats-hp');
		$enemyAp = $container.find('.info-enemystats-ap');
		$enemyAttack = $container.find('.info-enemystats-attack');
		$enemyDefense = $container.find('.info-enemystats-defense');
		$enemySpeed = $container.find('.info-enemystats-speed');
		
		snail.model.battleplan.playerBp.addBattlePlanUpdatedHandler(
			onBattlePlanUpdated(snail.model.battleplan.playerBp, $playerAttack, $playerDefense, $playerSpeed)
		);
		
		snail.model.battleplan.enemyBp.addBattlePlanUpdatedHandler(
			onBattlePlanUpdated(snail.model.battleplan.enemyBp, $enemyAttack, $enemyDefense, $enemySpeed)
		);
		
		$playerAttack.siblings().addBack().on('animationend', removeAnimationClasses);
		$playerDefense.siblings().addBack().on('animationend', removeAnimationClasses);
		$playerSpeed.siblings().addBack().on('animationend', removeAnimationClasses);
		
		snail.model.battle.addEventHandler(function (event, args) {
			switch (event) {
				case 'battleStarted':
					onBattleStarted(args);
					break;
				case 'roundComplete':
					onRoundComplete(args);
					break;
			}
		});
	};
	
	return snail;
}(snail || {}));
