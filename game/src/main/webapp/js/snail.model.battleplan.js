var snail = (function (snail, $) {
	snail.model = snail.model || {};
	snail.model.battleplan = snail.model.battleplan || {};
	
	// private variables
	let ioLayer, getSnails, getWeapons, getShells, getAccessories, getItems;
	let selectedSnail, selectedWeapon, selectedShell, selectedAccessory;
	let selectedItems = [null, null];
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
	
	const presetKey = function (presetNumber) {
		return 'preset' + presetNumber;
	};
	
	const setBattlePlan = function (bp) {
		$.when(getSnails, getWeapons, getShells, getAccessories, getItems)
		.then(function (snailsResponse, weaponsResponse, shellsResponse, accessoriesResponse, itemsResponse) {
			const bpmodel = snail.model.battleplan;
			bpmodel.setSnail(findByProperty(snailsResponse, 'name', bp.snail));
			bpmodel.setWeapon(findByProperty(weaponsResponse, 'name', bp.weapon));
			bpmodel.setShell(findByProperty(shellsResponse, 'name', bp.shell));
			bpmodel.setAccessory(findByProperty(accessoriesResponse, 'name', bp.accessory));
			bpmodel.setItem(0, findByProperty(itemsResponse, 'name', bp.item1));
			bpmodel.setItem(1, findByProperty(itemsResponse, 'name', bp.item2));
			bpmodel.setItemCondition(0, bp.item1Rule);
			bpmodel.setItemCondition(1, bp.item2Rule);
			bpmodel.setInstructions(bp.instructions);
		});
	};
	
	// public methods
	snail.model.battleplan.init = function (io) {
		ioLayer = io;
		getSnails = ioLayer.promiseSnailInfo();
		getWeapons = ioLayer.promiseWeaponInfo();
		getShells = ioLayer.promiseShellInfo();
		getAccessories = ioLayer.promiseAccessoryInfo();
		getItems = ioLayer.promiseItemInfo();
	};
	
	snail.model.battleplan.addBattlePlanUpdatedHandler = function (handler) {
		battlePlanUpdatedHandlers.push(handler);
	};
	
	snail.model.battleplan.promiseSnails = function () { return getSnails; };
	snail.model.battleplan.promiseWeapons = function () { return getWeapons; };
	snail.model.battleplan.promiseShells = function () { return getShells; };
	snail.model.battleplan.promiseAccessories = function () { return getAccessories; };
	snail.model.battleplan.promiseItems = function () { return getItems; };
	
	snail.model.battleplan.setSnail = function (newSnail) {
		selectedSnail = newSnail;
		if (selectedSnail.name === 'doug') {
			getShells.then(function (shells) {
				snail.model.battleplan.setShell(findByProperty(shells, 'name', 'none'));
			});
		}
		notifyBattlePlanUpdatedHandlers('snail', selectedSnail);
	};
	
	snail.model.battleplan.setWeapon = function (weapon) {
		selectedWeapon = weapon;
		notifyBattlePlanUpdatedHandlers('weapon', selectedWeapon);
	};
	
	snail.model.battleplan.setShell = function (shell) {
		if (selectedSnail && selectedSnail.name !== 'doug') {
			selectedShell = shell;
			notifyBattlePlanUpdatedHandlers('shell', selectedShell);
		} else {
			getShells.then(function (shells) {
				selectedShell = findByProperty(shells, 'name', 'none');
				notifyBattlePlanUpdatedHandlers('shell', selectedShell);
			});
		}
	};
	
	snail.model.battleplan.setAccessory = function (accessory) {
		selectedAccessory = accessory;
		notifyBattlePlanUpdatedHandlers('accessory', selectedAccessory);
	};
	
	snail.model.battleplan.setItem = function (index, item) {
		selectedItems[index] = item;
		notifyBattlePlanUpdatedHandlers('item' + (index + 1), selectedItems[index]);
	};
	
	snail.model.battleplan.createHasCondition = function (player, stat, inequality, threshold) {
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
	
	snail.model.battleplan.createEnemyUsesCondition = function (item) {
		return {
			hasCondition: null,
			enemyUsesCondition: item
		};
	}
	
	snail.model.battleplan.setItemCondition = function (index, condition) {
		if (selectedItems[index].name === 'none') {
			itemConditions[index] = null;
		} else {
			itemConditions[index] = condition;
			notifyBattlePlanUpdatedHandlers('item' + (index + 1) + 'Rule', itemConditions[index]);
		}
	};
	
	snail.model.battleplan.itemConditionsAreIdentical = function (cond1, cond2) {
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
	
	snail.model.battleplan.createAttackInstruction = function () {
		return createInstruction('attack', null, null);
	};
	
	snail.model.battleplan.createUseItemInstruction = function (item) {
		return createInstruction('use', item, null);
	};
	
	snail.model.battleplan.createWaitForApInstruction = function (apThreshold) {
		return createInstruction('wait', null, apThreshold);
	};
	
	snail.model.battleplan.setInstructions = function (newInstructions) {
		instructions = newInstructions;
		notifyBattlePlanUpdatedHandlers('instructions', instructions);
	};
	
	snail.model.battleplan.instructionsAreIdentical = function (inst1, inst2) {
		return (inst1 && inst2
						&& inst1.type && inst2.type && inst1.type === inst2.type
						&& inst1.itemToUse === inst2.itemToUse
						&& inst1.apThreshold === inst2.apThreshold);
	};
	
	snail.model.battleplan.getBattlePlan = function () {
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
	};
	
	snail.model.battleplan.getAttack = function () {
		if (selectedSnail && selectedWeapon && selectedShell && selectedAccessory) {
			return selectedSnail.attackModifier + selectedWeapon.attackModifier + selectedShell.attackModifier + selectedAccessory.attackModifier;
		} else {
			return 0;
		}
	};
	
	snail.model.battleplan.getDefense = function () {
		if (selectedSnail && selectedWeapon && selectedShell && selectedAccessory) {
			return selectedSnail.defenseModifier + selectedWeapon.defenseModifier + selectedShell.defenseModifier + selectedAccessory.defenseModifier;
		} else {
			return 0;
		}
	};
	
	snail.model.battleplan.getSpeed = function () {
		if (selectedSnail && selectedWeapon && selectedShell && selectedAccessory) {
			return selectedSnail.speedModifier + selectedWeapon.speedModifier + selectedShell.speedModifier + selectedAccessory.speedModifier;
		} else {
			return 0;
		}
	};
	
	snail.model.battleplan.getPresetDisplayName = function (presetNumber) {
		const key = presetKey(presetNumber);
		const loadedObject = ioLayer.loadLocal(key);
		return loadedObject ? loadedObject.displayName : null;
	};
	
	snail.model.battleplan.saveBattlePlan = function (presetNumber, displayName) {
		const key = presetKey(presetNumber);
		const value = {
			displayName: displayName,
			battlePlan: this.getBattlePlan()
		};
		ioLayer.saveLocal(key, value);
	};
	
	snail.model.battleplan.loadBattlePlan = function (presetNumber) {
		const key = presetKey(presetNumber);
		const loadedObject = ioLayer.loadLocal(key);
		if (loadedObject) {
			setBattlePlan(loadedObject.battlePlan);
			return loadedObject.displayName;
		} else {
			return null;
		}
	};
	
	snail.model.battleplan.deleteBattlePlan = function (presetNumber) {
		const key = presetKey(presetNumber);
		ioLayer.deleteLocal(key);
	};
	
	return snail;
}(snail || {}, jQuery));
