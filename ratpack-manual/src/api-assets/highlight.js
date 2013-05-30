var pres = document.getElementsByTagName("pre");
for (var i = 0; i < pres.length; i++) {
  var pre = pres[i];
  pre.className = pre.className + " language-groovy";
  pre.innerHTML = "<code>" + pre.innerHTML + "</code>";
  Prism.highlightElement(pre, false, null);
}

