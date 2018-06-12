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
			const displayName = snail.battleplan.model.loadBattlePlan(presetNumber);
			$button.text(displayName || emptyText);
		};
		
		const onSetClicked = function () {
			let name = $button.text();
			name = (name === emptyText) ? defaultText : name;
			$namebox.show();
			$nametext.val(name);
			$nametext.focus();
		};
		
		const onClearClicked = function () {
			snail.battleplan.model.deleteBattlePlan(presetNumber);
			$button.text(emptyText);
		};
		
		const onSaveClicked = function () {
			let name = $nametext.val();
			name = (name.length > 0) ? name : defaultText;
			snail.battleplan.model.saveBattlePlan(presetNumber, name);
			$button.text(name);
			$namebox.hide();
		};
		
		const onCancelClicked = function () {
			$namebox.hide();
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
		
		const displayName = snail.battleplan.model.getPresetDisplayName(presetNumber);
		$button.text(displayName || emptyText);
		$namebox.hide();
		
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
