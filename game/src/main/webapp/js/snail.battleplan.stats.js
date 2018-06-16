var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.stats = {};
	
	// private variables
	let $playerName, $playerHp, $playerAp, $playerAttack, $playerDefense, $playerSpeed;
	
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
	
	// callbacks
	const onBattlePlanUpdated = function () {
		const newAttack = snail.model.battleplan.getAttack();
		const newDefense = snail.model.battleplan.getDefense();
		const newSpeed = snail.model.battleplan.getSpeed();
		
		updateStat($playerAttack, newAttack);
		updateStat($playerDefense, newDefense);
		updateStat($playerSpeed, newSpeed);
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
		
		snail.model.battleplan.addBattlePlanUpdatedHandler(onBattlePlanUpdated);
		$playerAttack.siblings().addBack().on('animationend', removeAnimationClasses);
		$playerDefense.siblings().addBack().on('animationend', removeAnimationClasses);
		$playerSpeed.siblings().addBack().on('animationend', removeAnimationClasses);
		
		snail.battleplan.stats.refresh();
	};
	
	snail.battleplan.stats.refresh = function () {
		$playerName.text(snail.model.getPlayerName());
		$playerHp.text(snail.model.battle.getHp());
		$playerAp.text(snail.model.battle.getAp());
	};
	
	return snail;
}(snail || {}));
