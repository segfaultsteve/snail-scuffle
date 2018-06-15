var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.model = snail.model || {};
	snail.model.battleplan = snail.model.battleplan || {};
	
	// private variables
	const bpmodel = snail.model.battleplan;
	let $battleplan, snails, snailButtons, weaponButton, shellButton, accessoryButton, item1Button, item2Button, instructionBox;
	
	// private methods
	const createMenuButton = function ($container, itemsPromise, defaultSelectionIndex, selectionChangedHandler) {
		const onSelectionChanged = function (selectedIndex, selectedItem) {
			selectionChangedHandler(selectedItem)
		};
		const button = snail.battleplan.menubutton.create($container, onSelectionChanged);
		
		itemsPromise.done(function (items) {
			button.setOptionsList(items, defaultSelectionIndex);
		});
		
		return button;
	};
	
	const createItemButton = function ($container, itemsPromise, itemSlot) {
		const onItemChanged = function (selectedIndex, selectedItem) { bpmodel.setItem(itemSlot, selectedItem) };
		const onItemConditionChanged = function (condition) { bpmodel.setItemCondition(itemSlot, condition) };
		const button = snail.battleplan.itembutton.create($container, onItemChanged, onItemConditionChanged);
		
		itemsPromise.done(function (items) {
			button.setOptionsList(items, 'last');
		});
		
		return button;
	};
	
	const setSelectedSnail = function (displayName) {
		const currentSnail = $battleplan.find('.snails .selected-snail').text();
		if (displayName !== currentSnail) {
			$battleplan.find('.snails button').removeClass('selected-snail');
			const newSnail = snails.filter(snail => snail.displayName === displayName)[0];
			if (newSnail) {
				snailButtons[newSnail.name].addClass('selected-snail');
				bpmodel.setSnail(newSnail);
			}
		}
	};
	
	// callbacks
	const onBattlePlanUpdated = function (updatedElement, newValue) {
		switch (updatedElement) {
			case 'snail':
				setSelectedSnail(newValue.displayName);
				break;
			case 'weapon':
				weaponButton.setSelectedOption(newValue.displayName);
				break;
			case 'shell':
				shellButton.setSelectedOption(newValue.displayName);
				break;
			case 'accessory':
				accessoryButton.setSelectedOption(newValue.displayName);
				break;
			case 'item1':
				item1Button.setSelectedOption(newValue.displayName);
				break;
			case 'item2':
				item2Button.setSelectedOption(newValue.displayName);
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
		
		snailButtons = {
			dale: $container.find('.snails-dale'),
			gail: $container.find('.snails-gail'),
			todd: $container.find('.snails-todd'),
			doug: $container.find('.snails-doug')
		};
		weaponButton = createMenuButton($container.find('.equip-weapon'), bpmodel.promiseWeapons(), 0, bpmodel.setWeapon);
		shellButton = createMenuButton($container.find('.equip-shell'), bpmodel.promiseShells(), 'last', bpmodel.setShell);
		accessoryButton = createMenuButton($container.find('.equip-accessory'), bpmodel.promiseAccessories(), 'last', bpmodel.setAccessory);
		item1Button = createItemButton($container.find('.equip-item1'), bpmodel.promiseItems(), 0);
		item2Button = createItemButton($container.find('.equip-item2'), bpmodel.promiseItems(), 1);
		instructionBox = snail.battleplan.instructionbox;
		instructionBox.init($container.find('.instructionbox'));
		for (let i = 0; i < 4; i++) {
			let $preset = $container.find('.preset' + (i + 1));
			snail.battleplan.presetbutton.init($preset, i + 1);
		}
		
		$container.find('.snails button').click(function (e) { setSelectedSnail(e.target.firstChild.nodeValue) });
		bpmodel.addBattlePlanUpdatedHandler(onBattlePlanUpdated);
		
		bpmodel.promiseSnails().done(function (snailList) {
			snails = snailList;
			setSelectedSnail(snails[0].displayName);
		});
	};
	
	return snail;
}(snail || {}));
