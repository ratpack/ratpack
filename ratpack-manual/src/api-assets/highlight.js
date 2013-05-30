var pres = document.getElementsByTagName("pre");
for (var i = 0; i < pres.length; i++) {
  var pre = pres[i];
  if (pre.className.indexOf("groovyTestCase") != -1) {
    pre.className = pre.className + " language-groovy";
    pre.innerHTML = "<code>" + pre.innerHTML.replace(/^ /gm, '').replace(/^0.*$\n?/gm, '').replace(/\s+$/g, '') + "</code>";
    Prism.highlightElement(pre, false, null);
  }
}

