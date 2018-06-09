var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.model = snail.battleplan.model || {};
	
	// private variables
	const model = snail.battleplan.model;
	let $battleplan, snailButtons, weaponButton, shellButton, accessoryButton, item1Button, item2Button, instructionBox;
	
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
	
	const setSelectedSnail = function (newSnail) {
		const currentSnail = $battleplan.find('.snails .selected-snail').text();
		if (newSnail !== currentSnail) {
			$battleplan.find('.snails button').removeClass('selected-snail');
			if (newSnail in snailButtons) {
				snailButtons[newSnail].addClass('selected-snail');
				snail.battleplan.model.setSnail(newSnail);
			}
		}
	};
	
	// callbacks
	const onBattlePlanUpdated = function (updatedElement, newValue) {
		switch (updatedElement) {
			case 'snail':
				setSelectedSnail(newValue);
				break;
			case 'weapon':
				weaponButton.setSelectedOption(newValue);
				break;
			case 'shell':
				shellButton.setSelectedOption(newValue);
				break;
			case 'accessory':
				accessoryButton.setSelectedOption(newValue);
				break;
			case 'item1':
				item1Button.setSelectedOption(newValue);
				break;
			case 'item2':
				item2Button.setSelectedOption(newValue);
				break;
			case 'item1Rule':
				item1Button.setRule(newValue);
				break;
			case 'item2Rule':
				item2Button.setRule(newValue);
				break;
			case 'instructions':
				instructionBox.setInstructions(newValue);
				break;
		}
	};
	
	// public methods
	snail.battleplan.init = function ($container) {
		$battleplan = $container;
		
		// create components and set initial state
		snailButtons = {
			Dale: $container.find('.snails-dale'),
			Gail: $container.find('.snails-gail'),
			Todd: $container.find('.snails-todd'),
			Doug: $container.find('.snails-doug')
		};
		setSelectedSnail('Dale');
		weaponButton = createMenuButton($container.find('.equip-weapon'), model.promiseWeapons(), 0);
		shellButton = createMenuButton($container.find('.equip-shell'), model.promiseShells(), 'last');
		accessoryButton = createMenuButton($container.find('.equip-accessory'), model.promiseAccessories(), 'last');
		item1Button = createItemButton($container.find('.equip-item1'), model.promiseItems());
		item2Button = createItemButton($container.find('.equip-item2'), model.promiseItems());
		instructionBox = snail.battleplan.instructionbox.create($container.find('.instructions'));
		
		// bind callbacks for user-initiated updates (i.e., via UI)
		$container.find('.snails button').click(function (e) { setSelectedSnail(e.target.firstChild.nodeValue) });
		weaponButton.addSelectionChangedHandler(function (index, weapon) { model.setWeapon(weapon) });
		shellButton.addSelectionChangedHandler(function (index, shell) { model.setShell(shell) });
		accessoryButton.addSelectionChangedHandler(function (index, accessory) { model.setAccessory(accessory) });
		item1Button.addSelectionChangedHandler(function (index, item) { model.setItem(0, item) });
		item1Button.addConditionChangedHandler(function (condition) { model.setItemCondition(0, condition) });
		item2Button.addSelectionChangedHandler(function (index, item) { model.setItem(1, item) });
		item2Button.addConditionChangedHandler(function (condition) { model.setItemCondition(1, condition) });
		instructionBox.addInstructionsChangedHandler(function () { model.setInstructions(instructionBox.getInstructions()) });
		
		// bind callback for model-initiated updates
		snail.battleplan.model.addBattlePlanUpdatedHandler(onBattlePlanUpdated);
	};
	
	return snail;
}(snail || {}));
