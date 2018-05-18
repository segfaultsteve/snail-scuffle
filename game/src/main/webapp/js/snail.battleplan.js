/* global snail */

snail.battleplan = (function () {
	// private variables
	const weapons = ['A Salt Rifle', 'Rocket Launcher', 'Laser'];
	const shells = ['Aluminum', 'Steel', 'No Shell'];
	const accessories = ['Steroids', 'Snail Mail', 'Caffeine', 'Charged Attack', 'Adrenaline', 'Salted Shell', 'Thorns', 'Defibrillator', 'No Accessory'];
	const items = ['Attack Boost', 'Defense Boost', 'Speed Boost', 'None'];
	
	let weaponButton, shellButton, accessoryButton, item1Button, item2Button;
	
	// private methods
	const createMenuButton = function ($container, items, defaultSelection) {
		const button = snail.battleplan.menubutton.create($container);
		button.setOptionsList(items, defaultSelection);
		return button;
	}
	
	const createItemButton = function ($container, items) {
		const button = snail.battleplan.itembutton.create($container);
		button.setOptionsList(items, 'None');
		return button;
	}
	
	// public methods
	const init = function ($container) {
		weaponButton = createMenuButton($container.find('.equip-weapon'), weapons, weapons[0]);
		shellButton = createMenuButton($container.find('.equip-shell'), shells, 'No Shell');
		accessoryButton = createMenuButton($container.find('.equip-accessory'), accessories, 'No Accessory');
		item1Button = createItemButton($container.find('.equip-item1'), items);
		item2Button = createItemButton($container.find('.equip-item2'), items);
	};
	
	const get = function () {
		return {
			weapon: weaponButton.getSelectedOption(),
			shell: shellButton.getSelectedOption(),
			accessory: accessoryButton.getSelectedOption(),
			item1: item1Button.getSelectedOption(),
			item2: item2Button.getSelectedOption()
		};
	}
	
	return {
		init: init,
		get: get
	};
}());