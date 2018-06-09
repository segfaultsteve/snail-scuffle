var snail = (function (snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.instruction = {};
	
	const componentHtml = $('#components .components-instruction').html();
	
	snail.battleplan.instruction.create = function ($container, onInstructionUpdated, onInstructionRemoved) {
		// private variables
		const states = {
			attack: 'attack',
			use: 'use',
			wait: 'wait'
		};
		let $this, thisInstruction, instructionUpdatedHandler, instructionRemovedHandler, instructionType;
		
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
		
		// callbacks
		const onTypeChanged = function () {
			const lastType = instructionType;
			instructionType = this.value;
			
			if (instructionType !== lastType) {
				resetTypeSpecificElements();
				setState(instructionType);
			}
			
			instructionUpdatedHandler(thisInstruction);
		};
		
		const onItemChanged = function () {
			instructionUpdatedHandler(thisInstruction);
		};
		
		const onApThresholdChanged = function () {
			instructionUpdatedHandler(thisInstruction);
		};
		
		const onRemoveIconClicked = function() {
			instructionRemovedHandler(thisInstruction);
			thisInstruction.remove();
		};
		
		// public methods
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
		
		const setData = function (newData) {
			instructionType = newData.type;
			setState(newData.type);
			$this.find('.instruction-type').val(newData.type);
			
			if (newData.itemToUse) {
				$this.find('.instruction-item').val(newData.itemToUse);
			} else if (newData.apThreshold) {
				$this.find('.instruction-waitcondition-ap').val(newData.apThreshold);
			}
			
			instructionUpdatedHandler(thisInstruction);
		};
		
		const remove = function () {
			$this.remove();
		};
		
		// init code
		$container.append(componentHtml);
		$this = $container.find('.instruction:last');
		$this.find('.instruction-type').change(onTypeChanged);
		$this.find('.instruction-item').change(onItemChanged);
		$this.find('.instruction-waitcondition-ap').change(onApThresholdChanged);
		$this.find('.instruction-removeicon').click(onRemoveIconClicked);
		instructionUpdatedHandler = onInstructionUpdated || function () { };
		instructionRemovedHandler = onInstructionRemoved || function () { };
		
		instructionType = 'attack';
		setState(states.attack);
		
		thisInstruction = {
			onInstructionUpdated: function (handler) { instructionUpdatedHandler = handler },
			onInstructionRemoved: function (handler) { instructionRemovedHandler = handler },
			getData: getData,
			setData: setData,
			remove: remove
		};
		return thisInstruction;
	};
	
	return snail;
}(snail || {}, jQuery));
