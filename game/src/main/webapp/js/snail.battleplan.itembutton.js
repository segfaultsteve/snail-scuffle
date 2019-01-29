var snail = (function (snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.itembutton = {};
	
	const componentHtml = $('#components .components-itembutton').html();
	
	snail.battleplan.itembutton.create = function ($container, onItemChanged, onItemConditionChanged) {
		// private variables
		const states = {
			noCondition: 'noCondition',
			addCondition: 'addCondition',
			hasCondition: 'hasCondition',
			usesCondition: 'usesCondition'
		};
		let menubutton, options, state;
		
		// private methods
		const setState = function (newState) {
			state = newState;
			const $condition = $container.find('.itembutton-condition');
			if (!(newState in states) || newState === states.noCondition) {
				$condition.addClass('hidden');
			} else {
				$condition.removeClass('hidden');
				$condition.children().addClass('hidden');

				if (newState === states.addCondition) {
					$condition.find('.itembutton-condition-addicon').removeClass('hidden');
					$condition.find('.itembutton-condition-addtext').removeClass('hidden');
					resetConditionElements();
				} else {
					$condition.find('.itembutton-condition-removeicon').removeClass('hidden');
					$condition.find('.itembutton-condition-usetext').removeClass('hidden');
					$condition.find('.itembutton-condition-type').removeClass('hidden');

					if (newState === states.hasCondition) {
						$condition.find('.itembutton-condition-hascondition').removeClass('hidden');
					} else {
						$condition.find('.itembutton-condition-usescondition').removeClass('hidden');
					}
				}
			}
		};
		
		const resetConditionElements = function () {
			$container.find('.itembutton-condition-type').val('ihave');
			$container.find('.itembutton-condition-hascondition-stat').val('ap');
			$container.find('.itembutton-condition-hascondition-inequality').val('geq');
			$container.find('.itembutton-condition-hascondition-threshold').val('');
			$container.find('.itembutton-condition-usescondition-item').val('attack');
		};
		
		const notifyItemConditionChanged = function () {
			let condition = null;
			if (state === states.hasCondition) {
				const player = ($container.find('.itembutton-condition-type').val() === 'ihave') ? 'me' : 'enemy';
				const stat = $container.find('.itembutton-condition-hascondition-stat').val();
				const inequality = $container.find('.itembutton-condition-hascondition-inequality').val();
				const threshold = $container.find('.itembutton-condition-hascondition-threshold').val();
				condition = snail.model.battleplan.createHasCondition(player, stat, inequality, threshold);
			} else if (state === states.usesCondition) {
				const item = $container.find('.itembutton-condition-usescondition-item').val();
				condition = snail.model.battleplan.createEnemyUsesCondition(item);
			}
			onItemConditionChanged(condition);
		};
		
		// callbacks
		const onMenubuttonSelectionChanged = function (index, selection) {
			if (index === options.length - 1) {
				onItemChanged(index, selection);
				setState(states.noCondition);
			} else {
				onItemChanged(index, selection);
				setState(states.addCondition);
			}
			
			notifyItemConditionChanged();
		};
		
		const onAddConditionClicked = function() {
			setState(states.hasCondition);
			notifyItemConditionChanged();
		};
		
		const onRemoveConditionClicked = function() {
			setState(states.addCondition);
			notifyItemConditionChanged();
		};
		
		const onTypeSelectionChange = function () {
			if (this.value === 'enemyuses') {
				setState(states.usesCondition);
			} else {
				setState(states.hasCondition);
			}
			notifyItemConditionChanged();
		};
		
		// public methods
		const enable = function () {
			setState(states.noCondition);
			menubutton.enable();
		};
		
		const disable = function () {
			setState(states.noCondition);
			menubutton.disable();
		};
		
		const setOptionsList = function (optionsList, selectedIndex) {
			options = optionsList;
			menubutton.setOptionsList(options, selectedIndex);
		};
		
		const getSelectedOption = function () {
			return menubutton.getSelectedOption();
		};
		
		const setSelectedOption = function (displayName) {
			menubutton.setSelectedOption(displayName);
		};
		
		const setSelectedIndex = function (selectedIndex) {
			menubutton.setSelectedIndex(selectedIndex);
		};
		
		const setCondition = function (condition) {
			if (condition && condition.hasCondition) {
				const hc = condition.hasCondition;
				$container.find('.itembutton-condition-type').val(hc.player === 'me' ? 'ihave' : 'enemyhas');
				$container.find('.itembutton-condition-hascondition-stat').val(hc.stat);
				$container.find('.itembutton-condition-hascondition-inequality').val(hc.inequality);
				$container.find('.itembutton-condition-hascondition-threshold').val(hc.threshold);
				setState(states.hasCondition);
			} else if (condition && condition.enemyUsesCondition) {
				$container.find('.itembutton-condition-type').val('enemyuses');
				$container.find('.itembutton-condition-usescondition-item').val(condition.enemyUsesCondition);
				setState(states.usesCondition);
			} else {
				const displayNames = options.map(option => option.displayName);
				const selectedOption = menubutton.getSelectedOption();
				const selectedIndex = displayNames.indexOf(selectedOption.displayName);
				if (selectedIndex === options.length - 1) {
					setState(states.noCondition);
				} else {
					setState(states.addCondition);
				}
			}
		};
		
		// init code
		$container.addClass('itembutton');
		$container.html(componentHtml);
		
		snail.io.promiseItemInfo().done(function (items) {
			const $usesConditionItems = $container.find('.itembutton-condition-usescondition-item');
			items
				.map(i => '<option value="' + i.name + '">' + i.displayName + '</option>')
				.forEach(i => $usesConditionItems.append(i));
		});
		
		menubutton = snail.battleplan.menubutton.create($container.find('.itembutton-button'), onMenubuttonSelectionChanged)
		$container.find('.itembutton-condition-addicon, .itembutton-condition-addtext').click(onAddConditionClicked);
		$container.find('.itembutton-condition-removeicon').click(onRemoveConditionClicked);
		$container.find('.itembutton-condition-type').change(onTypeSelectionChange);
		$container.find('.itembutton-condition-hascondition-stat').change(notifyItemConditionChanged);
		$container.find('.itembutton-condition-hascondition-inequality').change(notifyItemConditionChanged);
		$container.find('.itembutton-condition-hascondition-threshold').change(notifyItemConditionChanged);
		$container.find('.itembutton-condition-usescondition-item').change(notifyItemConditionChanged);
		setState(states.noCondition);
		
		return {
			enable: enable,
			disable: disable,
			setOptionsList: setOptionsList,
			getSelectedOption: getSelectedOption,
			setSelectedOption: setSelectedOption,
			setSelectedIndex: setSelectedIndex,
			setCondition: setCondition
		};
	};
	
	return snail;
}(snail || {}, jQuery));
