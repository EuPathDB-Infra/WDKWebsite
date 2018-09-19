/* global customShowError, customSampleTab, customHelpTab, wdk */
/* jshint evil:true */

import _ from 'lodash';

/**
 * This file contains functions used to communicate strategy operations between
 * the client and the server.
 *
 * The current contract between client and server is as follows:
 *  - Client sends a request to the server to perform a strategy operation.
 *  - Server fulfills request and returns a new state object
 *  - Client resolves differences between new state object and local state
 *    object.
 *  - Client calls updateStrategies function which uses the resolved local
 *    state object to determine if a strategy's UI should be redrawn. The
 *    strategy's checksum is used to determine this action: if the checksum
 *    for a strategy changes between requests, it is redrawn.
 *
 */

wdk.namespace("window.wdk.strategy.controller", function (ns, $) {
  "use strict";

  var sidIndex = 0;
  ns.state = null;
  ns.strats = {};
  ns.stateString = '';

  // Current strategy and step objects
  var uiState = {
    strategy: null,
    step: null
  };

  function setUIState(newState) {
    Object.assign(uiState, newState);
  }

  function init(element, attrs) {

    // Selects the last step of the first strategy
    wdk.step.init();

    // decide whether strategy panel should be shown
    handleStratPanelVisibility(element);

    setupStepActionButtons(element);

    // Make the strategies window resizable
    element.find(".resizable-wrapper").resizable({
      handles: 's',
      minHeight: 150,
      stop: function() {
        wdk.stratTabCookie.setCurrentTabCookie('strategyWindow', $(".resizable-wrapper").height());
      }
    });

    // tell jQuery not to cache ajax requests.
    // generic error handling of ajax calls
    $.ajaxSetup ({
      cache: false,
      timeout: 1000 * 60 * 5, // was 180000 ms
      error: function(data, msg) {
        if (msg == "timeout") {
          var c = confirm("This request has timed out.\n" +
              "Would you like to try again? (This request will timeout after " +
              ((this.timeout / 60000) + 1) +" minutes.)");
          if (c) {
            this.timeout = this.timeout + 60000;
            $.ajax(this);
          } else {
            if ( (this.url.indexOf("showSummaryView.do") != -1) || (this.url.indexOf("showSummary.do") != -1 ) ) {
              wdk.util.removeLoading();
            } else {
              initDisplay();
            }
          }
        } else if (data.readyState !== 0 && data.status !== 0) {
          // not timeout, backend throws errors
          try {
            customShowError();
          } catch(err) {
            alert("controller.js: a backend error occurred.");
          }
          if (this.url.indexOf("showSummary.do") != -1) {
            wdk.util.removeLoading();
          }
        }
      }
    });

    // remove tabs that don't have an associated jsp
    initStrategyTabs();

    // init DYK and show strategy tab
    wdk.addStepPopup.showPanel(chooseStrategyTab(attrs.allCount, attrs.openCount));

    // get strategies json from server and draw strategies ui
    initDisplay();

    // strategyselect event fired when a step in a strategy is selected
    $("#Strategies")
      .on("strategyselect", ".diagram", function(e, strategy) {
        $(e.delegateTarget).find(".strategy-name.wdk-editable")
          .editable("hide");

        if (strategy.Steps.length > 1 && !strategy.hasCustomName()) {
          $(this).find(".strategy-name.wdk-editable").editable("show");
        }
      })
      .on("stepselect", function(e, step, isBoolean) {
        var stepId = isBoolean ? step.back_boolean_Id : step.back_step_Id;
        var $detailBoxes = $('.crumb_details');
        var $detailBox = $('#crumb_details_' + stepId);

        // disable View for selected Step detail box
        $detailBoxes.find('.view_step_link').removeClass('disabled');
        $detailBox.find('.view_step_link').addClass('disabled');

        // enable Analyze for selected Step detail box
        $detailBoxes.find('.analyze_step_link').addClass('disabled');
        $detailBox.find('.analyze_step_link').removeClass('disabled');
      });

    // update state when analysis status changes
    $(document).on('analysis:statuschange', function() {
      fetchStrategies(mergeStrategies);
    });

    // Add delegate submit handler here for question form
    // The callback is called when the event bubbles up to the body
    // element, where the event target is the question form. This
    // allows for a sort of "late binding" so it's called last.
    // We do this so custom Site submit handlers can cancel a form
    // submission just by calling event.preventDefault(), or
    // event.stopPropagation().
    $(document.body).on('submit', '#query_form form[name=questionForm]', wdk.addStepPopup.validateOperations);
  }

  function handleStratPanelVisibility(parentElement) {
    // set initial visibility
    doPanelVisibility(false);
    // add click event to toggle
    $(parentElement).on('click', '[data-action="toggle-strat-panel"]', function() {
      doPanelVisibility(true);
    });
    $(parentElement).on('stepselect', function() {
      doPanelVisibility(false);
    });
  }

  // define function to check cookie value and assign visibility accordingly
  function doPanelVisibility(isToggle = false) {
    // get DOM objects we will manipulate
    var $stratPanel = $('#strategies-panel');
    var $togglePanel = $('#strategies-panel-toggle');

    // exit if panel toggle does not exist
    if ($togglePanel.length === 0) return;

    // set cookie to default value if not already set
    if ($.cookie("show-strat-panel") == undefined) {
      $.cookie("show-strat-panel", $togglePanel.data('default'));
    }
    var cookieValue = $.cookie("show-strat-panel") === 'true';
    var showPanel = (isToggle ? !cookieValue : cookieValue);
    $togglePanel.find('img').attr('src', wdk.webappUrl("wdk/images/" + (showPanel ? 'minus.gif' : 'plus.gif')));
    $togglePanel.find('.toggle-command').html(showPanel ? 'Hide' : 'Show');
    $stratPanel.css("display", (showPanel ? 'block' : 'none'));
    $.cookie("show-strat-panel", (showPanel ? "true" : "false"));

    // update button text
    $('button[data-action="toggle-strat-panel"]')
      .each(function() {
        $(this).text($(this).data(showPanel ? 'hide-text' : 'show-text'));
      });

    // open DYK if not seen before
    if (showPanel) wdk.dyk.dykOpenIfNotSeen();
    else wdk.dyk.initDYK(false);
  }

  function setupStepActionButtons($element) {
    $element.on('click', '[data-action="revise-step"]', function(event) {
      var $target = $(event.target);
      var stepId = $target.data('step-id');
      var $detailBox = $(`#crumb_details_${stepId}`);
      if ($detailBox.is('.operation_details')) {
        $detailBox.show();
      }
      else {
        $detailBox.find('.edit_step_link').click();
      }
    });
  }

  /**
   * Returns the name of the tab to open based on all and open counts
   */
  function chooseStrategyTab(allCount, openCount) {
    var openTabName = 'strategy_results';
    var allTabName = 'search_history';
    // set tab state from query param, if present, and remove query param from url
    var tabQueryParamMatches = location.search.match(/\btab=(\w+)/);
    if (tabQueryParamMatches) {
      wdk.stratTabCookie.setCurrentTabCookie('application', tabQueryParamMatches[1]);
      history.replaceState('', null, location.pathname);
    }
    var current = wdk.stratTabCookie.getCurrentTabCookie('application');

    if (!current || current === null) {
      // no cookie set
      return (openCount > 0 || allCount === 0 ? openTabName : allTabName);
    }
    else {
      // cookie set
      if (current == allTabName) {
        return (allCount > 0 ? allTabName : openTabName);
      }
      else if (current == openTabName) {
        return (openCount > 0 || allCount === 0 ? openTabName : allTabName);
      }
      else {
        return current;
      }
    }
  }

  /**
   * For each tab, if associated JSP exists, call custom function if defined.
   * Otherwise, remove the tab.
   *
   * TODO - we should make this a configuration option.
   * Then users can name the files whatever they want
   * and we don't have to perform unnecessary HTTP requests
   * if nothing is configured.
   */
  function initStrategyTabs() {
    // Fetch sample, new strat, and help tab contents
    // If no page is found for a given tab, remove
    // that tab from the page
    $.ajax({
      url: "wdkCustomization/jsp/strategies/samplesTab.jsp",
      type: "GET",
      dataType: "html",
      success: function(data) {
        $("#sample_strat").html(data);
        try {
          customSampleTab();
        }
        catch(e) {}
      },
      error: function() {
        $("#tab_sample_strat").parent("li").remove();
      }
    });

    $.ajax({
      url: "wdkCustomization/jsp/strategies/newTab.jsp",
      type: "GET",
      dataType: "html",
      success: function(data) {
        $("#strategy_new").html(data);
      },
      error: function() {
        $("#tab_strategy_new").parent("li").remove();
      }
    });

    $.ajax({
      url: "wdkCustomization/jsp/strategies/helpTab.jsp",
      type: "GET",
      dataType: "html",
      success: function(data) {
        $("#help").html(data);
        try {
          customHelpTab();
        }
        catch(e) {}
      },
      error: function() {
        $("#tab_help").parent("li").remove();
      },
      complete: function() {
        wdk.dyk.initHelp();
      }
    });
  }

  /**
   * Get state info from server and call updateStrategies
   */
  function initDisplay(){
    ns.stateString = '';

    // it doesn't make sense for this to be in util
    wdk.util.showLoading();
    fetchStrategies(updateStrategies);
  }

  /**
   * Fetch state info from server
   */
  function fetchStrategies(cb, options = {}) {
    var data = {
      state: encodeURIComponent(ns.stateString),
      updateResults: Boolean(options.updateResults)
    };

    $.ajax({
      url: "showStrategy.do",
      type: "POST",
      dataType: "json",
      data: data,
      success: function(data) {
        cb(data);
      }
    });
  }

  /**
   * Update the namespace-attached `strats` array
   *
   * The state is attached to the namespace. Then, strategies are removed based
   * on the state object. Then, strategies are loaded (see @loadModel) if the
   * strategy is not loaded, or the checksum of the strategy has changed.
   *
   * @param {Object} data Object retreived from server with state information
   * @param {Boolean} ignoreFilters If `true` filters will not be reloaded.
   *   Otherwise they will be reloaded.
   */
  function updateStrategies(data, ignoreFilters = false, count = 1) {
    // Increment count if we refetch strategies with updated results
    var nextUpdateStrategies = _.partialRight(updateStrategies, ignoreFilters,
                                              count + 1);

    // Fetch strategies with updated results. Used if root step results == -1
    var updateResults = _.partial(fetchStrategies, nextUpdateStrategies, {
      updateResults: true
    });

    ns.state = data.state;
    ns.stateString = JSON.stringify(ns.state);

    // If root step size is -1, fetch strategies with updated results and skip
    // updating the strategy panel.
    for (var checksum in data.strategies) {
      if (checksum == 'length') continue;
      var strategy = data.strategies[checksum];
      if (strategy.steps[strategy.steps.length].results == -1) {
        setTimeout(updateResults, 250 * count);
        break;
      }
    }

    // if a strategy is removed, update display
    removeClosedStrategies();
    for (var newOrdering in ns.state) {

      if (newOrdering == "count") {
        // it appears the span was removed, so this code does nothing.
        $("#mysearch span").text('My Strategies ('+ns.state[newOrdering]+')');
      }

      else if (newOrdering != "length") {
        // Always reload the strategy objects.
        var strategy = data.strategies[ns.state[newOrdering].checksum];
        if (strategy) {
          loadModel(strategy, newOrdering);
        }
        // var strategyId = ns.state[newOrdering].id;
        // if (wdk.strategy.model.isLoaded(strategyId)) {
        //   var loadedStrategy = wdk.strategy.model.getStrategyFromBackId(strategyId);
        //   var newStrategy = data.strategies[ns.state[newOrdering].checksum];
        //   var loadedRootStep = loadedStrategy.JSON.steps[loadedStrategy.JSON.steps.length];
        //   var newRootStep = newStrategy.steps[newStrategy.steps.length];

        //   if (loadedStrategy.checksum != ns.state[newOrdering].checksum ||
        //       loadedRootStep.results != newRootStep.results) {
        //     // If the checksums are not the same, reload the model.
        //     // This assumes the strategy object in the response (`data`)
        //     // matches the current strategy in the global state object.
        //     loadModel(data.strategies[ns.state[newOrdering].checksum], newOrdering);
        //   }
        // }

        // else {
        //   loadModel(data.strategies[ns.state[newOrdering].checksum], newOrdering);
        // }
      }
    }

    showStrategies(data.currentView, ignoreFilters, data.state.length);
  }

  // Use this to sync server objects with client objects without redrawing
  function mergeStrategies(data) {
    ns.state = data.state;
    ns.stateString = JSON.stringify(ns.state);

    for (var newOrdering in ns.state) {
      if (newOrdering != "length" && newOrdering != "count") {
        var strategyId = ns.state[newOrdering].id;
        if (wdk.strategy.model.isLoaded(strategyId)) {
          if (wdk.strategy.model.getStrategyFromBackId(ns.state[newOrdering].id).checksum ==
              ns.state[newOrdering].checksum) {
            // If the checksums are not the same, reload the model.
            // This assumes the strategy object in the response (`data`)
            // matches the current strategy in the global state object.
            mergeModel(data.strategies[ns.state[newOrdering].checksum]);
          }
        }
      }
    }
  }

  /**
   * Remove strategies from, and reorders, the namespace-attached `strats`
   *  array.
   */
  function removeClosedStrategies(){
    var removedStrategy = false;

    for (var currentOrder in ns.strats) {
      if (currentOrder.indexOf(".") == -1) {
        var removeTopStrategy = true;
        for (var newOrder in ns.state) {
          if (newOrder != "length") {
            if (ns.strats[currentOrder].checksum == ns.state[newOrder].checksum) {
              removeTopStrategy = false;
              if (newOrder != currentOrder) {
                ns.strats[newOrder] = ns.strats[currentOrder];
                removeSubStrategies(currentOrder, newOrder);
                delete ns.strats[currentOrder];
                break;
              }
            } else if(ns.strats[currentOrder].backId == ns.state[newOrder].id) {
              removeTopStrategy = false;
              removeSubStrategies(currentOrder, newOrder);
              if (newOrder != currentOrder) {
                ns.strats[newOrder] = ns.strats[currentOrder];
                break;
              }
            }
          }
        }
        if (removeTopStrategy) {
          removeSubStrategies(currentOrder);
          delete ns.strats[currentOrder];
          wdk.history.update_hist(true); //set update flag for history if anything was closed.
          removedStrategy = true;
        }
      }
    }

    return removedStrategy;
  }

  /**
   * Remove substrategies from `strats`. If `newOrder` is defined, then the
   * substrategy's parent stategy order is updated. Otherwise, the substrategy
   * is removed.
   *
   * @param {Number} currentOrder The stategy's current order
   * @param {Number} newOrder The strategy's new order
   */
  function removeSubStrategies(currentOrder, newOrder){
    for (var order in ns.strats) {
      if (order.split(".").length > 1 && order.split(".")[0] == currentOrder) {
        if (newOrder === undefined) {
          delete ns.strats[order];
        } else {
          var n_ord = order.split(".");
          n_ord[0] = newOrder;
          n_ord = n_ord.join(".");
          ns.strats[n_ord] = ns.strats[order];
          delete ns.strats[order];
        }
      }
    }
  }

  /**
   * Insert strategy HTML into DOM.
   *
   * @param {Object} view Current strategy, step, and results offset retrieved
   *    from server.
   * @param {Boolean} ignoreFilters If `true`, don't reload filters; otherwise
   *    reload filters.
   * @param {Number} count The number of open strategies
   * @param {Object} jQuery.Deferred object used to allow promise chaining for
   *  updating strategies. This adds complexity and will probably removed in
   *  favor of event triggering.
   */
  function showStrategies(view, ignoreFilters, count, deferred){
    $("#tab_strategy_results font.subscriptCount").text("(" + count + ")");
    var sC = 0;
    for (var s in ns.strats) {
      if (s.indexOf(".") == -1) {
        sC++;
      }
    }
    var s2 = document.createElement('div');

    // remove existing crumb_details from workspace
    $('.crumb_details').remove();

    for (var t=1; t<=sC; t++) {
      var strategyDiv = wdk.strategy.view.displayModel(ns.strats[t]);
      $(s2).prepend(strategyDiv);
      displayOpenSubStrategies(ns.strats[t], s2);
    }
    $("#strategy_messages").hide();
    $("#strategy_results #strategies-panel-toggle").show();
    doPanelVisibility();

    $('#Strategies').html(s2);
    var height = wdk.stratTabCookie.getCurrentTabCookie('strategyWindow');
    var wrapper = $("#strategy_results .resizable-wrapper:has(#Strategies)");
    if (!height && $("#Strategies").parent().parent().height() > 330) {
      // unless otherwise specified, don't allow height > 330
      wrapper.height(330);
    } else if (height) {
      height = parseInt(height, 10);
      if (wrapper.resizable("option", "minHeight") <= height) {
        // shrink wrapper to specified height only if no less than minHeight
        wrapper.height(height);
      } else if ($("#Strategies").height() + 10 < wrapper.height()) {
        // shrink wrapper to fit Strategies
        wrapper.height($("#Strategies").height() + 10);
      }
    }
    if (view.action !== undefined) {
      if (view.action == "share" || view.action == "save") {
        var x = $("a#" + view.action + "_" + view.actionStrat);
        x.click();
      }
    }
    if (view.strategy !== undefined || view.step !== undefined) {
      var initStr = wdk.strategy.model.getStrategyFromBackId(view.strategy);
      var initStp = initStr.getStep(view.step, false);
      if (initStr === false || initStp === null) {
        newResults(-1);
      }
      else if (initStp.isLoading) {
        // newResults(-1);
        wdk.util.showLoading(initStr.frontId);
      }
      else {
        var isVenn = (initStp.back_boolean_Id == view.step);
        var pagerOffset = view.pagerOffset;
        if (view.action !== undefined && view.action.match("^basket")) {
          newResults(initStr.frontId, initStp.frontId, isVenn, pagerOffset,
              ignoreFilters, view.action, deferred);
        } else {
          newResults(initStr.frontId, initStp.frontId, isVenn, pagerOffset,
              ignoreFilters, null, deferred);
        }
      }
    } else {
      newResults(-1);
    }
    if (sC === 0) showInstructions();
    // add fancy tooltips
    wdk.tooltips.assignTooltips(".filterImg", 0);
    wdk.tooltips.assignTooltips(".step-elem", 0);
  }

  /**
   * Insert substrategies into DOM and add colored border around them and
   * associated steps in parent strategies.
   *
   * @param {Object} strategy Parent strategy
   * @param {Object} div DOM node
   */
  function displayOpenSubStrategies(strategy, div) {
    //Colors for expanded substrategies
    var indent = 20;
    var colors = [
      {step:"#A00000", top:"#A00000", right:"#A00000", bottom:"#A00000", left:"#A00000"},
      {step:"#A0A000", top:"#A0A000", right:"#A0A000", bottom:"#A0A000", left:"#A0A000"},
      {step:"#A000A0", top:"#A000A0", right:"#A000A0", bottom:"#A000A0", left:"#A000A0"},
      {step:"#00A0A0", top:"#00A0A0", right:"#00A0A0", bottom:"#00A0A0", left:"#00A0A0"},
      {step:"#0000A0", top:"#0000A0", right:"#0000A0", bottom:"#0000A0", left:"#0000A0"}
    ];
    Object.keys(strategy.subStratOrder).sort().forEach(function(j) {
      var subs = wdk.strategy.model.getStrategy(strategy.subStratOrder[j]);
      var subStrategyDiv = wdk.strategy.view.displayModel(subs);
      subs.color = parseInt(strategy.getStep(wdk.strategy.model.getStrategy(strategy.subStratOrder[j]).backId.split("_")[1],false).frontId, 10) % colors.length;
      $(subStrategyDiv).addClass("sub_diagram").css({
        "margin-left": (subs.depth(null) * indent) + "px",
        "border-color": colors[subs.color].top+" "+colors[subs.color].right+" "+colors[subs.color].bottom+" "+colors[subs.color].left
      });
      $("div#diagram_" + strategy.frontId + " div#step_" + strategy.getStep(wdk.strategy.model.getStrategy(strategy.subStratOrder[j]).backId.split("_")[1],false).frontId + "_sub", div).css({"border-color":colors[subs.color].step});
      $("div#diagram_" + strategy.frontId, div).after(subStrategyDiv);
      if (wdk.strategy.model.getSubStrategies(strategy.subStratOrder[j]).length > 0) {
        displayOpenSubStrategies(wdk.strategy.model.getStrategy(strategy.subStratOrder[j]),div);
      }
    });
  }

  function showInstructions() {
    $("#strategy_messages").empty();
    //$("#strat-instructions").remove();
    //$("#strat-instructions-2").remove();
    //var instr = document.createElement('div');
    //var id = "strat-instructions";
    //if ($("#tab_strategy_new").length > 0) id = "strat-instructions-2"
    //$(instr).attr("id",id).html(getSimpleInstructionsHtml());
    var instr = getSimpleInstructionsHtml();
    $("#strategy_messages").append(instr);
    $("#strategy_results .resizable-wrapper:has(#Strategies)").hide();
    $("#strategy_results #strategies-panel-toggle").hide();
    $("#strategy_messages").show();
  }

  /* FIXME: probably want to eventually fix this to use 'single-arrow' markup below to
   *   tell users what to do if there are no open strategies AND there is no 'new' tab. */
  function getSimpleInstructionsHtml() {
    // if 'new' tab doesn't exist, then don't display fancy instructions with arrows
    var openTabContents = '<div style="font-size:120%;line-height:1.2em;text-indent:10em;padding:0.5em">' +
        'You have no open strategies.  Please run a search to start a strategy.' +
        '<p style="text-indent:10em">To open an existing strategy, visit the ' +
        '<a href=\"javascript:wdk.addStepPopup.showPanel(\'search_history\')">\'All\' tab</a>.</p></div>';
    return openTabContents;
  }

  /*

  The following two functions don't appear to be used, but keeping around in case we need it. dmf

  function getInstructionsHtml() {
    var arrow_image = "<img id='bs-arrow' alt='Arrow pointing to Browse Strategy Tab' src='" + wdk.assetsUrl('wdk/images/lookUp2.png') + "' width='45px'/>";
    if ($("#tab_strategy_new").length > 0) {
      arrow_image = "<img id='ns-arrow' alt='Arrow pointing to New Search Button' src='" + wdk.assetsUrl('wdk/images/lookUp.png') + "' width='45px'/>" + arrow_image;
    }

    arrow_image += getInstructionsText();
    return arrow_image;
  }

  function getInstructionsText() {
    var instr_text = "<p style='width: 85px; position: absolute; padding-top: 14px;'>Run a new search to start a strategy</p>";
    if ($("#tab_strategy_new").length > 0) {
      instr_text = "<p style='width: 85px; position: absolute; left: 12px; padding-top: 14px;'>Click '<a href=\"javascript:wdk.addStepPopup.showPanel('strategy_new')\">New</a>' to start a strategy</p>";
    }
    var instr_text2 = "<p style='width: 85px; position: absolute; right: 12px; padding-left: 1px;'>Or Click on '<a href=\"javascript:wdk.addStepPopup.showPanel('search_history')\">All</a>' to view your strategies.</p>";
    return instr_text + "<br>" + instr_text2;
  }
  */

  /**
   * Instantiate Strategy object.
   *
   * @param {Object} json Strategy object retreived from server.
   * @param {Number} ord The order in which to display the strategy.
   * @param {Boolean} skipDisplay Only update the model.
   */
  function loadModel(json, ord) {
    wdk.history.update_hist(true); //set update flag for history if anything was opened/changed.
    var strategy = json;
    var strat = null;
    if (!wdk.strategy.model.isLoaded(strategy.id)) {
      strat = new wdk.strategy.model.Strategy(sidIndex, strategy.id, true);
      sidIndex++;
    } else {
      strat = wdk.strategy.model.getStrategyFromBackId(strategy.id);
      strat.subStratOrder = {};
    }
    if (strategy.importId !== "") {
      strat.isDisplay = true;
      strat.checksum = ns.state[ord].checksum;
    } else {
      var prts = strat.backId.split("_");
      strat.subStratOf = wdk.strategy.model.getStrategyFromBackId(prts[0]).frontId;
      if (strategy.order > 0) {
        strat.isDisplay = true;
      }
    }
    strat.JSON = strategy;
    strat.isSaved = strategy.saved;
    strat.isPublic = strategy.isPublic;
    strat.isValid = strategy.isValid;
    strat.name = strategy.name;
    strat.description = strategy.description;
    strat.importId = strategy.importId;
    ns.strats[ord] = strat;
    strat.initSteps(strategy.steps, ord);
    strat.dataType = strategy.steps[strategy.steps.length].dataType;
    strat.displayType = strategy.steps[strategy.steps.length].displayType; //corresponds with record displayName in model e.g. Metabolic Pathway (singular always)
    strat.nonTransformLength = strategy.steps.nonTransformLength;
    // strat.DIV = wdk.strategy.view.displayModel(strat);
    return strat.frontId;
  }

  // Use with caution
  function mergeModel(strategy) {
    var strat = wdk.strategy.model.getStrategyFromBackId(strategy.id);
    strat.JSON = strategy;
    strat.isSaved = strategy.saved;
    strat.isPublic = strategy.isPublic;
    strat.isValid = strategy.isValid;
    strat.name = strategy.name;
    strat.description = strategy.description;
    strat.importId = strategy.importId;
    strat.dataType = strategy.steps[strategy.steps.length].dataType;
    strat.displayType = strategy.steps[strategy.steps.length].displayType; //corresponds with record displayName in model e.g. Metabolic Pathway (singular always)
    strat.nonTransformLength = strategy.steps.nonTransformLength;

    for (var i in strategy.steps) {
      if (i != 'length' && i != 'nonTransformLength') {
        var oldStep = strat.getStep(i, true);
        var newStep = strategy.steps[i];
        oldStep.booleanHasCompleteAnalyses = Boolean(newStep.hasCompleteAnalyses);
        if (oldStep.step) {
          oldStep.hasCompleteAnalyses = Boolean(newStep.step.hasCompleteAnalyses);
        }
      }
    }

    return strat.frontId;
  }

  /**
   * Display results for a particular step.
   *
   * Retrieve results from server as HTML and insert into workspace.
   *
   * @param {Number} f_strategyId Strategy order/frontID
   * @param {Number} f_stepId Step order/frontID
   * @param {Boolean} isBoolean If Step is a Boolean Step set to `true`; else `false`
   * @param {Number} pagerOffset Results offset
   * @param {Boolean} ignoreFilters If `true` don't reload filters; else reload
   *    filters
   * @param {String} action Action to trigger once results are loaded.
   * @param {Object} deferred jQuery.Deffered object created in `updateStrategies`
   */
  function newResults(f_strategyId, f_stepId, isBoolean, pagerOffset, ignoreFilters,
      action, deferred) {

    if (f_strategyId == -1) {
      // don't show any results
      $("#strategy_results > div.Workspace").html("");
      wdk.addStepPopup.current_Front_Strategy_Id = null;
      return;
    }

    wdk.addStepPopup.current_Front_Strategy_Id = f_strategyId;

    var strategy = wdk.strategy.model.getStrategy(f_strategyId);
    var step = strategy.getStep(f_stepId, true);
    var url = "showSummary.do";
    var data = {
      strategy: strategy.backId,
      step: isBoolean ? step.back_boolean_Id : step.back_step_Id,
      resultsOnly: true,
      strategy_checksum: (strategy.checksum !== null) ? strategy.checksum :
          wdk.strategy.model.getStrategy(strategy.subStratOf).checksum
    };

    if (!pagerOffset) {
      data.noskip = 1;
    } else {
      data.pager = { offset: pagerOffset };
    }

    return $.ajax({
      url: url,
      dataType: "html",
      type: "post",
      data: data,
      beforeSend: function() {
        wdk.util.showLoading(f_strategyId);
      },
      success: function(data) {
        var $Strategies = $("#Strategies");
        var oldSelectedStrategyId = $Strategies.find(".diagram").has(".selected").attr("id");
        var oldSelectedStepId = $Strategies.find(".selected").attr("id");

        step.isSelected = true;
        if (wdk.strategy.error.ErrorHandler("Results", data, strategy,
            $("#diagram_" + strategy.frontId + " step_" + step.frontId +
                "_sub div.crumb_details div.crumb_menu a.edit_step_link"))
        ) {
          // unselect previously selected step
          $Strategies.find(".selected").removeClass("selected");

          var init_view_step;

          if (isBoolean) {
            $("#Strategies div#diagram_" + strategy.frontId + " div[id='step_" +
                step.frontId + "']").addClass("selected");
            init_view_step = step.back_step_Id + ".v";
          } else {
            $("#Strategies div#diagram_" + strategy.frontId + " div[id='step_" +
                step.frontId + "_sub']").addClass("selected");
            init_view_step = step.back_step_Id;
          }

          // insert results HTML into DOM
          wdk.resultsPage.resultsToGrid(data, ignoreFilters, $("#strategy_results .Workspace"));
          // update results pane title
          wdk.resultsPage.updateResultLabels($("#strategy_results .Workspace"), strategy, step);

          // remember user's action, if user is not logged in,
          // and tries to save, this place holds the previous
          // action the user was doing.
          var linkToClick = $("a#" + action);
          if (linkToClick.length > 0) {
            linkToClick.click();
          }

          // trigger custom events
          // in this case, select events
          // defer this so the DOM has time to finish initializing
          //
          // Call wdk.load() to avoid a race condition causing some jQuery
          // plugins to not get initialized until after we expect.
          wdk.load();
          var $selectedStrategy = $("#Strategies .diagram").has(".selected");
          var $selectedStep = $("#Strategies .diagram").find(".selected");

          if ($selectedStrategy.attr("id") !== oldSelectedStrategyId) {
            $selectedStrategy.trigger("strategyselect", [strategy]);
            $selectedStep.trigger("stepselect", [step, isBoolean]);
          } else if ($selectedStep.attr("id") !== oldSelectedStepId) {
            $selectedStep.trigger("stepselect", [step, isBoolean]);
          }

          setUIState({
            strategy: strategy,
            step: step
          });
        }

        wdk.util.removeLoading(f_strategyId);
        wdk.basket.checkPageBasket();
        $.cookie("refresh_results", "false", { path : '/' });
      }
    }).then(function() {
      if (deferred) {
        deferred.resolve();
      }
    });
  }

  //===========================================================================
  //  =Actions
  //
  //  The following functions make xhr calls to the server. The server returns
  //  a state object that is used by the function updateStrategies which will
  //  update the local state (strats) and redraw the UI.
  //===========================================================================

  function RenameStep(ele, s, stp) {
    var new_name = $(ele).val();
    var step = wdk.strategy.model.getStep(s, stp);
    var strat = wdk.strategy.model.getStrategy(s);
    var url = "renameStep.do?strategy=" + strat.backId +
        "&stepId=" + step.back_step_Id + "&customName=" +
        encodeURIComponent(new_name);
    $.ajax({
      url: url,
      dataType: "html",
      data: "state=" + encodeURIComponent(ns.stateString),
      beforeSend: function() {
        wdk.util.showLoading(s);
      },
      success: function(data) {
        data = eval("(" + data + ")");
        if (wdk.strategy.error.ErrorHandler("RenameStep", data, wdk.strategy.model.getStrategy(s), null)) {

          // kludge to force a redraw of top level strategy by dirtying the checksum
          // strat.checksum += '_';
          // endkludge

          updateStrategies(data);
        } else {
          wdk.util.removeLoading(s);
        }
      }
    });
  }

  function DeleteStep(f_strategyId,f_stepId) {
    var strategy = wdk.strategy.model.getStrategy(f_strategyId);
    var step = strategy.getStep(f_stepId, true);
    var cs = strategy.checksum;
    var url;

    if (strategy.subStratOf !== null) {
      cs = wdk.strategy.model.getStrategy(strategy.subStratOf).checksum;
    }

    if (step.back_boolean_Id === "") {
      url = "deleteStep.do?strategy=" + strategy.backId + "&step=" +
          step.back_step_Id + "&strategy_checksum=" + cs;
    } else {
      url = "deleteStep.do?strategy=" + strategy.backId + "&step=" +
          step.back_boolean_Id + "&strategy_checksum=" + cs;
    }

    $.ajax({
      url: url,
      type: "post",
      dataType: "json",
      data: "state=" + encodeURIComponent(ns.stateString),
      beforeSend: function() {
        wdk.util.showLoading(f_strategyId);
      },
      success: function(data) {
        if (wdk.strategy.error.ErrorHandler("DeleteStep", data, strategy, null)) {
          updateStrategies(data);
        } else {
          wdk.util.removeLoading(strategy.frontId);
        }
      }
    });
  }

  function ExpandStep(e, f_strategyId, f_stepId, collapsedName, uncollapse) {
    var strategy = wdk.strategy.model.getStrategy(f_strategyId);
    var step = strategy.getStep(f_stepId, true);
    var cs = strategy.checksum;
    if (strategy.subStratOf !== null) {
      cs = wdk.strategy.model.getStrategy(strategy.subStratOf).checksum;
    }
    var url = "expandStep.do?strategy=" + strategy.backId + "&step=" +
        step.back_step_Id + "&collapsedName=" + collapsedName +
        "&strategy_checksum=" + cs;
    if (uncollapse) url += "&uncollapse=true";

    $.ajax({
      url: url,
      type: "post",
      dataType: "json",
      data: "state=" + encodeURIComponent(ns.stateString),
      beforeSend: function() {
        wdk.util.showLoading(f_strategyId);
      },
      success: function(data) {
        if (wdk.strategy.error.ErrorHandler("EditStep", data, strategy, $("div#query_form"))) {

          // kludge to force a redraw of top level strategy by dirtying the checksum
          // var topStrategy = (strategy.subStratOf !== null) ?
          //     wdk.strategy.model.getStrategy(strategy.subStratOf) :
          //     strategy;
          // topStrategy.checksum += '_';
          // endkludge

          updateStrategies(data);
        } else {
          wdk.util.removeLoading(f_strategyId);
        }
      }
    });
  }

  function openStrategy(stratId){
    var url = "showStrategy.do?strategy=" + stratId;
    var encodedStateString = encodeURIComponent(ns.stateString);
    $.ajax({
      url: url,
      dataType: "json",
      data: "state=" + encodeURIComponent(ns.stateString),
      beforeSend: function() {
        $("body").block();
      },
      success: function(data) {
        $("body").unblock();
        if (wdk.strategy.error.ErrorHandler("Open", data, null, null)) {
          updateStrategies(data);
          if ($("#strategy_results").css('display') == 'none') {
            wdk.addStepPopup.showPanel('strategy_results');
          }
        }
      }
    });
  }

  function deleteStrategy(stratId, fromHist) {
    var url = "deleteStrategy.do?strategy=" + stratId;
    var stratName;
    var strat;
    var message = "If you shared a strategy, its URL stays valid even if you " +
        "delete the strategy.======== Are you sure you want to " +
        "delete the strategy '";

    if (fromHist) {
      stratName = $.trim($("div#text_" + stratId).text());
    } else {
      strat = wdk.strategy.model.getStrategyFromBackId(stratId);
      stratName = strat.name;

      if (strat.subStratOf !== null) {
        var parent = wdk.strategy.model.getStrategy(strat.subStratOf);
        var cs = parent.checksum;
        url = "deleteStep.do?strategy=" + strat.backId + "&step=" +
            stratId.split('_')[1] + "&strategy_checksum=" + cs;
        message = "Are you sure you want to delete the substrategy '";
        stratName = strat.name + "' from the strategy '" + parent.name;
      }
    }
    message = message + stratName + "'?";
    var agree = confirm(message);
    if (agree) {
      $.ajax({
        url: url,
        dataType: "json",
        data: "state=" + encodeURIComponent(ns.stateString),
        beforeSend: function() {
          if (!fromHist) wdk.util.showLoading(stratId);
        },
        success: function(data) {
          if (wdk.strategy.error.ErrorHandler("DeleteStrategy", data, null, null)) {
            updateStrategies(data);
            wdk.history.update_hist(true);
            if ($('#search_history').css('display') != 'none') {
              wdk.history.updateHistory();
            }
          }
        }
      });
    }
  }

  function closeStrategy(stratId, isBackId) {
    var strat = wdk.strategy.model.getStrategy(stratId);
    if (isBackId) {
      strat = wdk.strategy.model.getStrategyFromBackId(stratId);
      stratId = strat.frontId;
    }
    var cs = strat.checksum;
    if (strat.subStratOf !== null) {
      cs = wdk.strategy.model.getStrategy(strat.subStratOf).checksum;
    }
    var url = "closeStrategy.do?strategy=" + strat.backId +
        "&strategy_checksum=" + cs;
    $.ajax({
      url: url,
      dataType: "json",
      data: "state=" + encodeURIComponent(ns.stateString),
      beforeSend: function() {
        wdk.util.showLoading(stratId);
      },
      success: function(data) {
        if (wdk.strategy.error.ErrorHandler("CloseStrategy", data, strat, null)) {

          // kludge to force a redraw of top level strategy by dirtying the checksum
          // var topStrategy = (strat.subStratOf !== null) ?
          //     wdk.strategy.model.getStrategy(strat.subStratOf) :
          //     strat;
          // topStrategy.checksum += '_';
          // endkludge

          updateStrategies(data);
          if ($('#search_history').css('display') != 'none') {
            wdk.history.update_hist(true);
            wdk.history.updateHistory();
          }
        }
      }
    });
  }

  function copyStrategy(stratId, fromHist) {
    wdk.strategy.model.getStrategyOBJ(stratId)
      .then(function(ss) {
        var result = confirm("Do you want to make a copy of strategy '" +
            ss.name + "'?");
        if (result === false) return;
        var url = "copyStrategy.do?strategy=" + stratId + "&strategy_checksum=" +
            ss.checksum;
        $.ajax({
          url: url,
          dataType: "json",
          data: "state=" + encodeURIComponent(ns.stateString),
          beforeSend: function() {
            if (!fromHist) {
              wdk.util.showLoading(ss.frontId);
            }
          },
          success: function(data) {
            if (wdk.strategy.error.ErrorHandler("Copystrategy", data, ss, null)) {
              updateStrategies(data);
              if (fromHist) {
                wdk.history.update_hist(true);
                wdk.history.updateHistory();
              }
              wdk.util.removeLoading(ss.frontId);
            }
          }
        });
      });
  }

  function saveOrRenameStrategy(strategy, checkName, save, fromHist, form) {
    return $.ajax({
      url: "renameStrategy.do",
      type: "POST",
      dataType: "json",
      data: {
        strategy: strategy.backId,
        name: strategy.name,
        description: strategy.description,
        isPublic: strategy.isPublic,
        checkName: checkName,
        save: save,
        strategy_checksum: (strategy.subStratOf !== null) ?
            wdk.strategy.model.getStrategy(strategy.subStratOf).checksum :
            strategy.checksum,
        showHistory: fromHist,
        state: encodeURIComponent(ns.stateString)
      },
      beforeSend: function() {
        if (!fromHist) {
          wdk.util.showLoading(strategy.frontId);
        }
      },
      success: function(data) {
        var type = save ? "SaveStrategy" : "RenameStrategy";
        if (wdk.strategy.error.ErrorHandler(type, data, strategy, form, strategy.name, fromHist)) {
          updateStrategies(data);
          if (fromHist) {
            wdk.history.update_hist(true);
            wdk.history.updateHistory();
          }
        }
        if(!fromHist) {
          wdk.util.removeLoading(strategy.frontId);
        }
      }
    });
  }

  function ChangeFilter(strategyId, stepId, url, filter) {
    var filterElt = filter;
    var strategy = uiState.strategy;
    var step = uiState.step;

    var f_strategyId = strategy.frontId;
    // This is a no-op. Not sure what the intention is. - dmf
    // if (strategy.subStratOf !== null) {
    //   ns.strats.splice(wdk.strategy.model.findStrategy(f_strategyId));
    // }
    var cs = strategy.checksum;
    if (strategy.subStratOf !== null) {
      cs = wdk.strategy.model.getStrategy(strategy.subStratOf).checksum;
    }
    url += "&strategy_checksum="+cs;

    var doUpdate = $.ajax.bind($, {
      url: url,
      type: "GET",
      dataType: "json",
      data: "state=" + encodeURIComponent(ns.stateString),
      beforeSend: function() {
        $("#strategy_results > div.Workspace").block();
        wdk.util.showLoading(f_strategyId);
        wdk.addStepPopup.disableAddStepButtons();
      },
      success: function(data) {
        if (wdk.strategy.error.ErrorHandler("ChangeFilter", data, strategy, null)) {
          updateStrategies(data, true);
          $("div.layout-detail td div.filter-instance div.current")
              .removeClass('current');
          $(filterElt).parent('div').addClass('current');
          wdk.addStepPopup.enableAddStepButtons();
        }
      }
    });

    var showAnalysisWarning = hasDownstreamCompleteAnalyses(step, strategy,
        step.back_boolean_Id === Number(stepId));

    if (showAnalysisWarning) {
      $('<div style="font-size: 120%;">' +
          '<h3 style="margin:0;padding:0">Warning</h3>' +
          '<p><img width="20" alt="filtering icon" src="' +
         wdk.assetsUrl('wdk/images/filter-short.png') + '"/>' +
        ' Clicking this will change the gene ' +
        ' results that were used to generate your analyses.' +
         ' Analysis results for this and subsequent strategy steps will be lost.' +
          '&nbsp;  <a style="font-size:80%" href="' + wdk.webappUrl('/analysisTools.jsp') + '" target="_blank">(Learn more...)</a></p>' +
        '</div>')
        .dialog({
          modal: true,
          dialogClass: 'no-close',
          width: '400px',
          buttons: [{
            autofocus: true,
            text: 'Proceed anyway',
            click: function() {
              doUpdate();
              $(this).dialog('close');
            }
          }, {
            text: 'Cancel',
            click: function() {
              $(this).dialog('close');
            }
          }]
        });
    } else {
      doUpdate();
    }
  }

  // Check if any downstream steps have complete analyses
  //
  // Given strategy X (where the bottom row is combined steps):
  //
  //     B   D   E
  //     |   |   |
  // A - C - E - G
  //
  //
  // * For step C, determine if steps C, E, or G have a complete analysis
  // * For step D, determine if steps D, E, or G have a complete analysis
  //
  // NB: We only need to check combined steps to the right.
  //
  // Returns Boolean
  function hasDownstreamCompleteAnalyses(step, strategy, isBoolean) {
    var ret = isBoolean
      ? step.booleanHasCompleteAnalyses
      : step.hasCompleteAnalyses || step.booleanHasCompleteAnalyses;

    if (strategy.subStratOf !== null) {
      var parentStratPieces = strategy.backId.split('_');
      var parentStrategy = wdk.strategy.model.getStrategyFromBackId(parentStratPieces[0]);
      var parentStep = wdk.strategy.model.getStepFromBackId(parentStratPieces[0], parentStratPieces[1]);
      ret = ret || hasDownstreamCompleteAnalyses(parentStep, parentStrategy, false);
    }

    return ret || _(strategy.Steps.slice(strategy.Steps.indexOf(step) + 1))
      .some(function(downstreamStep) {
        return downstreamStep.booleanHasCompleteAnalyses;
      });
  }

  function SetWeight(e, f_strategyId, f_stepId) {
    var strategy = wdk.strategy.model.getStrategy(f_strategyId);
    var step = strategy.getStep(f_stepId, true);
    var cs = strategy.checksum;
    var weight = $(e).siblings("input#weight").val();
    if (weight === undefined) {
      weight = $(e).siblings().find("input[name='weight']").val();
    }
    if(strategy.subStratOf !== null) {
      cs = wdk.strategy.model.getStrategy(strategy.subStratOf).checksum;
    }
    var url = "processFilter.do?strategy=" + strategy.backId +
        "&revise=" + step.back_step_Id + "&weight=" + weight +
        "&strategy_checksum=" + cs;

    $.ajax({
      url: url,
      type: "post",
      dataType: "json",
      data: "state=" + encodeURIComponent(ns.stateString),
      beforeSend: function() {
        wdk.util.showLoading(f_strategyId);
      },
      success: function(data) {
        if (wdk.strategy.error.ErrorHandler("SetWeight", data, strategy, null)) {
          updateStrategies(data);
        } else {
          wdk.util.removeLoading(f_strategyId);
        }
      }
    });
  }

  //===========================================================================
  //  =End Actions
  //===========================================================================

  // editable plugin event handler
  function updateStrategyName(widget) {
    var strategyId = widget.element.data("id");
    var strategy = wdk.strategy.model.getStrategyFromBackId(strategyId);
    var oldName = strategy.name;
    var deferred = $.Deferred();

    strategy.name = widget.value;

    wdk.util.showLoading(strategy.frontId);

    strategy.update().success(function(data) {
      if (wdk.strategy.error.ErrorHandler("RenameStrategy", data, strategy, null, strategy.name, null)) {
        updateStrategies(data);
        $(".strategy-name[data-id='" + strategyId + "']").text(strategy.name);
        deferred.resolve();
      } else {
        strategy.name = oldName;
        deferred.reject();
      }
      wdk.util.removeLoading(strategy.frontId);
    });
    return deferred;
  }

  ns.init = init;
  //ns.AddStepToStrategy = AddStepToStrategy;
  ns.ChangeFilter = ChangeFilter;
  ns.DeleteStep = DeleteStep;
  //ns.EditStep = EditStep;
  ns.ExpandStep = ExpandStep;
  ns.newResults = newResults;
  ns.RenameStep = RenameStep;
  ns.SetWeight = SetWeight;
  ns.closeStrategy = closeStrategy;
  ns.copyStrategy = copyStrategy;
  ns.deleteStrategy = deleteStrategy;
  ns.initDisplay = initDisplay;
  ns.fetchStrategies = fetchStrategies;
  ns.loadModel = loadModel;
  ns.openStrategy = openStrategy;
  ns.saveOrRenameStrategy = saveOrRenameStrategy;
  //ns.setStrategyStatusCounts = setStrategyStatusCounts;
  ns.showStrategies = showStrategies;
  ns.updateStrategies = updateStrategies;
  ns.updateStrategyName = updateStrategyName;
});
