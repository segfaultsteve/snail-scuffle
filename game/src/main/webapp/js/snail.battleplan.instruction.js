var snail = (function (snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.instruction = {};
	
	const componentHtml = $('#components .components-instruction').html();
	
	snail.battleplan.instruction.create = function ($container) {
		// private variables
		const states = {
			attack: 'attack',
			use: 'use',
			wait: 'wait'
		};
		let instructionUpdatedHandlers = [];
		let instructionRemovedHandlers = [];
		let $this, thisInstruction, instructionType;
		
		// private methods
		const setState = function (newState) {
			$this.find('.instruction-item, .instruction-waitcondition').hide();
			
			if (newState === states.use) {
				$this.find('.instruction-item').show();
			} else if (newState === states.wait) {
				$this.find('.instruction-waitcondition').show();
			}
		};
		
		const resetTypeSpecificElements = function () {
			$this.find('.instruction-item').val('attack');
			$this.find('.instruction-waitcondition-ap').val('');
		};
		
		const notifyInstructionUpdated = function () {
			instructionUpdatedHandlers.forEach(handler => handler(thisInstruction));
		};
		
		// callbacks
		const onTypeChanged = function () {
			const lastType = instructionType;
			instructionType = this.value;
			
			if (instructionType !== lastType) {
				resetTypeSpecificElements();
				setState(instructionType);
			}
			
			notifyInstructionUpdated();
		};
		
		const onItemChanged = function () {
			notifyInstructionUpdated();
		};
		
		const onApThresholdChanged = function () {
			notifyInstructionUpdated();
		};
		
		const onRemoveIconClicked = function() {
			instructionRemovedHandlers.forEach(handler => handler(thisInstruction));
			$this.remove();
		};
		
		// public methods
		const addInstructionUpdatedHandler = function (callback) {
			instructionUpdatedHandlers.push(callback);
		};
		
		const addInstructionRemovedHandler = function (callback) {
			instructionRemovedHandlers.push(callback);
		};
		
		const getData = function () {
			const model = snail.battleplan.model;
			if (instructionType === 'attack') {
				return model.createAttackInstruction();
			} else if (instructionType === 'use') {
				const item = $this.find('.instruction-item').val();
				return model.createUseItemInstruction(item);
			} else {
				const apThreshold = $this.find('.instruction-waitcondition-ap').val();
				return model.createWaitForApInstruction(apThreshold);
			}
		};
		
		// init code
		$container.append(componentHtml);
		$this = $container.find('.instruction:last');
		$this.find('.instruction-type').change(onTypeChanged);
		$this.find('.instruction-item').change(onItemChanged);
		$this.find('.instruction-waitcondition-ap').change(onApThresholdChanged);
		$this.find('.instruction-removeicon').click(onRemoveIconClicked);
		
		instructionType = 'attack';
		setState(states.attack);
		
		thisInstruction = {
			addInstructionUpdatedHandler: addInstructionUpdatedHandler,
			addInstructionRemovedHandler: addInstructionRemovedHandler,
			getData: getData
		};
		return thisInstruction;
	};
	
	return snail;
}(snail || {}, jQuery));
