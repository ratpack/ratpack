// -------------------------------------
// Toggle (show/hide) import statements
// -------------------------------------
(function($) {
  // JQuery plugin - simple unique id generator
  var id_count = 0;
  $.rpkUniqueId = function(base) {
    return (base ? base : "ui-") + (++id_count);
  };
})(jQuery);

(function($) {
  // jQuery to show and hide imports in java/groovy code samples
  $.rpkToggleImports = function(options) {
    // default options
    var settings = $.extend({
      lang:   "java",   // could be "java" | "groovy"
      action: "hide",   // could be "hide" | "show"
      actionEl: null
    }, options);
    var langs = ["java", "groovy"],
        actions = ["hide", "show"];

    if ($.inArray(settings.lang, langs) == -1 ||
        $.inArray(settings.action, actions) == -1) {
      return;
    }

    // delete fake import ... DOM element
    var $startEl;
    if (!settings.actionEl) {
      $startEl = $("code.language-" + settings.lang);
    }
    else if (settings.action === "show") {
      // find parent with given class
      $startEl = $(settings.actionEl).parent("code.language-" + settings.lang);
      $(settings.actionEl).remove();
      settings.actionEl = null;
    }
    else if (settings.action === "hide") {
      $startEl = $(settings.actionEl).parent("code.language-" + settings.lang);
    }

    $startEl.each(function(idx) {
      var firstEntry = true;
      $(this)
        .children("span.token.keyword")
          .filter(function() { return $(this).text() === "import"; })
            .each(function(idxEl, el) {
              var $el = $(el);
              if (settings.action === "hide" && firstEntry) {
                var uid = $.rpkUniqueId("showImports-");
                if ($el.hasClass("gutter-open")) {
                  $el.removeClass("gutter-open");
                }
                $el.before("<span class=\"token keyword gutter-folded\" id=\"" + uid + "\">import ...\n\n</span>");
                $("#" + uid).css("cursor", "pointer").click(function() {
                  $.rpkToggleImports({lang: settings.lang, action: "show", actionEl: "#"+uid});
                });
                firstEntry = false;
              }
              else if (settings.action === "show" && firstEntry) {
                if (!$el.hasClass("gutter-open")) {
                  $el.addClass("gutter-open");
                }
                firstEntry = false;
              }
              var els = [el]; // array of DOM elements to hide/show
              var nSiblings = $el.siblings().length;  // get num of siblings
              var nEl = el;
              // limit inifinite loop to number of siblings
              while (nSiblings >= 0) {
                var nElS = nEl.nextElementSibling;
                // if next sibling is text, not DOM node
                if (settings.action === "hide" && nEl.nextSibling && nEl.nextSibling.nodeType === 3) {
                  var txt = nEl.nextSibling.textContent;
                  if (txt) {
                    nEl.nextSibling.textContent = "";
                    $(nEl).after("<span>" + txt + "</span>");
                    els[els.length] = nEl.nextElementSibling;
                  }
                }
                if (!nElS) {
                  break;
                }
                els[els.length] = nElS;
                var ns = nElS.nextSibling, 
                    nsIsText = ns && ns.nodeType === 3 && ns.textContent;

                if (settings.lang === "java" && $(nElS).text() === ";" ||
                    settings.lang === "groovy" && $(nElS).text().indexOf("\n") >= 0 ||
                    settings.lang === "groovy" && nsIsText) {
                  if (settings.action === "hide" && nsIsText && ns.textContent.indexOf("\n") >= 0) {
                    var txt2 = ns.textContent, crIdx = txt2.indexOf("\n");
                    for (++crIdx; crIdx < txt2.length; crIdx++) {
                      if (txt2[crIdx] !== "\n") {
                        break;
                      }
                    }
                    var txt2_1 = txt2.substring(0, crIdx);
                    var txt2_2 = txt2.substring(crIdx);
                    if (txt2_2) {
                      ns.textContent = txt2_2;
                    }
                    else {
                      ns.textContent = "";
                    }
                    $(nElS).after("<span>" + txt2_1 + "</span>");
                    els[els.length] = nElS.nextElementSibling;
                    break;
                  }
                  if (settings.action === "show" && nElS.nextElementSibling) {
                    var txt3 = $(nElS.nextElementSibling).text();
                    if (txt3 && txt3.indexOf("\n") >=0) {
                      els[els.length] = nElS.nextElementSibling;
                    }
                    break;
                  }
                }
                if ($(nElS).text().indexOf("\n") >= 0) {
                  break;
                }
                nEl = nElS;
                nSiblings--;
              }
              // hide/show elements
              $.each(els, function(elsIdx, v) {
                if (settings.action === "hide") {
                  $(v).hide();
                }
                else if (settings.action === "show") {
                  $(v)
                    .show()
                      .css("cursor", "pointer")
                        .off("click")
                          .click(function() {
                            var uid = this.id || $.rpkUniqueId("showImports-");
                            if (!this.id) {
                              this.id = uid;
                            }
                            $.rpkToggleImports({lang: settings.lang, action: "hide", actionEl: "#" + uid});
                          });
                }
              });
            });
    });
  };
})(jQuery);


// -------------------------------------
// Handle javadoc samples. Highlight them with Prism.
// Build DOM that is compatible with 'toggle imports' functionality.
// -------------------------------------
(function($) {
  // jQuery for Javadoc API. Code samples are not included in <pre><code></code></pre> structure
  $.rpkHighlightJavadocCode = function() {
    $("pre").each(function(idx) {
      var el = this, $el = $(this);
      var elClass = $el.attr("class");
      if (!elClass) {
        return;
      }
      if (elClass.indexOf("tested") != -1 ||
          elClass.indexOf("groovy-chain-dsl") != -1 ||
          elClass.indexOf("groovy-ratpack-dsl") != -1 ||
          elClass.indexOf("java-chain-dsl") != -1 ||
          elClass.indexOf("java") != -1 ||
          elClass.indexOf("exec") != -1 ||
          elClass.indexOf("not-tested") != -1) {
        elClass = elClass + " language-groovy";
        $el.attr("class", elClass);
        var elInnerHtml = $el.html();
        $el.html("<code class=\"language-groovy\">" + elInnerHtml.replace(/^ /gm, '').replace(/\s+$/g, '') + "</code>");
        $el.children("code.language-groovy").each(function() {
          Prism.highlightElement(this, false, null);
        });
      }
    });
  };
})(jQuery);

$(function() {
    $.rpkHighlightJavadocCode();
    $.rpkToggleImports({lang: "java", action: "hide"});
    $.rpkToggleImports({lang: "groovy", action: "hide"});
});

