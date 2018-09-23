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
		let $expandicon, $collapseicon, $instructions, $addbox, $defaultattack;
		
		// private methods
		const beginExpand = function () {
			$instructionbox.removeClass(states.collapsed);
			$instructionbox.addClass(states.expanding);
			$instructions.show();
			$addbox.show();
			$defaultattack.show();
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
			$expandicon.hide();
			$collapseicon.show();
			$collapseicon.one('click', beginCollapse);
		};
		
		const endCollapse = function() {
			$instructionbox.removeClass(states.collapsing);
			$instructionbox.addClass(states.collapsed);
			$collapseicon.hide();
			$instructions.hide();
			$addbox.hide();
			$defaultattack.hide();
			$expandicon.show();
			$instructionbox.one('click', beginExpand);
		};
		
		const clearInstructions = function () {
			instructionList.forEach(instruction => instruction.remove());
			instructionList = [];
		};
		
		const updateModel = function () {
			let instructionData = [];
			for (let i = 0; i < instructionList.length; i++) {
				const instruction = instructionList[i].getData();
				instructionData.push(instruction);
			}
			snail.model.battleplan.playerBp.setInstructions(instructionData)
		};
		
		// callbacks
		const onAddInstruction = function () {
			const newInstruction = snail.battleplan.instruction.create($instructions, onInstructionUpdated, onInstructionRemoved);
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
		
		// init code
		$instructions = $instructionbox.find('.instructionbox-instructions');
		$addbox = $instructionbox.find('.instructionbox-addbox');
		$defaultattack = $instructionbox.find('.instructionbox-defaultattack');
		$expandicon = $instructionbox.find('.instructionbox-header-expandicon');
		$collapseicon = $instructionbox.find('.instructionbox-header-collapseicon');
		
		$addbox.on('click', '.instructionbox-addbox-addicon, .instructionbox-addbox-text', onAddInstruction);
		
		endCollapse();
	};
	
	return snail;
}(snail || {}));
