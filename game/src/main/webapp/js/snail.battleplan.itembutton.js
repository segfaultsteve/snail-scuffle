var snail = (function (snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.itembutton = {};
	
	const componentHtml = $('#components .components-itembutton').html();
	
	snail.battleplan.itembutton.create = function ($container) {
		// private variables
		const states = {
			noCondition: 'noCondition',
			addCondition: 'addCondition',
			hasCondition: 'hasCondition',
			usesCondition: 'usesCondition'
		};
		let selectionChangedHandlers = [];
		let conditionChangedHandlers = [];
		let menubutton, options, state, selectedItem;
		
		// private methods
		const setState = function (newState) {
			state = newState;
			const $condition = $container.find('.itembutton-condition');
			if (!(newState in states) || newState === states.noCondition) {
				$condition.hide();
			} else {
				$condition.show();
				$condition.children().hide();

				if (newState === states.addCondition) {
					$condition.find('.itembutton-condition-addicon').show();
					$condition.find('.itembutton-condition-addtext').show();
					resetConditionElements();
				} else {
					$condition.find('.itembutton-condition-removeicon').show();
					$condition.find('.itembutton-condition-usetext').show();
					$condition.find('.itembutton-condition-type').show();

					if (newState === states.hasCondition) {
						$condition.find('.itembutton-condition-hascondition').show();
					} else {
						$condition.find('.itembutton-condition-usescondition').show();
					}
				}
			}
			
			notfiyConditionChangeHandlers();
		};
		
		const resetConditionElements = function () {
			$container.find('.itembutton-condition-type').val('ihave');
			$container.find('.itembutton-condition-hascondition-stat').val('ap');
			$container.find('.itembutton-condition-hascondition-inequality').val('geq');
			$container.find('.itembutton-condition-hascondition-threshold').val('');
			$container.find('.itembutton-condition-usescondition-item').val('attack');
		};
		
		const notfiyConditionChangeHandlers = function () {
			let condition = snail.battleplan.model.createEmptyItemCondition();
			if (state === states.hasCondition) {
				const player = ($container.find('.itembutton-condition-type').val() === 'ihave') ? 'me' : 'enemy';
				const stat = $container.find('.itembutton-condition-hascondition-stat').val();
				const inequality = $container.find('.itembutton-condition-hascondition-inequality').val();
				const threshold = $container.find('.itembutton-condition-hascondition-threshold').val();
				condition = snail.battleplan.model.createHasCondition(player, stat, inequality, threshold);
			} else if (state === states.usesCondition) {
				const item = $container.find('.itembutton-condition-usescondition-item').val();
				condition = snail.battleplan.model.createEnemyUsesCondition(item);
			}
			conditionChangedHandlers.forEach(handler => handler(condition));
		};
		
		// callbacks
		const onMenubuttonSelectionChanged = function (index, selection) {
			const lastSelection = selectedItem;
			selectedItem = selection;
			
			if (index === options.length - 1) {
				selectionChangedHandlers.forEach(handler => handler(index, selection));
				setState(states.noCondition);
			} else if (selection === lastSelection) {
				return;
			} else {
				selectionChangedHandlers.forEach(handler => handler(index, selection));
				setState(states.addCondition);
			}
		};
		
		const onAddConditionClicked = function() {
			setState(states.hasCondition);
		};
		
		const onRemoveConditionClicked = function() {
			setState(states.addCondition);
		};
		
		const onTypeSelectionChange = function () {
			if (this.value === 'enemyuses') {
				setState(states.usesCondition);
			} else {
				setState(states.hasCondition);
			}
		};
		
		// public methods
		const setOptionsList = function (optionsList, selectedIndex) {
			options = optionsList;
			const numericIndex = (selectedIndex === 'last') ? options.length - 1 : selectedIndex;
			selectedItem = options[numericIndex];
			menubutton.setOptionsList(optionsList, selectedIndex);
		};
		
		const setSelectedOption = function (selectedOption) {
			menubutton.setSelectedOption(selectedOption);
		};
		
		const getSelectedOption = function () {
			return menubutton.getSelectedOption();
		};
		
		const addSelectionChangedHandler = function (handler) {
			selectionChangedHandlers.push(handler);
		};
		
		const addConditionChangedHandler = function (handler) {
			conditionChangedHandlers.push(handler);
		};
		
		// init code
		$container.addClass('itembutton');
		$container.html(componentHtml);
		menubutton = snail.battleplan.menubutton.create($container.find('.itembutton-button'))
		menubutton.addSelectionChangedHandler(onMenubuttonSelectionChanged);
		$container.find('.itembutton-condition-addicon, .itembutton-condition-addtext').click(onAddConditionClicked);
		$container.find('.itembutton-condition-removeicon').click(onRemoveConditionClicked);
		$container.find('.itembutton-condition-type').change(onTypeSelectionChange);
		$container.find('.itembutton-condition-hascondition-stat').change(notfiyConditionChangeHandlers);
		$container.find('.itembutton-condition-hascondition-inequality').change(notfiyConditionChangeHandlers);
		$container.find('.itembutton-condition-hascondition-threshold').change(notfiyConditionChangeHandlers);
		$container.find('.itembutton-condition-usescondition-item').change(notfiyConditionChangeHandlers);
		setState(states.noCondition);
		
		return {
			setOptionsList: setOptionsList,
			setSelectedOption: setSelectedOption,
			getSelectedOption: getSelectedOption,
			addSelectionChangedHandler: addSelectionChangedHandler,
			addConditionChangedHandler: addConditionChangedHandler
		};
	};
	
	return snail;
}(snail || {}, jQuery));
