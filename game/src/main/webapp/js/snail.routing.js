var snail = (function (snail) {
	snail.routing = {};
	
	// private variables
	let pages, currentPage;
	
	// public methods
	snail.routing.init = function (pagesMap) {
		pages = pagesMap;
		for (let key in pages) {
			pages[key].addClass('hidden');
		}
	};
	
	snail.routing.switchTo = function (page) {
		if (currentPage) {
			currentPage.addClass('hidden');
		}
		currentPage = pages[page];
		currentPage.removeClass('hidden');
	};
	
	return snail;
}(snail || {}));
