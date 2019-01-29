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
		let availableItems = [];
		
		// private methods
		const setState = function (newState) {
			$this.find('.instruction-item, .instruction-waitcondition').addClass('hidden');
			
			if (newState === states.use) {
				$this.find('.instruction-item').removeClass('hidden');
			} else if (newState === states.wait) {
				$this.find('.instruction-waitcondition').removeClass('hidden');
			}
		};
		
		const resetTypeSpecificElements = function () {
			if (availableItems.length > 0) {
				$this.find('.instruction-item').val(availableItems[0]);
			}
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
			const bpmodel = snail.model.battleplan;
			if (instructionType === 'attack') {
				return bpmodel.createAttackInstruction();
			} else if (instructionType === 'use') {
				const item = $this.find('.instruction-item').val();
				return bpmodel.createUseItemInstruction(item);
			} else {
				const apThreshold = $this.find('.instruction-waitcondition-ap').val();
				return bpmodel.createWaitForApInstruction(apThreshold);
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
		
		const setAvailableItems = function (itemNames) {
			availableItems = itemNames.sort();
			
			$this.find('.instruction-item option').addClass('hidden');
			for (let i = 0; i < availableItems.length; i++) {
				$this.find('.instruction-item option[value=' + availableItems[i] + ']').removeClass('hidden');
			}
			
			if (availableItems.length > 0) {
				$this.find('.instruction-type option[value=use]').removeClass('hidden');
			} else {
				$this.find('.instruction-type option[value=use]').addClass('hidden');
			}
		};
		
		const remove = function () {
			$this.remove();
		};
		
		// init code
		$container.append(componentHtml);
		$this = $container.find('.instruction:last');
		snail.io.promiseItemInfo().done(function (items) {
			const $items = $this.find('.instruction-item');
			items
				.map(i => '<option value="' + i.name + '">' + i.displayName + '</option>')
				.forEach(i => $items.append(i));
		});
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
			setAvailableItems: setAvailableItems,
			remove: remove
		};
		return thisInstruction;
	};
	
	return snail;
}(snail || {}, jQuery));
