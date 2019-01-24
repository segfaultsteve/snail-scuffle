var snail = (function (snail) {
	snail.routing = {};
	
	// private variables
	let pages, currentPage;
	
	// public methods
	snail.routing.init = function (pagesMap) {
		pages = pagesMap;
		for (let key in pages) {
			pages[key].hide();
		}
	};
	
	snail.routing.switchTo = function (page) {
		if (currentPage) {
			currentPage.hide();
		}
		currentPage = pages[page];
		currentPage.show();
	};
	
	return snail;
}(snail || {}));
