var snail = (function(snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.menubutton = {};
	
	const componentHtml = $('#components .components-menubutton').html();
	
	snail.battleplan.menubutton.create = function ($container, onSelectionChanged) {
		// private variables
		let options, selectedIndex;
		
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
			$container.find('.menubutton-button').text(options[selectedIndex]);
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
			setSelectedOption($(e.target).text());
		};
		
		// public methods
		const setOptionsList = function (optionsList, selectedIndex) {
			options = [];
			for (let i = 0; i < optionsList.length; i++) {
				options[i] = optionsList[i];
			}
			refreshOptionsList();
			setSelectedIndex(selectedIndex);
		};
		
		const setSelectedOption = function (option) {
			const index = options.indexOf(option);
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
				updateButtonText();
				onSelectionChanged(index, options[index]);
			}
		};
		
		const getSelectedOption = function () {
			return options[selectedIndex];
		};
		
		// init code
		$container.addClass('menubutton');
		$container.html(componentHtml);
		$container.find('.menubutton-button').click(onButtonClicked);
		$container.find('.menubutton-list').click(onOptionClicked);
		
		return {
			setOptionsList: setOptionsList,
			setSelectedOption: setSelectedOption,
			setSelectedIndex: setSelectedIndex,
			getSelectedOption: getSelectedOption
		};
	};
	
	return snail;
}(snail || {}, jQuery));
