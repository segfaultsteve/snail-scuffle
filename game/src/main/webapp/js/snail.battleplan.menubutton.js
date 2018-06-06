var snail = (function(snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.menubutton = {};
	
	const componentHtml = $('#components .components-menubutton').html();
	
	snail.battleplan.menubutton.create = function ($container) {
		// private variables
		let selectionChangedHandlers = [];
		let options, selected;
		
		// private methods
		const refreshOptionsList = function () {
			const $list = $container.find('.menubutton-list');
			$list.empty();
			for (let i = 0; i < options.length; i++) {
				const li = '<li>' + options[i] + '</li>';
				$list.append(li);
			}
		};
		
		const updateButtonText = function () {
			$container.find('.menubutton-button').text(options[selected]);
		};
		
		// callbacks
		const onButtonClicked = function (e) {
			const $list = $container.find('.menubutton-list');
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
			const index = options.indexOf($(e.target).text());
			setSelectedOption(index);
		};
		
		// public methods
		const setOptionsList = function (optionsList, selectedIndex) {
			options = [];
			selected = 0;
			for (let i = 0; i < optionsList.length; i++) {
				options[i] = optionsList[i];
			}
			refreshOptionsList();
			setSelectedOption(selectedIndex);
		};
		
		const setSelectedOption = function (index) {
			if (index === 'last') {
				index = options.length - 1;
			}
			selected = index;
			updateButtonText();
			selectionChangedHandlers.forEach(handler => handler(index, options[index]));
		};
		
		const getSelectedOption = function () {
			return options[selected];
		};
		
		const addSelectionChangedHandler = function(handler) {
			selectionChangedHandlers.push(handler);
		};
		
		// init code
		$container.addClass('menubutton');
		$container.html(componentHtml);
		$container.find('.menubutton-button').click(onButtonClicked);
		$container.find('.menubutton-list').click(onOptionClicked);
		
		return {
			setOptionsList: setOptionsList,
			setSelectedOption: setSelectedOption,
			getSelectedOption: getSelectedOption,
			addSelectionChangedHandler: addSelectionChangedHandler
		};
	};
	
	return snail;
}(snail || {}, jQuery));
