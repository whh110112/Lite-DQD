package com.whh11.dqdlite.util

/**
 * 懂球帝 WebView 页面净化、夜间/白天主题适配与全量评论注入脚本。
 * 1. 自动展开全文（隐藏"展开全文"拦截）；
 * 2. 隐藏"打开懂球帝APP"、下载引导浮层、底部推广条；
 * 3. 动态从官方公开接口拉取真实精彩评论与最新评论并高保真渲染，解决原网页需App原生Bridge才能读评论的限制；
 * 4. 彻底适配白天与夜间模式高对比度配色。
 */
object Injector {

    private fun getCss(isDark: Boolean): String {
        val themeCss = if (isDark) {
            """
            html, body, #app, .wrapper, .main, article, .con, .detail-container, .article-foot-recommend {
                background-color: #071009 !important;
                color: #E9F2EB !important;
            }
            p, span, div, h1, h2, h3, a, li, time, .title, .writer, .con p, .con span {
                color: #E9F2EB !important;
            }
            .writer, .time, time {
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
            html, body, #app, .wrapper, .main, article, .con, .detail-container, .article-foot-recommend {
                background-color: #FFFFFF !important;
                color: #1A241C !important;
            }
            p, span, div, h1, h2, h3, a, li, time, .title, .writer, .con p, .con span {
                color: #1A241C !important;
            }
            .writer, .time, time {
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
        .show-more-btn, .article-download-btn-container,
        [class*="download"], [class*="open-app"], [class*="app-guide"],
        .btn-app, .header, .top-header, .share-container, .pop-download,
        .recommend-container, .article-foot-recommend > div:first-child,
        .comm-container > div > a.btn {
            display: none !important;
            height: 0 !important;
            min-height: 0 !important;
            visibility: hidden !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
        article.show-more {
            max-height: none !important;
            height: auto !important;
            overflow: visible !important;
        }
        html, body {
            padding-top: 0 !important;
            margin-top: 0 !important;
            overscroll-behavior-y: contain;
            -webkit-tap-highlight-color: transparent;
        }
        $themeCss
        """
    }

    private const val SCRIPT = """
(function(){
  var CSS = __CSS__;
  var IS_DARK = __IS_DARK__;

  function addStyle(){
    var s = document.getElementById('__dqd_style');
    if (!s) {
      s = document.createElement('style');
      s.id = '__dqd_style';
      (document.head || document.documentElement).appendChild(s);
    }
    s.textContent = CSS;
    var art = document.querySelector('article.show-more');
    if (art) {
      art.style.maxHeight = 'none';
      art.style.height = 'auto';
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
          var parent = document.getElementById('commentWrapper') || document.querySelector('article') || document.body;
          parent.appendChild(box);
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
    loadComments();
    var btns = document.querySelectorAll('.show-more-btn, .article-download-btn-container');
    for (var i = 0; i < btns.length; i++) btns[i].style.display = 'none';
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', sweep);
  } else {
    sweep();
  }
  setTimeout(sweep, 600);
  setTimeout(sweep, 1800);
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
