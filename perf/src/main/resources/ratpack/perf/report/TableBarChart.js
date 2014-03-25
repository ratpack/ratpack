/*
 * Copyright 2014 the original author or authors.
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

jQuery.fn.tableBarChart = function(targetDiv, caption, reverseGroup) {
  var source = $(this);
  var target = $(targetDiv);
  var maxValue = 0;
  var minValue = Number.MAX_VALUE;
  var yAxisMax = 0;
  var yAxisMin = 0;
  var groupTitles = [];
  var legends = [];
  var tableData = [];

  function getHeaderNames(isColumn) {
    var parentNode = isColumn ? 'thead' : 'tbody';
    var names = [];
    source.find(parentNode + ' th').each(function(index, element) {
      // skip first cell
      if (index > 0 || isColumn === undefined || !isColumn) {
        names.push($(element).text());
      }
    });
    return names;
  }

  function getDataByRow() {
    var dataMatrix = [];
    source.find('tbody tr').each(function(i, trElement) {
      var rowValue = [];
      $(trElement).find('td').each(function(j, tdElement) {
        var intValue = parseInt($(tdElement).text());
        if (intValue > maxValue) {
          maxValue = intValue;
        } else if (intValue < minValue) {
          minValue = intValue;
        }
        rowValue.push(intValue);
      });
      dataMatrix.push(rowValue);
    });
    return dataMatrix;
  }

  function getDataByColumn() {
    var dataMatrix = [];
    var numCols = source.find('tbody tr:eq(0) td').size();
    for (var i = 0; i < numCols; i++) {
      var colValue = [];
      source.find('tbody tr').each(function() {
        var intValue = parseInt($($(this).find('td:eq(' + i + ')')[0]).text());
        if (intValue > maxValue) {
          maxValue = intValue;
        } else if (intValue < minValue) {
          minValue = intValue;
        }
        colValue.push(intValue);
      });
      dataMatrix.push(colValue);
    }
    return dataMatrix;
  }

  function getCaption() {
    if (caption === undefined || caption.length === 0) {
      caption = '';
      source.find('caption').each(function() {
        caption = $(this).text();
      });
    }
    return caption;
  }

  function getAxisHTML(sourceArray, cssClass) {
    var axis = $('<ul class="' + cssClass + '"></ul>');
    $(sourceArray).each(function() {
      axis.append('<li><span>' + this + '</span></li>');
    });
    return axis;
  }

  function getYAxisArray(stepCount) {
    var maxDigitCount = String(maxValue).length;
    var minDigitCount = String(minValue).length;
    var base10 = Math.pow(10, maxDigitCount - 1);
    yAxisMax = base10 * (Math.floor(maxValue / base10) + 1);
    if (maxDigitCount === minDigitCount) {
      yAxisMin = base10 * (Math.floor(minValue / base10));
    }
    var result = [];
    var step = (yAxisMax - yAxisMin) / stepCount;
    for (var i = stepCount; i >= 0; i--) {
      var stepValue = yAxisMin + step * i;
      if (stepValue % 1 !== 0) {
        stepValue = parseFloat(Math.round(stepValue * 100) / 100).toFixed(2);
      }
      result.push(stepValue);
    }
    return result;
  }

  function animateBar(index) {
    $('.bar.item-' + index).each(function() {
      var bar = $(this);
      bar.css('height', 0);
      var value = bar.attr('value');
      bar.animate({
        'height': value
      }, 800);
    });
  }

  function getLegendHTML() {
    var legendContainer = $('<ul class="legend"></ul>');
    $(legends).each(function(index) {
      var legendItem = $('<li><span class="icon item-' + index + '"></span>' +
        this + '</li>');
      legendItem.mouseenter(function() {
        animateBar(index);
      });
      legendContainer.append(legendItem);
    });
    return legendContainer;
  }

  function getBarChartHTML() {
    var barsContainer = $('<div class="bars"></div>');
    $(tableData).each(function(i, columnGroup) {
      var barGroup = $('<div class="bar-group"></div>');
      $(columnGroup).each(function(j, cell) {
        var bar = $('<div class="bar item-' + j + '" value="' +
          Math.floor((cell - yAxisMin) / (yAxisMax - yAxisMin) * 100) + '%"><span>' +
          cell + '</span></div>');

        // CSS :hover won't work on IE
        bar.hover(function() {
          bar.find('span').css('display', 'block');
        }, function() {
          bar.find('span').css('display', 'none');
        });

        barGroup.append(bar);
      });
      barsContainer.append(barGroup);
    });
    return barsContainer;
  }

  function layout() {
    var defaultMargin = 10;
    var yAxisWidth = 50;
    $('.y-axis').css('width', '100%');
    $('.y-axis span').css('width', yAxisWidth).css('margin', '-' + defaultMargin + 'px 0 0 -' + (yAxisWidth + defaultMargin) + 'px');
    var graphWidth = target.width() - (yAxisWidth + 2 * defaultMargin);
    var graphHeight = target.height() - $('.caption').height() - $('.legend').height() - 3 * defaultMargin;
    $('.graph').css('width', graphWidth).css('height', graphHeight);
    var stepHeight = Math.floor((graphHeight - $('.x-axis').height() - 2 * defaultMargin) / (groupTitles.length + 1));
    $('.y-axis li').css('height', stepHeight).css('width', '100%');
    var barGroupHeight = ($('.y-axis li').height() + 1) * (groupTitles.length + 1);
    $('.bars').css('height', barGroupHeight).css('width', '100%');
    var barGroupWidth = graphWidth / groupTitles.length - 2 * defaultMargin;
    $('.bar-group').css('width', barGroupWidth).css('margin', '0 ' + defaultMargin);
    $('.x-axis li').css('width', barGroupWidth);
    var barWidth = barGroupWidth / legends.length - 2;
    $('.bar').css('width', barWidth);
    for (var i = 0; i < legends.length; i++) {
      $('.bar.item-' + i).css('left', i * (barWidth + 2));
      animateBar(i);
    }
  }

  function render() {
    target.append('<div class="caption">' + getCaption() + '</div>');
    var graphContainer = $('<div class="graph"></div>');
    graphContainer.append(getAxisHTML(groupTitles, 'x-axis'));
    graphContainer.append(getAxisHTML(getYAxisArray(groupTitles.length + 1), 'y-axis'));
    graphContainer.append(getBarChartHTML());
    target.append(graphContainer);
    target.append(getLegendHTML());
    layout();
  }

  function initialize() {
    groupTitles = getHeaderNames(reverseGroup ? false : true);
    legends = getHeaderNames(reverseGroup ? true : false);
    tableData = reverseGroup ? getDataByRow() : getDataByColumn();
    render();
  }

  initialize();
}