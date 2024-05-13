// Avoid flcker on reload by fooling caching
var theimg = document.getElementById("imgID");
var theurl = src="http://"+document.location.host;
setInterval(function() {
  theimg.src = theurl;
}, 20);
