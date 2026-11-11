// Theme i18n: 中文/English switching with browser-language auto-detection.
// Translations come from the theme settings (theme.config.i18n.strings JSON:
// {"menu.首页":{"zh-CN":"首页","en":"Home"}, ...}) merged with built-in defaults.
(function () {
  'use strict';

  var DEFAULTS = {
    'menu.首页': { 'zh-CN': '首页', en: 'Home' },
    'menu.关于杰佳创': { 'zh-CN': '关于杰佳创', en: 'About Us' },
    'menu.产品中心': { 'zh-CN': '产品中心', en: 'Products' },
    'menu.新闻资讯': { 'zh-CN': '新闻资讯', en: 'News' },
    'menu.联系我们': { 'zh-CN': '联系我们', en: 'Contact Us' },
    'menu.新闻中心': { 'zh-CN': '新闻中心', en: 'News' },
    'menu.在线留言': { 'zh-CN': '在线留言', en: 'Message' },
    'menu.在线下单': { 'zh-CN': '在线下单', en: 'Order Online' },
    'menu.阿里1688': { 'zh-CN': '阿里1688', en: '1688 Shop' },
    'menu.淘宝店铺': { 'zh-CN': '淘宝店铺', en: 'Taobao Shop' },
    'menu.高压气体回收系统': { 'zh-CN': '高压气体回收系统', en: 'Gas Recovery' },
    'menu.高温炉节能优化项目': { 'zh-CN': '高温炉节能优化项目', en: 'Furnace Energy Saving' },
    'menu.饮料生产线改造': { 'zh-CN': '饮料生产线改造', en: 'Line Retrofit' },
    'menu.关于我们': { 'zh-CN': '关于我们', en: 'About Us' },
    'menu.公司简介': { 'zh-CN': '公司简介', en: 'Company Profile' },
    'sidebar.contact': { 'zh-CN': '联系我们', en: 'Contact Us' },
    'sidebar.address': { 'zh-CN': '地址：广东省佛山市南海区罗村桂丹路华泓商业城305B', en: 'Add: Room 305B, Huahong Business City, Luocun Guidan Road, Nanhai, Foshan, Guangdong' },
    'section.products': { 'zh-CN': '产品中心', en: 'Products' },
    'section.news': { 'zh-CN': '新闻资讯', en: 'News' },
    'section.about': { 'zh-CN': '关于杰佳创', en: 'About Us' },
    'section.more': { 'zh-CN': '更多动态', en: 'More News' },
    'section.hotline': { 'zh-CN': '服务热线', en: 'Hotline' },
    'section.noProducts': { 'zh-CN': '该分类下暂无产品。', en: 'No products in this category yet.' },
    'section.viewAll': { 'zh-CN': '查看完整介绍', en: 'Read Full Profile' },
    'section.contactHeading': { 'zh-CN': '评论', en: 'Comments' },
    'page.categoriesTitle': { 'zh-CN': '产品中心', en: 'Products' },
    'page.archivesTitle': { 'zh-CN': '文章归档', en: 'Archives' },
    'page.tagsTitle': { 'zh-CN': '标签', en: 'Tags' },
    'page.prev': { 'zh-CN': '上一页', en: 'Previous' },
    'page.next': { 'zh-CN': '下一页', en: 'Next' },
    'page.empty': { 'zh-CN': '还没有发布任何文章。', en: 'No posts yet.' },
  };

  var STORE_KEY = 'theme-lang';

  function detect() {
    try {
      var saved = localStorage.getItem(STORE_KEY);
      if (saved === 'zh-CN' || saved === 'en') return saved;
    } catch (e) { /* localStorage unavailable */ }
    var nav = (navigator.languages && navigator.languages[0]) || navigator.language || 'zh-CN';
    return /^en/i.test(nav) ? 'en' : 'zh-CN';
  }

  function strings() {
    var el = document.body;
    var custom = {};
    if (el && el.dataset.i18nBase) {
      try { custom = JSON.parse(el.dataset.i18nBase) || {}; } catch (e) { /* ignore */ }
    }
    var merged = {};
    Object.keys(DEFAULTS).forEach(function (k) { merged[k] = DEFAULTS[k]; });
    Object.keys(custom).forEach(function (k) {
      merged[k] = Object.assign({}, DEFAULTS[k] || {}, custom[k]);
    });
    return merged;
  }

  function apply(lang) {
    var dict = strings();
    document.documentElement.lang = lang;
    document.querySelectorAll('[data-i18n-key]').forEach(function (el) {
      var entry = dict[el.getAttribute('data-i18n-key')];
      if (entry && entry[lang]) {
        if (el.hasAttribute('data-i18n-orig') === false) {
          el.setAttribute('data-i18n-orig', el.textContent);
        }
        el.textContent = entry[lang];
      }
    });
    document.querySelectorAll('.theme-lang-current').forEach(function (el) {
      el.textContent = lang === 'en' ? 'English' : '中文';
    });
    document.querySelectorAll('.theme-lang-pick').forEach(function (a) {
      a.classList.toggle('active', a.dataset.lang === lang);
    });
  }

  function current() { return document.documentElement.lang || detect(); }

  document.addEventListener('DOMContentLoaded', function () {
    apply(detect());
    document.querySelectorAll('.theme-lang-pick').forEach(function (a) {
      a.addEventListener('click', function (ev) {
        ev.preventDefault();
        var lang = a.dataset.lang === 'en' ? 'en' : 'zh-CN';
        try { localStorage.setItem(STORE_KEY, lang); } catch (e) { /* ignore */ }
        apply(lang);
      });
    });
  });

  // expose for re-apply after dynamic content changes
  window.themeI18n = { apply: apply, current: current };
})();
