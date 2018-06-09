var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.model = snail.battleplan.model || {};
	
	// private variables
	const model = snail.battleplan.model;
	let $battleplan, snailButtons, weaponButton, shellButton, accessoryButton, item1Button, item2Button, instructionBox;
	
	// private methods
	const createMenuButton = function ($container, itemsPromise, defaultSelectionIndex, selectionChangedHandler) {
		const onSelectionChanged = function (selectedIndex, selectedItem) { selectionChangedHandler(selectedItem) };
		const button = snail.battleplan.menubutton.create($container, onSelectionChanged);
		
		itemsPromise.done(function (items) {
			const displayNames = items.map(i => i.displayName);
			button.setOptionsList(displayNames, defaultSelectionIndex);
		});
		
		return button;
	};
	
	const createItemButton = function ($container, itemsPromise, itemSlot) {
		const onItemChanged = function (selectedIndex, selectedItem) { model.setItem(itemSlot, selectedItem) };
		const onItemConditionChanged = function (condition) { model.setItemCondition(itemSlot, condition) };
		const button = snail.battleplan.itembutton.create($container, onItemChanged, onItemConditionChanged);
		
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
				item1Button.setCondition(newValue);
				break;
			case 'item2Rule':
				item2Button.setCondition(newValue);
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
		weaponButton = createMenuButton($container.find('.equip-weapon'), model.promiseWeapons(), 0, model.setWeapon);
		shellButton = createMenuButton($container.find('.equip-shell'), model.promiseShells(), 'last', model.setShell);
		accessoryButton = createMenuButton($container.find('.equip-accessory'), model.promiseAccessories(), 'last', model.setAccessory);
		item1Button = createItemButton($container.find('.equip-item1'), model.promiseItems(), 0);
		item2Button = createItemButton($container.find('.equip-item2'), model.promiseItems(), 1);
		instructionBox = snail.battleplan.instructionbox;
		instructionBox.init($container.find('.instructions'));
		$container.find('.snails button').click(function (e) { setSelectedSnail(e.target.firstChild.nodeValue) });
		snail.battleplan.model.addBattlePlanUpdatedHandler(onBattlePlanUpdated);
	};
	
	return snail;
}(snail || {}));
