var snail = (function(snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.menubutton = {};
	
	const componentHtml = $('#components .components-menubutton').html();
	
	snail.battleplan.menubutton.create = function ($container, onSelectionChanged) {
		// private variables
		let $button, $list, options, selectedIndex;
		
		// callbacks
		const onButtonClicked = function (e) {
			const $allLists = $('.menubutton-list');
			const initiallyHidden = $list.is(':hidden');
			
			$allLists.hide();
			if (initiallyHidden) {
				$list.show();
				$('html').one('click', function() {
					$list.hide();
				});
				e.stopPropagation();
			}
		};
		
		const onOptionClicked = function (e) {
			setSelectedOption($(e.target).text());
		};
		
		// public methods
		const setOptionsList = function (optionsList, selectedIndex) {
			options = optionsList;
			for (let i = 0; i < options.length; i++) {
				const li = '<li>' + options[i].displayName + '</li>';
				$list.append(li);
			}
			setSelectedIndex(selectedIndex);
		};
		
		const setSelectedOption = function (displayName) {
			const displayNames = options.map(option => option.displayName);
			const index = displayNames.indexOf(displayName);
			if (index > -1) {
				setSelectedIndex(index);
			}
		};
		
		const setSelectedIndex = function (index) {
			if (index === 'last') {
				index = options.length - 1;
			}
			if (index !== selectedIndex) {
				selectedIndex = index;
				$button.text(options[selectedIndex].displayName);
				onSelectionChanged(index, options[index]);
			}
		};
		
		const getSelectedOption = function () {
			return options[selectedIndex];
		};
		
		// init code
		$container.addClass('menubutton');
		$container.html(componentHtml);
		$button = $container.find('.menubutton-button');
		$list = $container.find('.menubutton-list');
		
		$button.click(onButtonClicked);
		$list.click(onOptionClicked);
		
		return {
			setOptionsList: setOptionsList,
			setSelectedOption: setSelectedOption,
			setSelectedIndex: setSelectedIndex,
			getSelectedOption: getSelectedOption
		};
	};
	
	return snail;
}(snail || {}, jQuery));
