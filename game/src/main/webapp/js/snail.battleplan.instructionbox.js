var snail = (function (snail) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.instructionbox = {};
	
	snail.battleplan.instructionbox.init = function ($instructionbox) {
		// private variables
		const states = {
			collapsed: 'collapsed',
			expanding: 'expanding',
			expanded: 'expanded',
			collapsing: 'collapsing'
		};
		let instructionList = [];
		let equippedItems = [];
		let $expandicon, $collapseicon, $instructions, $addbox, $defaultattack;
		
		// private methods
		const beginExpand = function () {
			$instructionbox.removeClass(states.collapsed);
			$instructionbox.addClass(states.expanding);
			$instructions.removeClass('hidden');
			$addbox.removeClass('hidden');
			$defaultattack.removeClass('hidden');
			$instructionbox.one('animationend', endExpand);
		};
		
		const beginCollapse = function () {
			$instructionbox.removeClass(states.expanded);
			$instructionbox.addClass(states.collapsing);
			$instructionbox.one('animationend', endCollapse);
		};
		
		const endExpand = function () {
			$instructionbox.removeClass(states.expanding);
			$instructionbox.addClass(states.expanded);
			$expandicon.addClass('hidden');
			$collapseicon.removeClass('hidden');
			$collapseicon.one('click', beginCollapse);
		};
		
		const endCollapse = function() {
			$instructionbox.removeClass(states.collapsing);
			$instructionbox.addClass(states.collapsed);
			$collapseicon.addClass('hidden');
			$instructions.addClass('hidden');
			$addbox.addClass('hidden');
			$defaultattack.addClass('hidden');
			$expandicon.removeClass('hidden');
			$instructionbox.one('click', beginExpand);
		};
		
		const clearInstructions = function () {
			instructionList.forEach(instruction => instruction.remove());
			instructionList = [];
			updateModel();
		};
		
		const updateModel = function () {
			let instructionData = [];
			for (let i = 0; i < instructionList.length; i++) {
				const instruction = instructionList[i].getData();
				instructionData.push(instruction);
			}
			snail.model.battleplan.playerBp.setInstructions(instructionData);
		};
		
		// callbacks
		const onAddInstruction = function () {
			const newInstruction = snail.battleplan.instruction.create($instructions, onInstructionUpdated, onInstructionRemoved);
			newInstruction.setAvailableItems(equippedItems);
			instructionList.push(newInstruction);
			updateModel();
		};
		
		const onInstructionUpdated = function () {
			updateModel();
		};
		
		const onInstructionRemoved = function (instruction) {
			const index = instructionList.indexOf(instruction);
			if (index > -1) {
				instructionList.splice(index, 1);
			}
			updateModel();
		};
		
		// public methods
		snail.battleplan.instructionbox.setInstructions = function (newInstructions) {
			let instructionsAreTheSame = (newInstructions.length === instructionList.length);
			for (let i = 0; instructionsAreTheSame && i < instructionList.length; i++) {
				const oldInstructionData = instructionList[i].getData();
				instructionsAreTheSame = snail.model.battleplan.instructionsAreIdentical(oldInstructionData, newInstructions[i]);
			}
			
			if (!instructionsAreTheSame) {
				clearInstructions();
				for (let i = 0; i < newInstructions.length; i++) {
					const instruction = snail.battleplan.instruction.create($instructions);
					instructionList.push(instruction);
					instruction.setData(newInstructions[i]);
					instruction.onInstructionUpdated(onInstructionUpdated);
					instruction.onInstructionRemoved(onInstructionRemoved);
				}
				updateModel();
			}
		};
		
		snail.battleplan.instructionbox.refreshItemReferences = function () {
			equippedItems = snail.model.battleplan.playerBp.getItems().map(i => i.name).filter(i => i !== 'none');
			for (let i = 0; i < instructionList.length; i++) {
				const data = instructionList[i].getData();
				if (data.type === 'use' && !equippedItems.includes(data.itemToUse)) {
					instructionList[i].remove();
					instructionList.splice(i, 1);
					i--;
				} else {
					instructionList[i].setAvailableItems(equippedItems);
				}
			}
		};
		
		// init code
		$instructions = $instructionbox.find('.instructionbox-instructions');
		$addbox = $instructionbox.find('.instructionbox-addbox');
		$defaultattack = $instructionbox.find('.instructionbox-defaultattack');
		$expandicon = $instructionbox.find('.instructionbox-header-expandicon');
		$collapseicon = $instructionbox.find('.instructionbox-header-collapseicon');
		
		$addbox.on('click', '.instructionbox-addbox-addicon, .instructionbox-addbox-text', onAddInstruction);
		
		snail.model.battle.addEventHandler(function (event) {
			if (event === 'battleStarted' || event === 'roundComplete') {
				clearInstructions();
			}
		});
		
		endCollapse();
	};
	
	return snail;
}(snail || {}));
