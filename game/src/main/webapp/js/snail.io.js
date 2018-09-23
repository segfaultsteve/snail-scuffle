var snail = (function (snail, $) {
	snail.io = {};
	
	// private variables
	let servers, snailInfo, weaponInfo, shellInfo, accessoryInfo, itemInfo;
	
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
		snailInfo = ajaxWithRetry($.getJSON, snail.config.snailsPath);
		weaponInfo = ajaxWithRetry($.getJSON, snail.config.weaponsPath);
		shellInfo = ajaxWithRetry($.getJSON, snail.config.shellsPath);
		accessoryInfo = ajaxWithRetry($.getJSON, snail.config.accessoriesPath);
		itemInfo = ajaxWithRetry($.getJSON, snail.config.itemsPath);
	};
	
	snail.io.promiseSnailInfo = function () {
		return snailInfo;
	};
	
	snail.io.promiseWeaponInfo = function () {
		return weaponInfo;
	};
	
	snail.io.promiseShellInfo = function () {
		return shellInfo;
	};
	
	snail.io.promiseAccessoryInfo = function () {
		return accessoryInfo;
	};
	
	snail.io.promiseItemInfo = function () {
		return itemInfo;
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
