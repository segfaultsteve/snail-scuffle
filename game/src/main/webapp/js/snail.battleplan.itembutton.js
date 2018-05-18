/* global snail */

(function () {
	const create = function ($container) {
		// private variables
		const controlHtml = ''
			+ '<span class="itembutton-button"></span>'
			+ '<span class="itembutton-condition">'
			+ 	'<img class="itembutton-condition-addicon" src="assets/plus.png" height="20 width="20>'
			+ 	'<img class="itembutton-condition-removeicon" src="assets/x.png" height="20 width="20>'
			+ 	'<span class="itembutton-condition-addtext"><i>Add Condtion</i></span>'
			+ 	'<span class="itembutton-condition-usetext">Use when</span>'
			+ 	'<select class="itembutton-condition-type">'
			+ 		'<option value="ihave">I have</option>'
			+ 		'<option value="enemyhas">Enemy has</option>'
			+ 		'<option value="enemyuses">Enemy uses</option>'
			+ 	'</select>'
			+ 	'<span class="itembutton-condition-hascondition">'
			+ 		'<select class="itembutton-condition-hascondition-stat">'
			+ 			'<option value="ap">AP</option>'
			+ 			'<option value="hp">HP</option>'
			+ 		'</select>'
			+ 		'<select class="itembutton-condition-hascondition-inequality">'
			+ 			'<option value="gte">&gt;=</option>'
			+ 			'<option value="lte">&lt;=</option>'
			+ 		'</select>'
			+ 		'<input type=text class="itembutton-condition-hascondition-threshold">'
			+ 	'</span>'
			+ 	'<span class="itembutton-condition-usescondition">'
			+ 		'<select class="itembutton-condition-usescondition-item">'
			+ 			'<option value="attack">Attack Boost</option>'
			+ 			'<option value="defense">Defense Boost</option>'
			+ 			'<option value="speed">Speed Boost</option>'
			+ 		'</select>'
			+ 	'</span>'
			+ '</span>';
		
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
		$container.html(controlHtml);
		menubutton = snail.battleplan.menubutton.create($container.find('.itembutton-button'), onMenubuttonSelectionChanged);
		$container.find('.itembutton-condition-addicon, .itembutton-condition-addtext').click(onAddConditionClicked);
		$container.find('.itembutton-condition-removeicon').click(onRemoveConditionClicked);
		$container.find('.itembutton-condition-type').change(onTypeSelectionChange);
		
		setState(states.noCondition);
		
		return {
			setOptionsList: setOptionsList,
			setSelectedOption: setSelectedOption,
			getSelectedOption: getSelectedOption
		}
	};
	
	snail.battleplan.itembutton = {
		create: create
	};
}());