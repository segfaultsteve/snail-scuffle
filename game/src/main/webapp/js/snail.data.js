var snail = (function (snail, $) {
	snail.data = {};
	
	snail.data.promiseSnailInfo = function () {
		return $.get(snail.config.snailsPath);
	};
	
	snail.data.promiseWeaponInfo = function () {
		return $.get(snail.config.weaponsPath);
	};
	
	snail.data.promiseShellInfo = function () {
		return $.get(snail.config.shellsPath);
	};
	
	snail.data.promiseAccessoryInfo = function () {
		return $.get(snail.config.accessoriesPath);
	};
	
	snail.data.promiseItemInfo = function () {
		return $.get(snail.config.itemsPath);
	};
	
	return snail;
}(snail || {}, jQuery));
