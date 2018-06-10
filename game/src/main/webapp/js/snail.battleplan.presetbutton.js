var snail = (function(snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.presetbutton = {};
	
	const componentHtml = $('#components .components-presetbutton').html();
	
	snail.battleplan.presetbutton.create = function ($container, presetNumber) {
		// local variables
		const defaultText = 'Preset ' + presetNumber;
		let $button, $save, $clear;
		
		// callbacks
		const onButtonClicked = function () {
			
		};
		
		const onSaveClicked = function () {
			
		};
		
		const onClearClicked = function () {
			
		};
		
		// init code
		$container.html(componentHtml);
		$button = $container.find('.presetbutton-button');
		$save = $container.find('.presetbutton-save');
		$clear = $container.find('.presetbutton-clear');
		
		$button.click(onButtonClicked);
		$save.click(onSaveClicked);
		$clear.click(onClearClicked);
		
		$button.text(defaultText);
		
		return {
		};
	};
	
	return snail;
}(snail || {}, jQuery));