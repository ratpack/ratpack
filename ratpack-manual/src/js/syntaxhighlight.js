SyntaxHighlighter.defaults['toolbar'] = false;
SyntaxHighlighter.all();

function anchor_highlight(elemId){
	var elem = $(elemId).parent();
	var off = {backgroundColor: "#FFFFFF"};
	var on = {backgroundColor: "#C6DFF4"};;
	var speed = 250;
	
	elem.css(off);
	elem.animate(
		on, 
		speed,
		function() {	
			elem.animate(
				off, 
				speed,
				function() {
					elem.animate(
						on, 
						speed,
						function() {
							elem.animate(off, speed);
						}
					);
				}
			);
		}
	);
	
	setTimeout(function() { $(elemId).animate({ backgroundColor: "#ffffff" }, 3000); },1000);
}

$(function() {
	$("pre code").each(function (i) {
		var code = $(this);
		var pre = code.parent("pre");
		
		pre.text(code.detach().text()).addClass("brush: groovy");
	});
	
	$('a[href*=#]').click(function(){
		var elemId = '#' + $(this).attr('href').split('#')[1];
		anchor_highlight(elemId);
	});
	
	if (document.location.hash) {
		anchor_highlight(document.location.hash);
	}
});