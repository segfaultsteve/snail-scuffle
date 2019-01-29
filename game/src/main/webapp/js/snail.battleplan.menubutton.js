var snail = (function(snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.menubutton = {};
	
	const menubuttonHtml = $('#components .components-menubutton').html();
	const itemHtml = $('#components .components-menubutton-item').html();
	
	snail.battleplan.menubutton.create = function ($container, onSelectionChanged) {
		// private variables
		let $button, $list, options, selectedIndex;
		
		// private methods
		const getStatStrings = function (equipInfo) {
			const stats = [];
			if (equipInfo.attackModifier !== 0) {
				stats.push('A: ' + signedString(equipInfo.attackModifier));
			}
			if (equipInfo.defenseModifier !== 0) {
				stats.push('D: ' + signedString(equipInfo.defenseModifier));
			}
			if (equipInfo.speedModifier !== 0) {
				stats.push('S: ' + signedString(equipInfo.speedModifier));
			}
			if (equipInfo.other && equipInfo.other.apCost) {
				stats.push('AP Cost: ' + equipInfo.other.apCost);
			}
			return stats;
		};
		
		const signedString = function (num) {
			if (num > 0) {
				return '+' + num;
			} else if (num < 0) {
				return '−' + (-num);
			} else {
				return '  0';
			}
		};
		
		const createItem = function (equipInfo, includeTooltip) {
			let $item = $(itemHtml);
			$item.filter('.item-name').text(equipInfo.displayName);
			
			if (includeTooltip && equipInfo.description && equipInfo.description !== '') {
				$item.filter('.item-tooltip').text(equipInfo.description);
			} else {
				$item = $item.not('.item-tooltip');
			}
			
			const $stats = $item.filter('.item-stats');
			const statStrings = getStatStrings(equipInfo);
			for (let i = 0; i < statStrings.length; i++) {
				$stats.append('<div class="item-stat">' + statStrings[i] + '</div>');
			}
			
			return $item;
		};
		
		// callbacks
		const onButtonClicked = function (e) {
			const $allLists = $('.menubutton-list');
			const initiallyHidden = $list.is(':hidden');
			
			$allLists.addClass('hidden');
			if (initiallyHidden) {
				$list.removeClass('hidden');
				$('html').one('click', function() {
					$list.addClass('hidden');
				});
				e.stopPropagation();
			}
		};
		
		const onOptionClicked = function (e) {
			const $target = $(e.target);
			if (!$target.hasClass('item-tooltip')) {
				setSelectedOption($(e.target).parents('li').addBack('li').find('.item-name').text());
			}
		};
		
		// public methods
		const setOptionsList = function (optionsList, selectedIndex) {
			options = optionsList;
			for (let i = 0; i < options.length; i++) {
				const $li = $('<li></li>');
				$li.html(createItem(options[i], true));
				$list.append($li);
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
				$button.html(createItem(options[selectedIndex]), false);
				onSelectionChanged(index, options[index]);
			}
		};
		
		const getSelectedOption = function () {
			return options[selectedIndex];
		};
		
		const enable = function () {
			if ($button.hasClass('disabled-button')) {
				$button.removeClass('disabled-button');
				$button.off('click', onButtonClicked).on('click', onButtonClicked);
			}
		};
		
		const disable = function () {
			if (!$button.hasClass('disabled-button')) {
				$button.addClass('disabled-button');
				$button.off('click', onButtonClicked);
			}
		};
		
		// init code
		$container.addClass('menubutton');
		$container.html(menubuttonHtml);
		$button = $container.find('.menubutton-button');
		$list = $container.find('.menubutton-list');
		
		$button.click(onButtonClicked);
		$list.click(onOptionClicked);
		
		return {
			setOptionsList: setOptionsList,
			setSelectedOption: setSelectedOption,
			setSelectedIndex: setSelectedIndex,
			getSelectedOption: getSelectedOption,
			enable: enable,
			disable: disable
		};
	};
	
	return snail;
}(snail || {}, jQuery));
