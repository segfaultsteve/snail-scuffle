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
		
		let menubutton;
		let selectedItem;
		
		// private methods
		const setState = function (newState) {
			const $condition = $container.find('.itembutton-condition');
			if (!(newState in states) || newState === states.noCondition) {
				$condition.hide();
				return;
			}
			
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
		};
		
		const resetConditionElements = function () {
			$container.find('.itembutton-condition-type').val('ihave');
			$container.find('.itembutton-condition-hascondition-stat').val('ap');
			$container.find('.itembutton-condition-hascondition-inequality').val('gte');
			$container.find('.itembutton-condition-hascondition-threshold').val('');
			$container.find('.itembutton-condition-usescondition-item').val('attack');
		}
		
		// event handlers
		const onMenubuttonSelectionChanged = function (selection) {
			const lastSelection = selectedItem;
			selectedItem = selection;
			
			if (selection === 'None') {
				setState(states.noCondition);
			} else if (selection === lastSelection) {
				return;
			} else {
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
		}
		
		// public methods
		const setOptionsList = function (optionsList, selectedOption) {
			selectedItem = selectedOption;
			menubutton.setOptionsList(optionsList, selectedOption);
		};
		
		const setSelectedOption = function (selectedOption) {
			menubutton.setSelectedOption(selectedOption);
		}
		
		const getSelectedOption = function () {
			return menubutton.getSelectedOption();
		}
		
		// init code
		$container.addClass('itembutton');
		$container.html(componentHtml);
		menubutton = snail.battleplan.menubutton.create($container.find('.itembutton-button'), onMenubuttonSelectionChanged);
		$container.find('.itembutton-condition-addicon, .itembutton-condition-addtext').click(onAddConditionClicked);
		$container.find('.itembutton-condition-removeicon').click(onRemoveConditionClicked);
		$container.find('.itembutton-condition-type').change(onTypeSelectionChange);
		
		return {
			setOptionsList: setOptionsList,
			setSelectedOption: setSelectedOption,
			getSelectedOption: getSelectedOption
		};
	};
	
	return snail;
}(snail || {}, jQuery));
