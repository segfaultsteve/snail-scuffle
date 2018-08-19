var snail = (function (snail, $) {
	snail.io = {};
	
	// private variables
	let servers;
	
	// private methods
	const ajaxWithRetry = function (func, args) {
		return func(args)
			.then(failOnErrorStatus, retryFailedRequest(func, args))
			.promise();
	};
	
	const failOnErrorStatus = function (data, textStatus, jqxhr) {
		if (jqxhr.status == 200) {
			return data;
		} else {
			return $.Deferred().reject(jqxhr);
		}
	};
	
	const retryFailedRequest = function (func, args) {
		return function () {
			const retry = $.Deferred();
			setTimeout(function () {
				func(args).then(retry.resolve, retry.reject);
			}, 1000)
			return retry;
		}
	};
	
	// public methods
	snail.io.init = function () {
		servers = ajaxWithRetry($.getJSON, snail.config.serversPath);
	};
	
	snail.io.promiseSnailInfo = function () {
		return ajaxWithRetry($.getJSON, snail.config.snailsPath);
	};
	
	snail.io.promiseWeaponInfo = function () {
		return ajaxWithRetry($.getJSON, snail.config.weaponsPath);
	};
	
	snail.io.promiseShellInfo = function () {
		return ajaxWithRetry($.getJSON, snail.config.shellsPath);
	};
	
	snail.io.promiseAccessoryInfo = function () {
		return ajaxWithRetry($.getJSON, snail.config.accessoriesPath);
	};
	
	snail.io.promiseItemInfo = function () {
		return ajaxWithRetry($.getJSON, snail.config.itemsPath);
	};
	
	snail.io.putSkirmish = function () {
		return servers
			.then(function (servers) {
				return $.ajax({
					url: servers.matchmaker + '/skirmishes',
					type: 'PUT',
					dataType: 'json',
					xhrFields: { withCredentials: true }
				});
			})
			.promise();
	};
	
	snail.io.getSkirmish = function (id) {
		return servers
			.then(function (servers) {
				return $.ajax({
					url: servers.matchmaker + '/skirmishes/' + id,
					type: 'GET',
					dataType: 'json',
					xhrFields: { withCredentials: true }
				});
			}).promise();
	};
	
	snail.io.saveLocal = function (key, value) {
		localStorage.setItem(key, JSON.stringify(value));
	};
	
	snail.io.loadLocal = function (key) {
		return JSON.parse(localStorage.getItem(key));
	};
	
	snail.io.deleteLocal = function (key) {
		localStorage.removeItem(key);
	};
	
	return snail;
}(snail || {}, jQuery));
