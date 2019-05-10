/* global wdk */
/*
WDK Strategy System
resultsPage.js

Provides functions to support results table
*/

import _ from 'lodash';

wdk.namespace("window.wdk.resultsPage", function(ns, $) {

  function updateResultLabels(currentDiv, strat, step) {
    if (currentDiv.hasClass('Workspace')) {
      currentDiv.find("span#text_strategy_number").html(strat.JSON.name);
      currentDiv.find("span#text_step_number").html(step.frontId);
    }
  }

  function resultsToGrid(data, ignoreFilters, div) {
    var oldFilters;
    var currentDiv = div;
    if (currentDiv === undefined) currentDiv = window.wdk.findActiveView();
    if (ignoreFilters) {
      oldFilters = $("#strategy_results > div.Workspace div.layout-detail div.filter-instance .link-url");
    }

    currentDiv.html(data);

    // invoke new filters
    var wdkFilterNew = new wdk.filter.WdkFilterNew(currentDiv.find('.wdk-filters'));

    wdkFilterNew.initialize();


    // invoke filters
    var wdkFilter = new wdk.filter.WdkFilter(currentDiv.find('.result-filters'));

    wdkFilter.initialize();

    if (ignoreFilters) {
      oldFilters.each(function() {
        var newFilter = document.getElementById(this.id);
        var count = $(this).text();

        // no need to update if nodes are the same
        // refs #15011
        if (this === newFilter) return;

        if (count === 0 || !/\d+/.test(count)) {
          $(newFilter).replaceWith(this);
        } else {
          $(newFilter).html(count);
        }
      });
    } else {
      // Using setTimeout allows the results HTML to be rendered first, and
      // thus the results ajax is fired before the filters ajax. This will make
      // getting results faster when there are lots of filters.
      _.defer(wdkFilter.loadFilterCount.bind(wdkFilter));
    }

    // check the basket for the page if needed
    wdk.basket.checkPageBasket();

    currentDiv.unblock();
  }

  ns.resultsToGrid = resultsToGrid;
  ns.updateResultLabels = updateResultLabels;
});
