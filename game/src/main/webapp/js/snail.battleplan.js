var snail = (function (snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.model = snail.model || {};
	snail.model.battleplan = snail.model.battleplan || {};
	
	// private variables
	let $battleplan, snails, snailButtons, weaponButton, shellButton, accessoryButton, item1Button, item2Button, instructionBox, playerBp;
	
	// private methods
	const createMenuButton = function ($container, itemsPromise, defaultSelectionIndex, selectionChangedHandler) {
		const onSelectionChanged = function (selectedIndex, selectedItem) {
			selectionChangedHandler(selectedItem);
		};
		const button = snail.battleplan.menubutton.create($container, onSelectionChanged);
		
		itemsPromise.done(function (items) {
			button.setOptionsList(items, defaultSelectionIndex);
		});
		
		return button;
	};
	
	const createItemButton = function ($container, itemsPromise, itemSlot) {
		const onItemChanged = function (selectedIndex, selectedItem) { playerBp.setItem(itemSlot, selectedItem) };
		const onItemConditionChanged = function (condition) { playerBp.setItemCondition(itemSlot, condition) };
		const button = snail.battleplan.itembutton.create($container, onItemChanged, onItemConditionChanged);
		
		itemsPromise.done(function (items) {
			button.setOptionsList(items, 'last');
		});
		
		return button;
	};
	
	const setSnail = function (displayName) {
		const newSnail = snails.filter(snail => snail.displayName === displayName)[0];
		if (newSnail) {
			playerBp.setSnail(newSnail);
		}
	};
	
	const updateSnailButtons = function (newSnail) {
		const $selectedSnailButton = $battleplan.find('.snails .selected-snail');
		const selectedSnailDisplayName = $selectedSnailButton.find('.snails-button-name').text();
		if (newSnail.displayName !== selectedSnailDisplayName) {
			$selectedSnailButton.removeClass('selected-snail');
			snailButtons[newSnail.name].addClass('selected-snail');
			
			if (newSnail.name === 'doug') {
				shellButton.disable();
			} else {
				shellButton.enable();
			}
		}
	};
	
	// callbacks
	const onBattlePlanUpdated = function (updatedElement, newValue) {
		switch (updatedElement) {
			case 'snail':
				updateSnailButtons(newValue);
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
	
	const onSnailButtonClicked = function (e) {
		const $button = $(e.target).closest('.snails-button');
		const displayName = $button.find('.snails-button-name').text();
		setSnail(displayName);
	};
	
	// public methods
	snail.battleplan.init = function ($container) {
		$battleplan = $container;
		playerBp = snail.model.battleplan.playerBp;
		
		snailButtons = {
			dale: $battleplan.find('.snails-dale'),
			gail: $battleplan.find('.snails-gail'),
			todd: $battleplan.find('.snails-todd'),
			doug: $battleplan.find('.snails-doug')
		};
		weaponButton = createMenuButton($battleplan.find('.equip-weapon'), snail.io.promiseWeaponInfo(), 0, playerBp.setWeapon);
		shellButton = createMenuButton($battleplan.find('.equip-shell'), snail.io.promiseShellInfo(), 'last', playerBp.setShell);
		accessoryButton = createMenuButton($battleplan.find('.equip-accessory'), snail.io.promiseAccessoryInfo(), 'last', playerBp.setAccessory);
		item1Button = createItemButton($battleplan.find('.equip-item1'), snail.io.promiseItemInfo(), 0);
		item2Button = createItemButton($battleplan.find('.equip-item2'), snail.io.promiseItemInfo(), 1);
		instructionBox = snail.battleplan.instructionbox;
		instructionBox.init($battleplan.find('.instructionbox'));
		for (let i = 0; i < 4; i++) {
			let $preset = $battleplan.find('.preset' + (i + 1));
			snail.battleplan.presetbutton.init($preset, i + 1);
		}
		snail.battleplan.stats.init($battleplan.find('.info-playerstats'));
		
		$battleplan.find('.snails-button').click(onSnailButtonClicked);
		playerBp.addBattlePlanUpdatedHandler(onBattlePlanUpdated);
		$battleplan.find('.submit').click(function () {
			snail.model.battle.submitBattlePlan(playerBp.get());
		});
		
		snail.io.promiseSnailInfo().done(function (snailList) {
			snails = snailList;
			for (let i = 0; i < snails.length; i++) {
				snailButtons[snails[i].name].find('.snails-button-description').text(snails[i].description);
			}
			playerBp.setSnail(snails[0]);
		});
	};
	
	return snail;
}(snail || {}, jQuery));
