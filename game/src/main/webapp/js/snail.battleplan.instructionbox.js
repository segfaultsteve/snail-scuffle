var snail = (function (snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.instructionbox = {};
	
	const componentHtml = $('#components .components-instructionbox').html();
	
	snail.battleplan.instructionbox.create = function ($container) {
		// private variables
		const states = {
			collapsed: 'collapsed',
			expanding: 'expanding',
			expanded: 'expanded',
			collapsing: 'collapsing'
		};
		
		let $instructionbox, $expandicon, $collapseicon, $instructions, $addbox, $defaultattack;
		
		// private methods
		const beginExpand = function () {
			$instructionbox.removeClass(states.collapsed);
			$instructionbox.addClass(states.expanding);
			$instructions.show();
			$addbox.show();
			$defaultattack.show();
			$instructionbox.one('animationend', endExpand);
		};
		
		const beginCollapse = function () {
			$instructionbox.removeClass(states.expanded);
			$instructionbox.addClass(states.collapsing);
			$instructionbox.one('animationend', endCollapse);
		};
		
		const endExpand = function () {
			$instructionbox.removeClass(states.expanding);
			$instructionbox.addClass(states.expanded);
			$expandicon.hide();
			$collapseicon.show();
			$collapseicon.one('click', beginCollapse);
		};
		
		const endCollapse = function() {
			$instructionbox.removeClass(states.collapsing);
			$instructionbox.addClass(states.collapsed);
			$collapseicon.hide();
			$instructions.hide();
			$addbox.hide();
			$defaultattack.hide();
			$expandicon.show();
			$instructionbox.one('click', beginExpand);
		};
		
		// callbacks
		const onAddInstruction = function () {
			snail.battleplan.instruction.create($instructions);
		};
		
		// public methods
		const getInstructions = function () {
			
		};
		
		// init code
		$container.html(componentHtml);
		$instructionbox = $container.find('.instructionbox');
		$instructions = $instructionbox.find('.instructionbox-instructions');
		$addbox = $instructionbox.find('.instructionbox-addbox');
		$defaultattack = $instructionbox.find('.instructionbox-defaultattack');
		$expandicon = $container.find('.instructionbox-header-expandicon');
		$collapseicon = $container.find('.instructionbox-header-collapseicon');
		
		$addbox.on('click', '.instructionbox-addbox-addicon, .instructionbox-addbox-text', onAddInstruction);
		
		endCollapse();
		
		return {
			getInstructions: getInstructions
		};
	};
	
	return snail;
}(snail || {}, jQuery));