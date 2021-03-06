var snail = (function(snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.presetbutton = {};
	
	const componentHtml = $('#components .components-presetbutton').html();
	
	snail.battleplan.presetbutton.init = function ($container, presetNumber) {
		// local variables
		const emptyText = '[Empty]';
		const defaultText = 'Preset ' + presetNumber;
		let $button, $set, $clear, $namebox, $nametext, $save, $cancel;
		
		// callbacks
		const onButtonClicked = function () {
			const displayName = snail.model.battleplan.loadBattlePlan(presetNumber);
			$button.text(displayName || emptyText);
		};
		
		const onSetClicked = function () {
			let name = $button.text();
			name = (name === emptyText) ? defaultText : name;
			$namebox.removeClass('hidden');
			$nametext.val(name);
			$nametext.focus();
		};
		
		const onClearClicked = function () {
			snail.model.battleplan.deleteBattlePlan(presetNumber);
			$button.text(emptyText);
		};
		
		const onSaveClicked = function () {
			let name = $nametext.val();
			name = (name.length > 0) ? name : defaultText;
			snail.model.battleplan.saveBattlePlan(presetNumber, name);
			$button.text(name);
			$namebox.addClass('hidden');
		};
		
		const onCancelClicked = function () {
			$namebox.addClass('hidden');
		};
		
		// init code
		$container.html(componentHtml);
		$button = $container.find('.presetbutton-button');
		$set = $container.find('.presetbutton-options-set');
		$clear = $container.find('.presetbutton-options-clear');
		$namebox = $container.find('.presetname');
		$nametext = $container.find('.presetname-text');
		$save = $container.find('.presetname-buttons-save');
		$cancel = $container.find('.presetname-buttons-cancel');
		
		$button.click(onButtonClicked);
		$set.click(onSetClicked);
		$clear.click(onClearClicked);
		$save.click(onSaveClicked);
		$cancel.click(onCancelClicked);
		
		const displayName = snail.model.battleplan.getPresetDisplayName(presetNumber);
		$button.text(displayName || emptyText);
		$namebox.addClass('hidden');
		
		$nametext.focus(function () {
			setTimeout(function () { $nametext.select() }, 50);
		});
		
		$nametext.keyup(function (e) {
			if (e.keyCode === 13) {					// Enter
				$save.click();
			} else if (e.keyCode === 27) {	// ESC
				$cancel.click();
			}
		});
	};
	
	return snail;
}(snail || {}, jQuery));
