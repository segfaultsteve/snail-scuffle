var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.model = snail.battleplan.model || {};
	
	// private variables
	const model = snail.battleplan.model;
	let snailButtons, weaponButton, shellButton, accessoryButton, item1Button, item2Button, instructionBox;
	
	// private methods
	const createMenuButton = function ($container, itemsPromise, defaultSelectionIndex) {
		const button = snail.battleplan.menubutton.create($container);
		itemsPromise.done(function (items) {
			const displayNames = items.map(i => i.displayName);
			button.setOptionsList(displayNames, defaultSelectionIndex);
		});
		return button;
	};
	
	const createItemButton = function ($container, itemsPromise) {
		const button = snail.battleplan.itembutton.create($container);
		itemsPromise.done(function (items) {
			const displayNames = items.map(i => i.displayName);
			button.setOptionsList(displayNames, 'last');
		});
		return button;
	};
	
	const setSelectedSnail = function (newSnail, $container) {
		$container.find('.snails button').removeClass('selected-snail');
		if (newSnail in snailButtons) {
			snailButtons[newSnail].addClass('selected-snail');
			snail.battleplan.model.setSnail(newSnail);
		}
	};
	
	// public methods
	snail.battleplan.init = function ($container) {
		snailButtons = {
			Dale: $container.find('.snails-dale'),
			Gail: $container.find('.snails-gail'),
			Todd: $container.find('.snails-todd'),
			Doug: $container.find('.snails-doug')
		};
		weaponButton = createMenuButton($container.find('.equip-weapon'), model.promiseWeapons(), 0);
		shellButton = createMenuButton($container.find('.equip-shell'), model.promiseShells(), 'last');
		accessoryButton = createMenuButton($container.find('.equip-accessory'), model.promiseAccessories(), 'last');
		item1Button = createItemButton($container.find('.equip-item1'), model.promiseItems());
		item2Button = createItemButton($container.find('.equip-item2'), model.promiseItems());
		instructionBox = snail.battleplan.instructionbox.create($container.find('.instructions'));
		
		$container.find('.snails button').click(function (e) { setSelectedSnail(e.target.firstChild.nodeValue, $container) });
		weaponButton.addSelectionChangedHandler(function (index, weapon) { model.setWeapon(weapon) });
		shellButton.addSelectionChangedHandler(function (index, shell) { model.setShell(shell) });
		accessoryButton.addSelectionChangedHandler(function (index, accessory) { model.setAccessory(accessory) });
		item1Button.addSelectionChangedHandler(function (index, item) { model.setItem(0, item) });
		item1Button.addConditionChangedHandler(function (condition) { model.setItemCondition(0, condition) });
		item2Button.addSelectionChangedHandler(function (index, item) { model.setItem(1, item) });
		item2Button.addConditionChangedHandler(function (condition) { model.setItemCondition(1, condition) });
		instructionBox.addInstructionsChangedHandler(function () { model.setInstructions(instructionBox.getInstructions()) });
		
		setSelectedSnail('Dale', $container);
	};
	
	return snail;
}(snail || {}));
