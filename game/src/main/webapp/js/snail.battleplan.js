var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	
	// private variables
	const weapons = ['A Salt Rifle', 'Rocket Launcher', 'Laser'];
	const shells = ['Aluminum', 'Steel', 'No Shell'];
	const accessories = ['Steroids', 'Snail Mail', 'Caffeine', 'Charged Attack', 'Adrenaline', 'Salted Shell', 'Thorns', 'Defibrillator', 'No Accessory'];
	const items = ['Attack Boost', 'Defense Boost', 'Speed Boost', 'None'];
	
	let weaponButton, shellButton, accessoryButton, item1Button, item2Button, instructions;
	
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
	snail.battleplan.init = function ($container) {
		weaponButton = createMenuButton($container.find('.equip-weapon'), weapons, weapons[0]);
		shellButton = createMenuButton($container.find('.equip-shell'), shells, 'No Shell');
		accessoryButton = createMenuButton($container.find('.equip-accessory'), accessories, 'No Accessory');
		item1Button = createItemButton($container.find('.equip-item1'), items);
		item2Button = createItemButton($container.find('.equip-item2'), items);
		instructions = snail.battleplan.instructionbox.create($container.find('.instructions'));
	};
	
	snail.battleplan.get = function () {
		return {
			weapon: weaponButton.getSelectedOption(),
			shell: shellButton.getSelectedOption(),
			accessory: accessoryButton.getSelectedOption(),
			item1: item1Button.getSelectedOption(),
			item2: item2Button.getSelectedOption(),
			instructions: instructions.getInstructions()
		};
	}
	
	return snail;
}(snail || {}));
