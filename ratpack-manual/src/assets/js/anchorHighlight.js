function jQuerySelectorEscape(expression) {
    return expression.replace(/[!"#$%&'()*+,.\/:;<=>?@\[\\\]^`{|}~ ]/g, '\\$&');
}

function anchor_highlight(anchor) {
    var anchorEscaped = jQuerySelectorEscape(anchor);

    var elem;
    var byId = $('#' + anchorEscaped);
    if (byId.length == 0) {
        var byName = $("a[name=" + anchorEscaped + "]");
        if (byName.length == 0) {
            return;
        } else {
            elem = byName.next(":header");
            if (elem.length == 0) {
                elem = byName.next().find(":header").first();
                if (elem.length == 0) {
                    return;
                }
            }
        }
    } else {
        elem = byId.parent();
    }

    var off = {backgroundColor: elem.css("backgroundColor")};
    var on = {backgroundColor: "#C6DFF4"};

    var speed = 300;

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

}

$(function() {
    $('a[href*=#]').click(function(){
        anchor_highlight($(this).attr('href').split('#')[1]);
    });

    if (document.location.hash) {
        anchor_highlight(document.location.hash.split('#')[1]);
    }
});

