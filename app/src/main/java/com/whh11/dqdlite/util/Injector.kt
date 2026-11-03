package com.whh11.dqdlite.util

/**
 * 懂球帝 WebView 页面净化、折叠展开、夜间/白天主题适配与全量评论注入脚本。
 * 1. 彻底击穿懂球帝头条式（tt-*）与传统式的文章截断折叠（.tt-fold.folded、article.show-more），强制全文展开；
 * 2. 彻底隐藏"打开APP"顶部条、"APP内打开"底部悬浮药丸、底部吸底栏、下载引导与广告遮罩；
 * 3. 采用 MutationObserver 动态守护，防止 Vue/Nuxt 异步水合（Hydration）后重新渲染浮层或折叠文章；
 * 4. 动态从官方公开接口拉取真实精彩评论并高保真渲染；
 * 5. 彻底适配白天与夜间模式高对比度配色。
 */
object Injector {

    private fun getCss(isDark: Boolean): String {
        val themeCss = if (isDark) {
            """
            html, body, #app, .wrapper, .main, article, .con, .detail-container, .article-foot-recommend, .tt-article, .tt-fold {
                background-color: #071009 !important;
                color: #E9F2EB !important;
            }
            .tt-title, h1, h2, h3, p, span, div, a, li, time, .title, .writer, .con p, .con span {
                color: #E9F2EB !important;
            }
            .tt-author-name {
                color: #E9F2EB !important;
            }
            .tt-author-info, .tt-author-info span, .tt-copyright, .writer, .time, time {
                color: #86968B !important;
            }
            .comment-card {
                background-color: #0E1A12 !important;
                border-bottom: 1px solid #1F3325 !important;
            }
            .comment-user {
                color: #45E668 !important;
            }
            .comment-time, .comment-up {
                color: #86968B !important;
            }
            .comment-body {
                color: #E9F2EB !important;
            }
            """
        } else {
            """
            html, body, #app, .wrapper, .main, article, .con, .detail-container, .article-foot-recommend, .tt-article, .tt-fold {
                background-color: #FFFFFF !important;
                color: #1A241C !important;
            }
            .tt-title, h1, h2, h3, p, span, div, a, li, time, .title, .writer, .con p, .con span {
                color: #1A241C !important;
            }
            .tt-author-name {
                color: #1A241C !important;
            }
            .tt-author-info, .tt-author-info span, .tt-copyright, .writer, .time, time {
                color: #617366 !important;
            }
            .comment-card {
                background-color: #FFFFFF !important;
                border-bottom: 1px solid #E0E6E1 !important;
            }
            .comment-user {
                color: #00A818 !important;
            }
            .comment-time, .comment-up {
                color: #8C9B90 !important;
            }
            .comment-body {
                color: #1A241C !important;
            }
            """
        }

        return """
        /* 1. 彻底隐藏头条式与传统版中所有打开APP提示、浮动按钮、顶部吸顶条、吸底条、折叠渐变和弹窗 */
        .tt-top,
        .tt-open,
        .tt-float,
        .tt-bar,
        .tt-fade,
        .tt-more,
        .tt-modal,
        .tt-feature,
        .tt-follow,
        [class*="tt-top"],
        [class*="tt-open"],
        [class*="tt-float"],
        [class*="tt-bar"],
        [class*="tt-fade"],
        [class*="tt-more"],
        [class*="tt-modal"],
        [class*="tt-feature"],
        .show-more-btn,
        .show-more-btn-text,
        .mask,
        .mask-content,
        .article-download-btn-container,
        [class*="download"],
        [class*="open-app"],
        [class*="openApp"],
        [class*="app-guide"],
        [class*="appGuide"],
        .btn-app,
        .header,
        .top-header,
        .share-container,
        .pop-download,
        .recommend-container,
        .article-foot-recommend > div:first-child,
        .comm-container > div > a.btn {
            display: none !important;
            height: 0 !important;
            min-height: 0 !important;
            visibility: hidden !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }

        /* 2. 彻底击穿文章折叠，强制展开全文 */
        .tt-fold,
        .tt-fold.folded,
        div[class*="tt-fold"],
        article,
        article.show-more,
        .article-content,
        #articlecon {
            max-height: none !important;
            height: auto !important;
            overflow: visible !important;
        }

        /* 3. 清理页面冗余吸附 padding 与优化图片显示 */
        html, body {
            padding-top: 10px !important;
            padding-bottom: 30px !important;
            margin-top: 0 !important;
            overscroll-behavior-y: contain;
            -webkit-tap-highlight-color: transparent;
        }
        img {
            max-width: 100% !important;
            height: auto !important;
        }
        $themeCss
        """
    }

