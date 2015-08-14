/**
 * @version 3.0.4
 * @link https://github.com/gajus/contents for the canonical source repository
 * @license https://github.com/gajus/contents/blob/master/LICENSE BSD 3-Clause
 */
(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);throw new Error("Cannot find module '"+o+"'")}var f=n[o]={exports:{}};t[o][0].call(f.exports,function(e){var n=t[o][1][e];return s(n?n:e)},f,f.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
  (function (global){
    /**
     * @link https://github.com/gajus/sister for the canonical source repository
     * @license https://github.com/gajus/sister/blob/master/LICENSE BSD 3-Clause
     */
    function Sister () {
      var sister = {},
        events = {};

      /**
       * @name handler
       * @function
       * @param {Object} data Event data.
       */

      /**
       * @param {String} name Event name.
       * @param {handler} handler
       * @return {listener}
       */
      sister.on = function (name, handler) {
        var listener = {name: name, handler: handler};
        events[name] = events[name] || [];
        events[name].unshift(listener);
        return listener;
      };

      /**
       * @param {listener}
       */
      sister.off = function (listener) {
        var index = events[listener.name].indexOf(listener);

        if (index != -1) {
          events[listener.name].splice(index, 1);
        }
      };

      /**
       * @param {String} name Event name.
       * @param {Object} data Event data.
       */
      sister.trigger = function (name, data) {
        var listeners = events[name],
          i;

        if (listeners) {
          i = listeners.length;
          while (i--) {
            listeners[i].handler(data);
          }
        }
      };

      return sister;
    }

    global.gajus = global.gajus || {};
    global.gajus.Sister = Sister;

    module.exports = Sister;
  }).call(this,typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{}],2:[function(require,module,exports){
  (function (global){
    var Sister = require('sister'),
      Contents;

    /**
     * @param {object} config
     * @return {Contents}
     */
    Contents = function Contents (config) {
      var contents,
        articles,
        tree,
        list,
        eventEmitter;

      if (!(this instanceof Contents)) {
        return new Contents(config);
      }

      contents = this;

      eventEmitter = Sister();

      config = Contents.config(config);

      articles = Contents.articles(config.articles, config.articleName, config.articleId);
      tree = Contents.tree(articles);
      list = Contents.list(tree, config.link);

      Contents.bind(eventEmitter, list, config);

      /**
       * @return {HTMLElement} Ordered list element representation of the table of contents.
       */
      contents.list = function () {
        return list;
      };

      /**
       * @return {array} Array representation of the table of contents.
       */
      contents.tree = function () {
        return tree;
      };

      /**
       * @return {Sister} Event emitter used to attach event listeners and trigger events.
       */
      contents.eventEmitter = function () {
        return eventEmitter;
      };
    };

    /**
     * Setups event listeners to reflect changes to the table of contents and user navigation.
     *
     * @param {Sister} eventEmitter
     * @param {HTMLElement} list Table of contents root element (<ol>).
     * @param {object} config Result of contents.config.
     * @return {object} Result of contents.eventEmitter.
     */
    Contents.bind = function (eventEmitter, list, config) {
      var windowHeight,
        /**
         * @var {Array}
         */
        articleOffsetIndex,
        lastArticleIndex,
        guides = list.querySelectorAll('li');

      eventEmitter.on('resize', function () {
        windowHeight = Contents.windowHeight();
        articleOffsetIndex = Contents.indexOffset(config.articles);

        eventEmitter.trigger('scroll');
      });

      eventEmitter.on('scroll', function () {
        var articleIndex,
          changeEvent;

        articleIndex = Contents.getIndexOfClosestValue(Contents.windowScrollY() + windowHeight * .05, articleOffsetIndex);

        if (articleIndex !== lastArticleIndex) {
          changeEvent = {};

          changeEvent.current = {
            article: config.articles[articleIndex],
            guide: guides[articleIndex]
          };

          if (lastArticleIndex !== undefined) {
            changeEvent.previous = {
              article: config.articles[lastArticleIndex],
              guide: guides[lastArticleIndex]
            };
          }

          eventEmitter.trigger('change', changeEvent);

          lastArticleIndex = articleIndex;
        }
      });

      // This allows the script that constructs Contents
      // to catch the first ready, resize and scroll events.
      setTimeout(function () {
        eventEmitter.trigger('resize');
        eventEmitter.trigger('ready');

        global.addEventListener('resize', Contents.throttle(function () {
          eventEmitter.trigger('resize');
        }, 100));

        global.addEventListener('scroll', Contents.throttle(function () {
          eventEmitter.trigger('scroll');
        }, 100));
      }, 10);
    };

    /**
     * @return {Number}
     */
    Contents.windowHeight = function () {
      return global.innerHeight || global.document.documentElement.clientHeight;
    };

    /**
     * @return {Number}
     */
    Contents.windowScrollY = function () {
      return global.pageYOffset || global.document.documentElement.scrollTop;
    };

    /**
     * Interpret execution configuration.
     *
     * @param {Object} config
     * @return {Object}
     */
    Contents.config = function (config) {
      var properties = ['articles', 'articleName', 'articleId', 'link'];

      config = config || {};

      Contents.forEach(Object.keys(config), function (name) {
        if (properties.indexOf(name) === -1) {
          throw new Error('Unknown configuration property.');
        }
      });

      if (config.articles) {
        if (!config.articles.length || !(config.articles[0] instanceof HTMLElement)) {
          throw new Error('Option "articles" is not a collection of HTMLElement objects.');
        }
      } else {
        config.articles = global.document.querySelectorAll('h1, h2, h3, h4, h5, h6');
      }

      if (config.articleName) {
        if (typeof config.articleName !== 'function') {
          throw new Error('Option "articleName" must be a function.');
        }
      } else {
        config.articleName = Contents.articleName;
      }

      if (config.articleId) {
        if (typeof config.articleId !== 'function') {
          throw new Error('Option "articleId" must be a function.');
        }
      } else {
        config.articleId = Contents.articleId;
      }

      if (config.link) {
        if (typeof config.link !== 'function') {
          throw new Error('Option "link" must be a function.');
        }
      } else {
        config.link = Contents.link;
      }

      return config;
    };

    /**
     * Derive article name.
     *
     * This method can be overwritten using config.articleName.
     *
     * @param {HTMLElement} element
     */
    Contents.articleName = function (element) {
      return element.innerText || element.textContent;
    };

    /**
     * Derive article ID.
     *
     * This method can be overwritten using config.articleId.
     *
     * @param {String} articleName
     * @param {HTMLElement} element
     */
    Contents.articleId = function (articleName, element) {
      return element.id || articleName;
    };

    /**
     * Make element ID unique in the context of the document.
     *
     * @param {String} id
     * @param {Array} existingIDs Existing IDs in the document. Required for markup-contents. (https://github.com/gajus/markdown-contents)
     * @return {String}
     */
    Contents.uniqueID = function (id, existingIDs) {
      var assignedId,
        i = 1;

      id = Contents.formatId(id);

      if (existingIDs) {
        assignedId = id;

        while (existingIDs.indexOf(assignedId) != -1) {
          assignedId = id + '-' + (i++);
        }

        existingIDs.push(assignedId);
      } else {
        if (!global.document) {
          throw new Error('No document context.');
        }

        assignedId = id;

        while (global.document.querySelector('#' + assignedId)) {
          assignedId = id + '-' + (i++);
        }
      }

      return assignedId;
    };

    /**
     * Formats text into an ID/anchor safe value.
     *
     * @see http://stackoverflow.com/a/1077111/368691
     * @param {String} str
     * @return {String}
     */
    Contents.formatId = function (str) {
      return str
        .toLowerCase()
        .replace(/[ãàáäâ]/g, 'a')
        .replace(/[ẽèéëê]/g, 'e')
        .replace(/[ìíïî]/g, 'i')
        .replace(/[õòóöô]/g, 'o')
        .replace(/[ùúüû]/g, 'u')
        .replace(/[ñ]/g, 'n')
        .replace(/[ç]/g, 'c')
        .replace(/\s+/g, '-')
        .replace(/[^a-z0-9\-_]+/g, '-')
        .replace(/\-+/g, '-')
        .replace(/^\-|\-$/g, '')
        .replace(/^[^a-z]+/g, '');
    };

    /**
     * Generate flat index of the articles.
     *
     * @param {Array} NodeList
     * @param {Contents.articleName} articleName
     * @param {Contents.articleId} articleId
     * @return {Array}
     */
    Contents.articles = function (elements, articleName, articleId) {
      var articles = [];

      if (!articleName) {
        articleName = Contents.articleName;
      }

      if (!articleId) {
        articleId = Contents.articleId;
      }

      Contents.forEach(elements, function (element) {
        var article = {};

        article.level = Contents.level(element);
        article.name = articleName(element);
        article.id = articleId(article.name, element);
        article.element = element;

        articles.push(article);
      });

      return articles;
    };

    /**
     * Makes hierarchical index of the articles from a flat index.
     *
     * @param {Array} articles Generated using Contents.articles.
     * @param {Boolean} makeUniqueIDs
     * @param {Array} uniqueIDpool
     * @return {Array}
     */
    Contents.tree = function (articles, makeUniqueIDs, uniqueIDpool) {
      var root = {descendants: [], level: 0},
        tree = root.descendants,
        lastNode;

      Contents.forEach(articles, function (article) {
        if (makeUniqueIDs) {
          article.id = Contents.uniqueID(article.id, uniqueIDpool);
        }
        article.descendants = [];

        if (!lastNode) {
          tree.push(article);
        } else if (lastNode.level === article.level) {
          Contents.tree.findParentNode(lastNode, root).descendants.push(article);
        } else if (article.level > lastNode.level) {
          lastNode.descendants.push(article);
        } else {
          Contents.tree.findParentNodeWithLevelLower(lastNode, article.level, root).descendants.push(article);
        }

        lastNode = article;
      });

      return tree;
    };

    /**
     * Find the object whose descendant is the needle object.
     *
     * @param {Object} needle
     * @param {Object} haystack
     */
    Contents.tree.findParentNode = function (needle, haystack) {
      var i,
        parent;

      if (haystack.descendants.indexOf(needle) != -1) {
        return haystack;
      }

      i = haystack.descendants.length;

      while (i--) {
        if (parent = Contents.tree.findParentNode(needle, haystack.descendants[i])) {
          return parent;
        }
      }

      throw new Error('Invalid tree.');
    };

    /**
     * Find the object whose descendant is the needle object.
     * Look for parent (including parents of the found object) with level lower than level.
     *
     * @param {Object} needle
     * @param {Number} level
     * @param {Object} haystack
     */
    Contents.tree.findParentNodeWithLevelLower = function (needle, level, haystack) {
      var parent = Contents.tree.findParentNode(needle, haystack);

      if (parent.level < level) {
        return parent;
      } else {
        return Contents.tree.findParentNodeWithLevelLower(parent, level, haystack);
      }
    };

    /**
     * Generate ordered list from a tree (see tree) object.
     *
     * @param {Array} tree
     * @param {Function} link Used to customize the destination element in the list and the source element.
     * @return {HTMLElement}
     */
    Contents.list = function (tree, link) {
      var list = global.document.createElement('ol');

      Contents.forEach(tree, function (article) {
        var li = global.document.createElement('li');

        if (link) {
          link(li, article);
        }

        if (article.descendants.length) {
          li.appendChild(Contents.list(article.descendants, link));
        }

        list.appendChild(li);
      });

      return list;
    };

    /**
     * This function is called after the table of contents is generated.
     * It is called for each article in the index.
     * Used to represent article in the table of contents and to setup navigation.
     *
     * @todo wrong description
     * @param {HTMLElement} guide An element in the table of contents representing an article.
     * @param {Object} article {level, id, name, element, descendants}
     */
    Contents.link = function (guide, article) {
      var guideLink = global.document.createElement('a'),
        articleLink = global.document.createElement('a');

      article.element.id = article.id;

      articleLink.href = '#' + article.id;

      while (article.element.childNodes.length) {
        articleLink.appendChild(article.element.childNodes[0]);
      }

      article.element.appendChild(articleLink);

      guideLink.appendChild(global.document.createTextNode(article.name));
      guideLink.href = '#' + article.id;

      guide.insertBefore(guideLink, guide.firstChild);
    };

    /**
     * Extract element level used to construct list hierarchy, e.g. <h1> is 1, <h2> is 2.
     * When element is not a heading, use Contents.level data attribute. Default to 1.
     *
     * @param {HTMLElement} element
     * @return {Number}
     */
    Contents.level = function (element) {
      var tagName = element.tagName.toLowerCase();

      if (['h1', 'h2', 'h3', 'h4', 'h5', 'h6'].indexOf(tagName) !== -1) {
        return parseInt(tagName.slice(1), 10);
      }

      if (element.dataset['gajus.contents.level'] !== undefined) {
        return parseInt(element.dataset['gajus.contents.level'], 10);
      }

      if (jQuery && jQuery.data(element, 'gajus.contents.level') !== undefined) {
        return jQuery.data(element, 'gajus.contents.level');
      }

      return 1;
    };

    /**
     * Produce a list of offset values for each element.
     *
     * @param {NodeList} articles
     * @return {Array}
     */
    Contents.indexOffset = function (elements) {
      var scrollYIndex = [],
        i = 0,
        j = elements.length,
        adjustment = 3,
        element,
        offset;

      while (i < j) {
        element = elements[i++];

        offset = element.offsetTop + element.parentElement.offsetTop;

        // element.offsetTop might produce a float value.
        // Round to the nearest multiple of 5 (either up or down).
        // This is done to help readability and testing.
        offset = adjustment *(Math.round(offset/adjustment));

        scrollYIndex.push(offset);
      }

      return scrollYIndex;
    };

    /**
     * Find the nearest value to the needle in the haystack and return the value index.
     *
     * @see http://stackoverflow.com/a/26366951/368691
     * @param {Number} needle
     * @param {Array} haystack
     * @return {Number}
     */
    Contents.getIndexOfClosestValue = function (needle, haystack) {
      var closestValueIndex = 0,
        lastClosestValueIndex,
        i = 0,
        j = haystack.length;

      if (!j) {
        throw new Error('Haystack must be not empty.');
      }

      while (i < j) {
        if (Math.abs(needle - haystack[closestValueIndex]) > Math.abs(haystack[i] - needle)) {
          closestValueIndex = i;
        }

        if (closestValueIndex === lastClosestValueIndex) {
          break;
        }

        lastClosestValueIndex = closestValueIndex;

        i++;
      }

      return closestValueIndex;
    };

    /**
     * @callback throttleCallback
     * @param {...*} var_args
     */

    /**
     * Creates and returns a new, throttled version of the passed function, that, when invoked repeatedly,
     * will only call the original function at most once per every wait milliseconds.
     *
     * @see https://remysharp.com/2010/07/21/throttling-function-calls
     * @param {throttleCallback} throttled
     * @param {Number} threshold Number of milliseconds between firing the throttled function.
     * @param {Object} context The value of "this" provided for the call to throttled.
     */
    Contents.throttle = function (throttled, threshold, context) {
      var last,
        deferTimer;

      threshold = threshold || 250;
      context = context || {};

      return function () {
        var now = +new Date(),
          args = arguments;

        if (last && now < last + threshold) {
          clearTimeout(deferTimer);
          deferTimer = setTimeout(function () {
            last = now;
            throttled.apply(context, args);
          }, threshold);
        } else {
          last = now;
          throttled.apply(context, args);
        }
      };
    };

    /**
     * @callback forEachCallback
     * @param {Number} index
     */

    /**
     * Iterates over elements of a collection, executing the callback for each element.
     *
     * @param {Number} n The number of times to execute the callback.
     * @param {forEachCallback} callback
     */
    Contents.forEach = function (collection, callback) {
      var i = 0,
        j = collection.length;

      while (i < j) {
        callback(collection[i], i);

        i++;
      }
    };

    global.gajus = global.gajus || {};
    global.gajus.Contents = Contents;

    module.exports = Contents;
  }).call(this,typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{"sister":1}]},{},[2])
