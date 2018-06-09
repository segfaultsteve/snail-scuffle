var snail = (function (snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.instructionbox = {};
	
	const componentHtml = $('#components .components-instructionbox').html();
	
	snail.battleplan.instructionbox.create = function ($container) {
		// private variables
		const states = {
			collapsed: 'collapsed',
			expanding: 'expanding',
			expanded: 'expanded',
			collapsing: 'collapsing'
		};
		let instructionList = [];
		let instructionsChangedHandlers = [];
		let $instructionbox, $expandicon, $collapseicon, $instructions, $addbox, $defaultattack;
		
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
		
		const addNewInstruction = function () {
			const newInstruction = snail.battleplan.instruction.create($instructions);
			instructionList.push(newInstruction);
			return newInstruction;
		};
		
		const addHandlersForInstruction = function (instruction) {
			instruction.addInstructionUpdatedHandler(onInstructionUpdated);
			instruction.addInstructionRemovedHandler(onInstructionRemoved);
		};
		
		const notifyInstructionsChangedHandlers = function () {
			instructionsChangedHandlers.forEach(handler => handler());
		};
		
		const clearInstructions = function () {
			instructionList.forEach(instruction => instruction.remove());
			instructionList = [];
		};
		
		// callbacks
		const onAddInstruction = function () {
			const newInstruction = addNewInstruction();
			addHandlersForInstruction(newInstruction);
			notifyInstructionsChangedHandlers();
		};
		
		const onInstructionUpdated = function () {
			notifyInstructionsChangedHandlers();
		};
		
		const onInstructionRemoved = function (instruction) {
			const index = instructionList.indexOf(instruction);
			if (index > -1) {
				instructionList.splice(index, 1);
			}
			notifyInstructionsChangedHandlers();
		};
		
		// public methods
		const addInstructionsChangedHandler = function (handler) {
			instructionsChangedHandlers.push(handler);
		};
		
		const getInstructions = function () {
			let instructionData = [];
			for (let i = 0; i < instructionList.length; i++) {
				const instruction = instructionList[i].getData();
				instructionData.push(instruction);
			}
			return instructionData;
		};
		
		const setInstructions = function (newInstructions) {
			let instructionsAreTheSame = (newInstructions.length === instructionList.length);
			for (let i = 0; instructionsAreTheSame && i < instructionList.length; i++) {
				const oldInstructionData = instructionList[i].getData();
				instructionsAreTheSame = snail.battleplan.model.instructionsAreIdentical(oldInstructionData, newInstructions[i]);
			}
			
			if (!instructionsAreTheSame) {
				clearInstructions();
				for (let i = 0; i < newInstructions.length; i++) {
					const instruction = addNewInstruction();
					instruction.setData(newInstructions[i]);
					addHandlersForInstruction(instruction);
				}
				notifyInstructionsChangedHandlers();
			}
		};
		
		// init code
		$container.html(componentHtml);
		$instructionbox = $container.find('.instructionbox');
		$instructions = $instructionbox.find('.instructionbox-instructions');
		$addbox = $instructionbox.find('.instructionbox-addbox');
		$defaultattack = $instructionbox.find('.instructionbox-defaultattack');
		$expandicon = $container.find('.instructionbox-header-expandicon');
		$collapseicon = $container.find('.instructionbox-header-collapseicon');
		
		$addbox.on('click', '.instructionbox-addbox-addicon, .instructionbox-addbox-text', onAddInstruction);
		
		endCollapse();
		
		return {
			addInstructionsChangedHandler: addInstructionsChangedHandler,
			getInstructions: getInstructions,
			setInstructions: setInstructions
		};
	};
	
	return snail;
}(snail || {}, jQuery));