    private const val SCRIPT = """
(function(){
  // 屏蔽客户端唤醒
  window.openApp = function(){ return false; };
  window.launchApp = function(){ return false; };
  window.downloadApp = function(){ return false; };
  if (window.global) {
    window.global.openApp = function(){ return false; };
  }

  var CSS = __CSS__;

  function addStyle(){
    var s = document.getElementById('__dqd_style');
    if (!s) {
      s = document.createElement('style');
      s.id = '__dqd_style';
      (document.head || document.documentElement).appendChild(s);
    }
    s.textContent = CSS;
  }

  // 展开全文并移除折叠
  function unfoldAll(){
    var folds = document.querySelectorAll('.tt-fold, div[class*="tt-fold"], article, article.show-more');
    for (var i = 0; i < folds.length; i++) {
      var el = folds[i];
      if (el.classList) {
        el.classList.remove('folded');
        el.classList.remove('show-more');
      }
      el.style.maxHeight = 'none';
      el.style.height = 'auto';
      el.style.overflow = 'visible';

      // 若绑定了 Vue 实例组件状态，直接将内部 folded 标志位置 false
      if (el.__vue__) {
        try {
          el.__vue__.folded = false;
          el.__vue__.noBlock = true;
          el.__vue__.modal = false;
        } catch(e){}
      }
    }

    // 触发点击展开（以防注册了内部事件监听）
    var moreBtn = document.querySelector('.tt-more');
    if (moreBtn) {
      try { moreBtn.click(); } catch(e){}
    }
  }

  // 隐藏所有APP推广与浮层
  var BAD_WORDS = ['打开APP','APP内打开','下载APP','点击查看完整内容','在APP中阅读','打开客户端','立即下载'];
  function stripBanners(){
    var targets = document.querySelectorAll('.tt-top, .tt-open, .tt-float, .tt-bar, .tt-fade, .tt-more, .tt-modal, .tt-feature, .show-more-btn, .article-download-btn-container, a, button, div, span');
    for (var i = 0; i < targets.length; i++) {
      var el = targets[i];
      var cls = el.className || '';
      if (typeof cls === 'string' && (
          cls.indexOf('tt-top') >= 0 || cls.indexOf('tt-open') >= 0 || cls.indexOf('tt-float') >= 0 ||
          cls.indexOf('tt-bar') >= 0 || cls.indexOf('tt-fade') >= 0 || cls.indexOf('tt-more') >= 0 ||
          cls.indexOf('tt-modal') >= 0 || cls.indexOf('tt-feature') >= 0 || cls.indexOf('show-more-btn') >= 0
      )) {
        el.style.setProperty('display', 'none', 'important');
        continue;
      }
      var t = (el.innerText || '').trim();
      if (t && t.length < 20) {
        for (var b = 0; b < BAD_WORDS.length; b++) {
          if (t === BAD_WORDS[b]) {
            el.style.setProperty('display', 'none', 'important');
            break;
          }
        }
      }
    }
  }

  // 抓取并注入评论
  var commentsLoaded = false;
  function loadComments(){
    if (commentsLoaded) return;
    var m = window.location.href.match(/article[\/=](\d+)/);
    if (!m) return;
    var aid = m[1];

    fetch('https://api.dongqiudi.com/v2/article/' + aid + '/comment')
      .then(function(r){ return r.json(); })
      .then(function(res){
        if (!res || !res.data) return;
        var data = res.data;
        var list = (data.recommend_list && data.recommend_list.length > 0) ? data.recommend_list : data.comment_list;
        if (!list || list.length === 0) return;
        commentsLoaded = true;

        var usersMap = {};
        if (data.user_list && Array.isArray(data.user_list)) {
          data.user_list.forEach(function(u){ usersMap[u.id] = u; });
        }

        var box = document.getElementById('__dqd_comments_box');
        if (!box) {
          box = document.createElement('div');
          box.id = '__dqd_comments_box';
          box.style.padding = '16px 14px 40px 14px';

          var copyright = document.querySelector('.tt-copyright');
          var commentWrapper = document.getElementById('commentWrapper');
          var fold = document.querySelector('.tt-fold');
          var art = document.querySelector('article');

          if (copyright && copyright.parentNode) {
            copyright.parentNode.insertBefore(box, copyright);
          } else if (commentWrapper) {
            commentWrapper.appendChild(box);
          } else if (fold && fold.parentNode) {
            fold.parentNode.insertBefore(box, fold.nextSibling);
          } else if (art && art.parentNode) {
            art.parentNode.insertBefore(box, art.nextSibling);
          } else {
            document.body.appendChild(box);
          }
        }

        var headerHtml = '<div style="font-size:16px;font-weight:bold;margin:18px 0 12px 0;padding-left:4px;border-left:3px solid #00D11F;">精彩评论 (' + (data.comment_total || list.length) + ')</div>';
        var itemsHtml = '';

        list.forEach(function(c){
          var u = usersMap[c.user_id] || { username: '热心球友', avatar: 'https://img1.dongqiudi.com/fastdfs1/M00/3B/EF/100x100/-/-/o4YBAFjM9lqAel9GAAAQrsgeQ3A103.jpg' };
          var content = c.content || c.content_old || '';
          var time = c.created_at || '';
          var up = c.up_count || c.up || 0;

          itemsHtml += '<div class="comment-card" style="padding:12px 4px;margin-bottom:8px;">' +
            '<div style="display:flex;align-items:center;margin-bottom:6px;">' +
              '<img src="' + u.avatar + '" style="width:28px;height:28px;border-radius:14px;margin-right:8px;object-fit:cover;" onerror="this.style.display=\'none\'" />' +
              '<div style="flex:1;">' +
                '<div class="comment-user" style="font-size:12.5px;font-weight:600;">' + u.username + '</div>' +
                '<div class="comment-time" style="font-size:10.5px;">' + time + '</div>' +
              '</div>' +
              '<div class="comment-up" style="font-size:11.5px;">👍 ' + up + '</div>' +
            '</div>' +
            '<div class="comment-body" style="font-size:14px;line-height:1.5;padding-left:36px;">' + content + '</div>' +
          '</div>';
        });

        box.innerHTML = headerHtml + itemsHtml;
      })
      .catch(function(e){ console.log('load comment err', e); });
  }

  function sweep(){
    addStyle();
    unfoldAll();
    stripBanners();
    loadComments();
    if (document.body) {
      document.body.style.paddingTop = '10px';
      document.body.style.paddingBottom = '30px';
    }
  }

  // 监听异步渲染变化（防御 Vue/Nuxt 异步更新）
  var mo = new MutationObserver(function(){
    unfoldAll();
    stripBanners();
  });
  if (document.documentElement) {
    mo.observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['class', 'style'] });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', sweep);
  } else {
    sweep();
  }
  setTimeout(sweep, 300);
  setTimeout(sweep, 800);
  setTimeout(sweep, 1500);
  setTimeout(sweep, 3000);
})();
"""

    fun script(isDark: Boolean = true): String {
        val literal = getCss(isDark).trimIndent()
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "")
            .replace("\n", " ")
        return SCRIPT.trimIndent()
            .replace("__CSS__", "'$literal'")
            .replace("__IS_DARK__", "$isDark")
    }
}
