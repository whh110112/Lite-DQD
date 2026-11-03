package com.whh11.dqdlite.util

/**
 * 懂球帝 WebView 页面净化脚本。
 * 针对懂球帝移动版 Vue 单页应用注入 CSS 和 MutationObserver，
 * 隐藏顶部"打开APP"、底部"下载懂球帝"横幅、推广与弹窗浮层。
 */
object Injector {

    private const val CSS = """
[class*="advert"],[id*="advert"],
[class*="ad-banner"],[class*="-ad-"],[class^="ad-"],[class$="-ad"],
[class*="adsbygoogle"],iframe[id^="google_ads"],
[class*="download-app"],[class*="downloadApp"],
[class*="open-app"],[class*="openApp"],
[class*="app-guide"],[class*="appGuide"],
[class*="client-guide"],
.bdshare,[class*="share-bar"],[class*="shareBar"],
[class*="popup-app"],[class*="guideDownload"],
.download-fixed,.open-fixed,[class*="footer-tip"],
[class*="download_btn"],[class*="app_link"]{display:none !important;}
html,body{overscroll-behavior-y:contain;-webkit-tap-highlight-color:transparent;}
"""

    private const val SCRIPT = """
(function(){
  if (window.__DQD_CLEANED__) { if (window.__DQD_SWEEP__) window.__DQD_SWEEP__(); return; }
  window.__DQD_CLEANED__ = true;

  var CSS = __CSS__;
  var BAD = ['下载APP','下载App','下载app','打开APP','打开App','立即下载','打开客户端',
             '前往APP','用APP打开','去APP','下载懂球帝','懂球帝APP','安装APP','打开应用'];

  function addStyle(){
    if (document.getElementById('__dqd_style')) return;
    var s = document.createElement('style');
    s.id = '__dqd_style';
    s.textContent = CSS;
    (document.head || document.documentElement).appendChild(s);
  }

  function isFloating(el){
    var st = window.getComputedStyle(el);
    return st.position === 'fixed' || st.position === 'sticky';
  }

  function looksBad(el){
    if (el.__dqdDone) return;
    var t = (el.innerText || '').trim();
    if (!t || t.length > 24) return;
    var hit = false;
    for (var j = 0; j < BAD.length; j++){ if (t.indexOf(BAD[j]) >= 0){ hit = true; break; } }
    if (!hit) return;
    var r = el.getBoundingClientRect();
    if (isFloating(el) || (r.height > 0 && r.height < 110 && el.children.length <= 3)) {
      el.style.setProperty('display','none','important');
      el.__dqdDone = true;
    }
  }

  function checkTree(root){
    if (!root || root.nodeType !== 1) return;
    looksBad(root);
    var kids = root.querySelectorAll('div,section,aside,a,button,span');
    for (var i = 0; i < kids.length; i++) looksBad(kids[i]);
  }

  function sweep(){
    addStyle();
    var all = document.querySelectorAll('div,section,aside,a,button,span');
    for (var i = 0; i < all.length; i++) looksBad(all[i]);
    var meta = document.querySelector('meta[name="apple-itunes-app"]');
    if (meta && meta.parentNode) meta.parentNode.removeChild(meta);
  }

  window.__DQD_SWEEP__ = sweep;

  var pending = false;
  var mo = new MutationObserver(function(muts){
    for (var i = 0; i < muts.length; i++){
      var added = muts[i].addedNodes;
      for (var k = 0; k < added.length; k++){
        if (added[k].nodeType === 1) checkTree(added[k]);
      }
    }
    if (pending) return;
    pending = true;
    setTimeout(function(){ pending = false; addStyle(); }, 300);
  });

  function boot(){
    sweep();
    try { mo.observe(document.documentElement, {childList:true, subtree:true}); } catch(e){}
    setInterval(function(){
      if (document.visibilityState === 'visible') sweep();
    }, 6000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else { boot(); }
})();
"""

    fun script(): String {
        val literal = CSS.trimIndent()
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "")
            .replace("\n", " ")
        return SCRIPT.trimIndent().replace("__CSS__", "'$literal'")
    }
}
