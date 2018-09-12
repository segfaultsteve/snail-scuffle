var snail = (function (snail, $) {
	snail.io = {};
	
	// private variables
	let servers;
	
	// private methods
	const ajaxWithRetry = function (func, args) {
		return func(args)
			.then(failOnErrorStatus, retryFailedRequest(func, args));
	};
	
	const failOnErrorStatus = function (data, textStatus, jqxhr) {
		if (jqxhr.status == 200) {
			return data;
		} else {
			return $.Deferred()
				.reject(jqxhr)
				.promise(jqxhr);
		}
	};
	
	const retryFailedRequest = function (func, args) {
		return function () {
			const retry = $.Deferred();
			setTimeout(function () {
				ajaxWithRetry(func, args).then(retry.resolve, retry.reject);
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
					type: 'PUT',
					url: servers.matchmaker + snail.config.skirmishPath,
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
					type: 'GET',
					url: servers.matchmaker + snail.config.skirmishPath + '/' + id,
					dataType: 'json',
					xhrFields: { withCredentials: true }
				});
			}).promise();
	};
	
	snail.io.putBattlePlan = function (id, battlePlan) {
		return servers
			.then(function (servers) {
				return ajaxWithRetry($.ajax, {
					type: 'PUT',
					url: servers.matchmaker + snail.config.skirmishPath + '/' + id,
					data: JSON.stringify(battlePlan),
					contentType: 'application/json; charset=utf-8',
					dataType: 'json',
					xhrFields: { withCredentials: true }
				});
			})
			.promise();
	};
	
	snail.io.postBattle = function (battlePlans) {
		const battleConfig = {
			battlePlans: battlePlans
		};
		
		return ajaxWithRetry($.post, {
			type: 'POST',
			url: snail.config.battlePath,
			data: JSON.stringify(battleConfig),
			contentType: 'application/json; charset=utf-8',
			dataType: 'json'
		});
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
