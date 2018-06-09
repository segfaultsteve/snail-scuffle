var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.model = snail.battleplan.model || {};
	
	// private variables
	let getSnails, getWeapons, getShells, getAccessories, getItems;
	let selectedSnail, selectedWeapon, selectedShell, selectedAccessory;
	let selectedItems = ['none', 'none'];
	let itemConditions = [null, null];
	let instructions = [];
	let battlePlanUpdatedHandlers = [];
	
	// private methods
	const findByProperty = function (equipInfoArray, propertyName, propertyValue) {
		for (let i = 0; i < equipInfoArray.length; i++) {
			if (equipInfoArray[i][propertyName] === propertyValue) {
				return equipInfoArray[i];
			}
		}
		return null;
	};
	
	const notifyBattlePlanUpdatedHandlers = function (updatedElement, newValue) {
		battlePlanUpdatedHandlers.forEach(handler => handler(updatedElement, newValue));
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
	
	snail.battleplan.model.addBattlePlanUpdatedHandler = function (handler) {
		battlePlanUpdatedHandlers.push(handler);
	};
	
	snail.battleplan.model.promiseSnails = function () { return getSnails; };
	snail.battleplan.model.promiseWeapons = function () { return getWeapons; };
	snail.battleplan.model.promiseShells = function () { return getShells; };
	snail.battleplan.model.promiseAccessories = function () { return getAccessories; };
	snail.battleplan.model.promiseItems = function () { return getItems; };
	
	snail.battleplan.model.setSnail = function (newSnail) {
		getSnails.then(function (snails) {
			selectedSnail = findByProperty(snails, 'displayName', newSnail);
			notifyBattlePlanUpdatedHandlers('snail', selectedSnail.displayName);
		});
	};
	
	snail.battleplan.model.setWeapon = function (weapon) {
		getWeapons.then(function (weapons) {
			selectedWeapon = findByProperty(weapons, 'displayName', weapon);
			notifyBattlePlanUpdatedHandlers('weapon', selectedWeapon.displayName);
		});
	};
	
	snail.battleplan.model.setShell = function (shell) {
		getShells.then(function (shells) {
			selectedShell = findByProperty(shells, 'displayName', shell);
			notifyBattlePlanUpdatedHandlers('shell', selectedShell.displayName);
		});
	};
	
	snail.battleplan.model.setAccessory = function (accessory) {
		getAccessories.then(function (accessories) {
			selectedAccessory = findByProperty(accessories, 'displayName', accessory);
			notifyBattlePlanUpdatedHandlers('accessory', selectedAccessory.displayName);
		});
	};
	
	snail.battleplan.model.setItem = function (index, item) {
		getItems.then(function (items) {
			selectedItems[index] = findByProperty(items, 'displayName', item);
			notifyBattlePlanUpdatedHandlers('item' + (index + 1), selectedItems[index].displayName);
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
		notifyBattlePlanUpdatedHandlers('item' + (index + 1) + 'Rule', itemConditions[index]);
	};
	
	snail.battleplan.model.itemConditionsAreIdentical = function (cond1, cond2) {
		if (cond1.hasCondition) {
			const hc1 = cond1.hasCondition;
			const hc2 = cond2.hasCondition;
			return (hc1 && hc2
							&& hc1.player === hc2.player
							&& hc1.stat === hc2.stat
							&& hc1.inequality === hc2.inequality
							&& hc1.threshold === hc2.threshold);
		} else {
			return cond1.enemyUsesCondition === cond2.enemyUsesCondition;
		}
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
		notifyBattlePlanUpdatedHandlers('instructions', instructions);
	};
	
	snail.battleplan.model.instructionsAreIdentical = function (inst1, inst2) {
		return (inst1 && inst2
						&& inst1.type && inst2.type && inst1.type === inst2.type
						&& inst1.itemToUse === inst2.itemToUse
						&& inst1.apThreshold === inst2.apThreshold);
	};
	
	snail.battleplan.model.getBattlePlan = function () {
		return {
			snail: selectedSnail.name,
			weapon: selectedWeapon.name,
			shell: selectedShell.name,
			accessory: selectedAccessory.name,
			item1: selectedItems[0].name,
			item2: selectedItems[1].name,
			item1Rule: itemConditions[0],
			item2Rule: itemConditions[1],
			instructions: instructions
		};
	}
	
	return snail;
}(snail || {}));
