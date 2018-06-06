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
		
		let $this, instructionType;
		
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
		};
		
		// init code
		$container.append(componentHtml);
		$this = $container.find('.instruction:last');
		$this.find('.instruction-type').change(onTypeChanged);
		$this.find('.instruction-removeicon').click(function () {
			$this.remove();
		});
		setState(states.attack);
	};
	
	return snail;
}(snail || {}, jQuery));