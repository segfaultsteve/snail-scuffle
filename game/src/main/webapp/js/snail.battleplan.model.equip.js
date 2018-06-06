var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.model = snail.battleplan.model || {};
	snail.battleplan.model.equip = {};
	
	let snails, weapons, shells, accessories, items;
	
	// public methods
	snail.battleplan.model.equip.init = function (data) {
		snails = data.getSnailInfo();
		weapons = data.getWeaponInfo();
		shells = data.getShellInfo();
		accessories = data.getAccessoryInfo();
		items = data.getItemInfo();
		
		const attachErrorHandler = function (promise, type) {
			promise.fail(function () {
				throw 'failed to retrieve ' + type + ' info';
			});
		};
		attachErrorHandler(snails, 'snail');
		attachErrorHandler(weapons, 'weapon');
		attachErrorHandler(shells, 'shell');
		attachErrorHandler(accessories, 'accessory');
		attachErrorHandler(items, 'item');
	};
	
	snail.battleplan.model.equip.getSnails = function () {
		return snails;
	};
	
	return snail;
}(snail || {}));