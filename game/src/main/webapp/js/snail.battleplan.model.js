var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.model = snail.battleplan.model || {};
	
	// private variables
	let getSnails, getWeapons, getShells, getAccessories, getItems;
	let selectedSnail, selectedWeapon, selectedShell, selectedAccessory;
	let selectedItems = ['none', 'none'];
	let itemConditions = [null, null];
	let instructions = [];
	
	// private methods
	const findByDisplayName = function (equipInfoArray, displayName) {
		for (let i = 0; i < equipInfoArray.length; i++) {
			if (equipInfoArray[i].displayName === displayName) {
				return equipInfoArray[i].name;
			}
		}
		
		throw `invalid equipment name: '${displayName}'`;
	};
	
	const createInstruction = function (type, itemToUse, apThreshold) {
		return {
			type: type,
			itemToUse: itemToUse,
			apThreshold: apThreshold
		};
	};
	
	// public methods
	snail.battleplan.model.init = function (data) {
		getSnails = data.promiseSnailInfo();
		getWeapons = data.promiseWeaponInfo();
		getShells = data.promiseShellInfo();
		getAccessories = data.promiseAccessoryInfo();
		getItems = data.promiseItemInfo();
	};
	
	snail.battleplan.model.promiseSnails = function () { return getSnails; };
	snail.battleplan.model.promiseWeapons = function () { return getWeapons; };
	snail.battleplan.model.promiseShells = function () { return getShells; };
	snail.battleplan.model.promiseAccessories = function () { return getAccessories; };
	snail.battleplan.model.promiseItems = function () { return getItems; };
	
	snail.battleplan.model.setSnail = function (newSnail) {
		getSnails.then(function (snails) {
			selectedSnail = findByDisplayName(snails, newSnail);
		});
	};
	
	snail.battleplan.model.setWeapon = function (weapon) {
		getWeapons.then(function (weapons) {
			selectedWeapon = findByDisplayName(weapons, weapon);
		});
	};
	
	snail.battleplan.model.setShell = function (shell) {
		getShells.then(function (shells) {
			selectedShell = findByDisplayName(shells, shell);
		});
	};
	
	snail.battleplan.model.setAccessory = function (accessory) {
		getAccessories.then(function (accessories) {
			selectedAccessory = findByDisplayName(accessories, accessory);
		});
	};
	
	snail.battleplan.model.setItem = function (index, item) {
		getItems.then(function (items) {
			selectedItems[index] = findByDisplayName(items, item);
		});
	};
	
	snail.battleplan.model.createHasCondition = function (player, stat, inequality, threshold) {
		return {
			hasCondition: {
				player: player,
				stat: stat,
				inequality: inequality,
				threshold: threshold
			},
			enemyUsesCondition: null
		};
	};
	
	snail.battleplan.model.createEnemyUsesCondition = function (item) {
		return {
			hasCondition: null,
			enemyUsesCondition: item
		};
	}
	
	snail.battleplan.model.setItemCondition = function (index, condition) {
		itemConditions[index] = condition;
	};
	
	snail.battleplan.model.createAttackInstruction = function () {
		return createInstruction('attack', null, null);
	};
	
	snail.battleplan.model.createUseItemInstruction = function (item) {
		return createInstruction('use', item, null);
	};
	
	snail.battleplan.model.createWaitForApInstruction = function (apThreshold) {
		return createInstruction('wait', null, apThreshold);
	};
	
	snail.battleplan.model.setInstructions = function (newInstructions) {
		instructions = newInstructions;
	};
	
	snail.battleplan.model.getBattlePlan = function () {
		return {
			snail: selectedSnail,
			weapon: selectedWeapon,
			shell: selectedShell,
			accessory: selectedAccessory,
			item1: selectedItems[0],
			item2: selectedItems[1],
			item1Rule: itemConditions[0],
			item2Rule: itemConditions[1],
			instructions: instructions
		};
	}
	
	return snail;
}(snail || {}));
