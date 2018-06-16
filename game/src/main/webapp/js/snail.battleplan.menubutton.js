var snail = (function(snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.menubutton = {};
	
	const componentHtml = $('#components .components-menubutton').html();
	
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
		
		const htmlToDisplay = function (equipInfo) {
			const stats = getStatStrings(equipInfo);
			let html = '<span class="equip-name">' + equipInfo.displayName + '</span><div class="equip-stats">';
			for (let i = 0; i < stats.length; i++) {
				html += '<div class="equip-stat">' + stats[i] + '</div>';
			}
			html += '</div>';
			return html;
		};
		
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
			setSelectedOption($(e.target).parents('li').addBack('li').find('.equip-name').text());
		};
		
		// public methods
		const setOptionsList = function (optionsList, selectedIndex) {
			options = optionsList;
			for (let i = 0; i < options.length; i++) {
				const li = '<li>' + htmlToDisplay(options[i]) + '</li>';
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
				$button.html(htmlToDisplay(options[selectedIndex]));
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
