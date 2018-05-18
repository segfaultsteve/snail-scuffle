/* global snail */

(function($) {
	const create = function ($container, onSelectionChanged) {
		// private variables
		const controlHtml = ''
			+ '<button type="button" class="menubutton-button"></button>'
			+ '<ul class="menubutton-list"></ul>';
		let options = ['None'];
		let selected = 0;
		
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
		}
		
		// event handlers
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
		}
		
		const onOptionClicked = function (e) {
			setSelectedOption($(e.target).text());
		}
		
		// public methods
		const setOptionsList = function (optionsList, selectedOption) {
			options = [];
			selected = 0;
			for (let i = 0; i < optionsList.length; i++) {
				options[i] = optionsList[i];
			}
			refreshOptionsList();
			setSelectedOption(selectedOption);
		};
		
		const setSelectedOption = function (selectedOption) {
			const index = options.indexOf(selectedOption);
			if (index >= 0) {
				selected = index;
				updateButtonText();
				if (onSelectionChanged) {
					onSelectionChanged(selectedOption, index);
				}
			}
		};
		
		const getSelectedOption = function () {
			return options[selected];
		};
		
		// init code
		$container.addClass('menubutton');
		$container.html(controlHtml);
		$container.find('.menubutton-button').click(onButtonClicked);
		$container.find('.menubutton-list').click(onOptionClicked);
		
		return {
			setOptionsList: setOptionsList,
			setSelectedOption: setSelectedOption,
			getSelectedOption: getSelectedOption
		};
	};
	
	snail.battleplan.menubutton = {
		create: create
	};
}(jQuery));