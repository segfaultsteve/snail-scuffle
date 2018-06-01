var snail = (function (snail, $) {
	snail.battleplan = snail.battleplan || {};
	snail.battleplan.instructions = {};
	
	const instructionBoxHtml = $('#components .components-instructionbox').html();
	const instructionHtml = $('#components .components-instruction').html();
	
	snail.battleplan.instructions.create = function ($container) {
		// private variables
		const states = {
			collapsed: 'collapsed',
			expanding: 'expanding',
			expanded: 'expanded',
			collapsing: 'collapsing'
		};
		
		let $instructionbox, $expandicon, $collapseicon;
		
		// private methods
		const beginExpand = function () {
			$instructionbox.removeClass(states.collapsed);
			$instructionbox.addClass(states.expanding);
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
			$expandicon.show();
			$instructionbox.one('click', beginExpand);
		};
		
		// public methods
		const get = function () {
			
		};
		
		// init code
		$container.html(instructionBoxHtml);
		$instructionbox = $container.find('.instructionbox');
		$expandicon = $container.find('.instructionbox-expandicon');
		$collapseicon = $container.find('.instructionbox-collapseicon');
		endCollapse();
		
		return {
			get: get
		};
	}
	
	return snail;
}(snail || {}, jQuery));