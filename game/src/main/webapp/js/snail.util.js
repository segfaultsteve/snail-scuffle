var snail = (function (snail) {
	snail.util = {};
	
	// public methods
	snail.util.formatHp = function (hp) {
		if (hp > 0.99999 || hp < 0.00001) {
			return snail.util.formatNumber(hp, 1);
		} else {
			return '< 1';
		}
	};
	
	snail.util.formatAp = function (ap) {
		return Math.max(Math.floor(ap), 0);
	};
	
	snail.util.formatNumber = function (num, decimalPlaces) {
		const mult = Math.pow(10, decimalPlaces);
		return Math.max(Math.round(mult*num)/mult, 0);
	};
	
	return snail;
}(snail || {}));
