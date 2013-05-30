var pres = document.getElementsByTagName("pre");
for (var i = 0; i < pres.length; i++) {
  var pre = pres[i];
  if (pre.className.indexOf("groovyTestCase") != -1) {
    pre.className = pre.className + " language-groovy";
    pre.innerHTML = "<code>" + pre.innerHTML.replace(/^\s+|\s+$/g, '') + "</code>";
    Prism.highlightElement(pre, false, null);
  }
}

