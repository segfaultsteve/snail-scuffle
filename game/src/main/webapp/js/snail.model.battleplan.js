var snail = (function (snail, $) {
	snail.model = snail.model || {};
	snail.model.battleplan = snail.model.battleplan || {};
	
	// private variables
	let ioLayer;
	
	// private methods
	const findByProperty = function (equipInfoArray, propertyName, propertyValue) {
		for (let i = 0; i < equipInfoArray.length; i++) {
			if (equipInfoArray[i][propertyName] === propertyValue) {
				return equipInfoArray[i];
			}
		}
		return null;
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
	
	// public methods
	snail.model.battleplan.init = function (io) {
		ioLayer = io;
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
	
	snail.model.battleplan.createAttackInstruction = function () {
		return createInstruction('attack', null, null);
	};
	
	snail.model.battleplan.createUseItemInstruction = function (item) {
		return createInstruction('use', item, null);
	};
	
	snail.model.battleplan.createWaitForApInstruction = function (apThreshold) {
		return createInstruction('wait', null, apThreshold);
	};
	
	snail.model.battleplan.instructionsAreIdentical = function (inst1, inst2) {
		return (inst1 && inst2
						&& inst1.type && inst2.type && inst1.type === inst2.type
						&& inst1.itemToUse === inst2.itemToUse
						&& inst1.apThreshold === inst2.apThreshold);
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
			battlePlan: snail.model.battleplan.playerBp.get()
		};
		ioLayer.saveLocal(key, value);
	};
	
	snail.model.battleplan.loadBattlePlan = function (presetNumber) {
		const key = presetKey(presetNumber);
		const loadedObject = ioLayer.loadLocal(key);
		if (loadedObject) {
			snail.model.battleplan.playerBp.set(loadedObject.battlePlan);
			return loadedObject.displayName;
		} else {
			return null;
		}
	};
	
	snail.model.battleplan.deleteBattlePlan = function (presetNumber) {
		const key = presetKey(presetNumber);
		ioLayer.deleteLocal(key);
	};
	
	// battleplan object
	snail.model.battleplan.create = function (bp) {
		let selectedSnail, selectedWeapon, selectedShell, selectedAccessory;
		let selectedItems = [null, null];
		let itemConditions = [null, null];
		let instructions = [];
		let battlePlanUpdatedHandlers = [];
		let itemUsedHandlers = [];
		
		const addBattlePlanUpdatedHandler = function (handler) {
			battlePlanUpdatedHandlers.push(handler);
		};
		
		const addItemUsedHandler = function (handler) {
			itemUsedHandlers.push(handler);
		}
		
		const notifyBattlePlanUpdatedHandlers = function (updatedElement, newValue) {
			battlePlanUpdatedHandlers.forEach(handler => handler(updatedElement, newValue));
		};
		
		const getAttack = function () {
			if (selectedSnail && selectedWeapon && selectedShell && selectedAccessory) {
				let attack = selectedSnail.attackModifier + selectedWeapon.attackModifier + selectedShell.attackModifier + selectedAccessory.attackModifier;
				if (selectedAccessory.name === 'salted_shell') {
					attack *= snail.model.battle.saltedShellAttackMultiplier();
				} else if (selectedAccessory.name === 'charged_attack') {
					attack *= 1 + snail.model.battle.chargedAttackModifier();
				} else if (selectedAccessory.name === 'adrenaline') {
					attack += snail.model.battle.adrenalineModifier();
				}
				return attack;
			} else {
				return 0;
			}
		};

		const getDefense = function () {
			if (selectedSnail && selectedWeapon && selectedShell && selectedAccessory) {
				let defense = selectedSnail.defenseModifier + selectedWeapon.defenseModifier + selectedShell.defenseModifier + selectedAccessory.defenseModifier;
				if (selectedAccessory.name === 'salted_shell') {
					defense *= snail.model.battle.saltedShellDefenseMultiplier();
				}
				return defense;
			} else {
				return 0;
			}
		};

		const getSpeed = function () {
			if (selectedSnail && selectedWeapon && selectedShell && selectedAccessory) {
				return selectedSnail.speedModifier + selectedWeapon.speedModifier + selectedShell.speedModifier + selectedAccessory.speedModifier;
			} else {
				return 0;
			}
		};
		
		const getWeaponApCost = function () {
			if (selectedWeapon) {
				return selectedWeapon.other.apCost;
			} else {
				return 0;
			}
		};
		
		const getItems = function () {
			return selectedItems.filter(i => i !== null);
		};
		
		const get = function () {
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
		
		const setSnail = function (newSnail) {
			selectedSnail = newSnail;
			if (selectedSnail.name === 'doug') {
				ioLayer.promiseShellInfo().then(function (shells) {
					setShell(findByProperty(shells, 'name', 'none'));
				});
			}
			notifyBattlePlanUpdatedHandlers('snail', selectedSnail);
		};
		
		const setWeapon = function (weapon) {
			selectedWeapon = weapon;
			notifyBattlePlanUpdatedHandlers('weapon', selectedWeapon);
		};

		const setShell = function (shell) {
			if (selectedSnail && selectedSnail.name !== 'doug') {
				selectedShell = shell;
				notifyBattlePlanUpdatedHandlers('shell', selectedShell);
			} else {
				ioLayer.promiseShellInfo().then(function (shells) {
					selectedShell = findByProperty(shells, 'name', 'none');
					notifyBattlePlanUpdatedHandlers('shell', selectedShell);
				});
			}
		};

		const setAccessory = function (accessory) {
			selectedAccessory = accessory;
			notifyBattlePlanUpdatedHandlers('accessory', selectedAccessory);
		};

		const setItem = function (index, item) {
			selectedItems[index] = item;
			notifyBattlePlanUpdatedHandlers('item' + (index + 1), selectedItems[index]);
		};
		
		const setItemCondition = function (index, condition) {
			if (selectedItems[index].name === 'none') {
				itemConditions[index] = null;
			} else {
				itemConditions[index] = condition;
				notifyBattlePlanUpdatedHandlers('item' + (index + 1) + 'Rule', itemConditions[index]);
			}
		};
		
		const setInstructions = function (newInstructions) {
			instructions = newInstructions;
			notifyBattlePlanUpdatedHandlers('instructions', instructions);
		};
		
		const set = function (bp) {
			$.when(
				ioLayer.promiseSnailInfo(),
				ioLayer.promiseWeaponInfo(),
				ioLayer.promiseShellInfo(),
				ioLayer.promiseAccessoryInfo(),
				ioLayer.promiseItemInfo()
			)
			.then(function (snailsResponse, weaponsResponse, shellsResponse, accessoriesResponse, itemsResponse) {
				setSnail(findByProperty(snailsResponse, 'name', bp.snail));
				setWeapon(findByProperty(weaponsResponse, 'name', bp.weapon));
				setShell(findByProperty(shellsResponse, 'name', bp.shell));
				setAccessory(findByProperty(accessoriesResponse, 'name', bp.accessory));
				setItem(0, findByProperty(itemsResponse, 'name', bp.item1));
				setItem(1, findByProperty(itemsResponse, 'name', bp.item2));
				setItemCondition(0, bp.item1Rule);
				setItemCondition(1, bp.item2Rule);
				setInstructions(bp.instructions);
			});
		};
		
		const registerItemUsed = function (itemName) {
			ioLayer.promiseItemInfo().done(function (items) {
				for (let i = 0; i < selectedItems.length; i++) {
					if (selectedItems[i] && selectedItems[i].name === itemName) {
						setItem(i, findByProperty(items, 'name', 'none'));
						setItemCondition(i, null);
						itemUsedHandlers.forEach(handler => handler(i));
						break;
					}
				}
			});
		};
		
		if (bp) {
			set(bp);
		}
		
		return {
			addBattlePlanUpdatedHandler: addBattlePlanUpdatedHandler,
			addItemUsedHandler: addItemUsedHandler,
			getAttack: getAttack,
			getDefense: getDefense,
			getSpeed: getSpeed,
			getWeaponApCost: getWeaponApCost,
			getItems: getItems,
			get: get,
			setSnail: setSnail,
			setWeapon: setWeapon,
			setShell: setShell,
			setAccessory: setAccessory,
			setItem: setItem,
			setItemCondition: setItemCondition,
			setInstructions: setInstructions,
			set: set,
			registerItemUsed: registerItemUsed
		};
	};
	
	// public fields
	snail.model.battleplan.playerBp = snail.model.battleplan.create();
	snail.model.battleplan.enemyBp = snail.model.battleplan.create();
	
	return snail;
}(snail || {}, jQuery));
