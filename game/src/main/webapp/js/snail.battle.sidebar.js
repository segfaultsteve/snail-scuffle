var snail = (function (snail, $) {
	snail.battle = snail.battle || {};
	snail.battle.sidebar = {};
	
	const componentHtml = $('#components .components-battlesidebar').html();
	
	snail.battle.sidebar.create = function ($container, battleplan) {
		// private variables
		let properties, bp;
		
		// callbacks
		const onBattlePlanUpdated = function (updatedElement, newValue) {
			set(updatedElement, newValue.displayName);
			updateStats();
		};
		
		// public methods
		const set = function (property, value) {
			if (properties[property]) {
				properties[property].text(value);
			}
		};
		
		const updateStats = function () {
			const hp = parseFloat(properties['hp'].text());
			const ap = parseFloat(properties['ap'].text());
			set('attack', snail.util.formatNumber(bp.getAttack(hp, ap), 1));
			set('defense', snail.util.formatNumber(bp.getDefense(), 1));
			set('speed', snail.util.formatNumber(bp.getSpeed(), 1));
		};
		
		// init code
		$container.append(componentHtml);
		
		properties = {
			name: $container.find('.sidebar-name'),
			snail: $container.find('.sidebar-equipment-snail'),
			weapon: $container.find('.sidebar-equipment-weapon'),
			shell: $container.find('.sidebar-equipment-shell'),
			accessory: $container.find('.sidebar-equipment-accessory'),
			attack: $container.find('.sidebar-stats-attack'),
			defense: $container.find('.sidebar-stats-defense'),
			speed: $container.find('.sidebar-stats-speed'),
			hp: $container.find('.sidebar-hpap-hp'),
			ap: $container.find('.sidebar-hpap-ap')
		};
		
		bp = battleplan;
		bp.addBattlePlanUpdatedHandler(onBattlePlanUpdated);
		
		return {
			set: set,
			updateStats: updateStats
		};
	};
	
	return snail;
}(snail || {}, jQuery));