var snail = (function (snail, $) {
	snail.data = {};
	
	snail.data.promiseSnailInfo = function () {
		return $.get(snail.config.snailsPath).promise();
	};
	
	snail.data.promiseWeaponInfo = function () {
		return $.get(snail.config.weaponsPath).promise();
	};
	
	snail.data.promiseShellInfo = function () {
		return $.get(snail.config.shellsPath).promise();
	};
	
	snail.data.promiseAccessoryInfo = function () {
		return $.get(snail.config.accessoriesPath).promise();
	};
	
	snail.data.promiseItemInfo = function () {
		return $.get(snail.config.itemsPath).promise();
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
