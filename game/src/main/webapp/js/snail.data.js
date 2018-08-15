var snail = (function (snail, $) {
	snail.data = {};
	
	snail.data.promiseSnailInfo = function () {
		return $.getJSON(snail.config.snailsPath).promise();
	};
	
	snail.data.promiseWeaponInfo = function () {
		return $.getJSON(snail.config.weaponsPath).promise();
	};
	
	snail.data.promiseShellInfo = function () {
		return $.getJSON(snail.config.shellsPath).promise();
	};
	
	snail.data.promiseAccessoryInfo = function () {
		return $.getJSON(snail.config.accessoriesPath).promise();
	};
	
	snail.data.promiseItemInfo = function () {
		return $.getJSON(snail.config.itemsPath).promise();
	};
	
	snail.data.promiseServerInfo = function () {
		return $.getJSON(snail.config.serversPath).promise();
	};
	
	snail.data.saveLocal = function (key, value) {
		localStorage.setItem(key, JSON.stringify(value));
	};
	
	snail.data.loadLocal = function (key) {
		return JSON.parse(localStorage.getItem(key));
	};
	
	snail.data.deleteLocal = function (key) {
		localStorage.removeItem(key);
	};
	
	return snail;
}(snail || {}, jQuery));
