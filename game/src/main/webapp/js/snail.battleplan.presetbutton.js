var snail = (function(snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.presetbutton = {};
	
	const componentHtml = $('#components .components-presetbutton').html();
	
	snail.battleplan.presetbutton.create = function ($container, presetNumber) {
		// local variables
		const emptyText = '[Empty]';
		const defaultText = 'Preset ' + presetNumber;
		let $button, $save, $clear;
		
		// callbacks
		const onButtonClicked = function () {
			const displayName = snail.battleplan.model.loadBattlePlan(presetNumber);
			$button.text(displayName || emptyText);
		};
		
		const onSaveClicked = function () {
			snail.battleplan.model.saveBattlePlan(presetNumber, defaultText);
			$button.text(defaultText);
		};
		
		const onClearClicked = function () {
			snail.battleplan.model.deleteBattlePlan(presetNumber);
			$button.text(emptyText);
		};
		
		// init code
		$container.html(componentHtml);
		$button = $container.find('.presetbutton-button');
		$save = $container.find('.presetbutton-options-set');
		$clear = $container.find('.presetbutton-options-clear');
		
		$button.click(onButtonClicked);
		$save.click(onSaveClicked);
		$clear.click(onClearClicked);
		
		const displayName = snail.battleplan.model.getPresetDisplayName(presetNumber);
		$button.text(displayName || emptyText);
		
		return {
		};
	};
	
	return snail;
}(snail || {}, jQuery));