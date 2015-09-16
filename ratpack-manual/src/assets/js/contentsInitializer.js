/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

!(function($) {
  $(function() {
    if ($('.single-toc').length) {
      var contents = gajus.Contents({
        articles: $('.manual').find('h1, h2, h3, h4, h5, h6')
      });
      document.querySelector('.nav-toc').appendChild(contents.list());
      var eventEmitter = contents.eventEmitter();
      eventEmitter.on('change', function (data) {

        $(data.current.article).addClass('active-article');

        var currentActiveGuide = $(data.current.guide);
        var topLi = currentActiveGuide.parents('li').last();
        if (!topLi.length) {
          topLi = currentActiveGuide;
        }
        topLi.addClass('active-chapter');
        currentActiveGuide.addClass('active-guide');

        if (data.previous) {
          $(data.previous.article).removeClass('active-article');
          var activeGuide = $(data.previous.guide);
          var prevLi = activeGuide.parents('li').last();
          if (!prevLi.length) {
            prevLi = activeGuide;
          }

          if (!prevLi.is(topLi)) {
            prevLi.removeClass('active-chapter')
          }

          activeGuide.removeClass('active-guide');
        }

      });
    }
  });
} (jQuery));

