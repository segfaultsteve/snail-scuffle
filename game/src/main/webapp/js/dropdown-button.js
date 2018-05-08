(function($){
	$.fn.dropdownButton = function(){
		const $allLists = $('.equip-list');
		return this.each(function(){
			const $button = $(this).find('.equip-button');
			const $list = $(this).find('.equip-list');

			$button.click(function(e) {
				const alreadyHidden = $list.is(':hidden');
				$allLists.hide();
				if (alreadyHidden) {
					$list.show();
					$('html').one('click', function(e) {
						$list.hide();
					});
					e.stopPropagation();
				}
			});

			$list.find('li').click(function(e) {
				$button.text($(this).html());
			});
		});
	};
})(jQuery);