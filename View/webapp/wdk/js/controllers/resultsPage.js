/*
WDK Strategy System
resultsPage.js

Provides functions to support results table
*/

import _ from 'lodash';
import Url from 'url';

wdk.namespace("window.wdk.resultsPage", function(ns, $) {

  "use strict";

  // Called when a step is selected and the tabs container is inserted in DOM
  function configureSummaryViews($element) {
    var addFeatureTooltipOnce = _.once(addFeatureTooltip); // only call once per step selection
    var currentTab = parseInt($element.children("ul").attr("currentTab"), 10);
    // var currentTab = 0;
    addAddAttributesListeners();

    $element.tabs({
      active : currentTab,
      activate: function(event, ui) {
        // save summary view preference
        var summaryViewName = ui.newTab.attr('id');
        var questionName = $element.attr('question');
        wdk.getWdkService().updateCurrentUserPreference("project","summary_view_" + questionName, summaryViewName)
          .catch(error => console.error(error));
      },
      load: function(event, ui) {
        addFeatureTooltipOnce($element);
        createFlexigridFromTable(ui.panel.find(".Results_Table"));
        wdk.basket.checkPageBasket();
        wdk.util.setDraggable(ui.panel.find("div.attributesList"), ".dragHandle");
        ui.panel.trigger('wdk-results-loaded');
      }
    });

    // if not a child of basket menu, configure analysis tabs
    if ($element.has('#add-analysis').length) {
      wdk.stepAnalysis.configureAnalysisViews($element);

      // check URL for query param indicating a preferred tab
      var preferredTabVals = wdk.util.parseUrlUtil("selectedTab", window.location);
      var preferredTab = preferredTabVals == "" ? undefined : preferredTabVals[0];
      if (preferredTab) {
        var tabIndex = $element.children("ul").children("li").get()
          .map(function(tab){ return tab.id; })
          .filter(function(id){ return id != "choose-step-analysis" && id != "add-analysis"; })
          .findIndex(function(id){ return id === preferredTab; });
        if (tabIndex != -1) {
          $('#Summary_Views').tabs('option', 'active', tabIndex);
        }
        window.history.replaceState("", null, location.pathname);
      }
    }

  }

  // Add a feature tooltip to page for new analysis tools
  //
  // Business rules are:
  // 1. Only create tooltip if .analysis-feature-tooltip is present
  // 2. Remove previously created feature tooltips created for analysis tools
  function addFeatureTooltip($element) {
    var createFeatureTooltip = wdk.components.createFeatureTooltip;
    var analysisFeatureTooltipTarget = $element.find('#add-analysis')
      .has('.analysis-feature-tooltip');
    var previousTooltip = $('.wdk-feature-tooltip').has('.analysis-feature-tooltip');

    if (analysisFeatureTooltipTarget.length) {
      createFeatureTooltip({
        el: analysisFeatureTooltipTarget,
        key: 'new-analysis::' + wdk.VERSION,
        title: 'New tools available!',
        text: analysisFeatureTooltipTarget.find('.analysis-feature-tooltip'),
        container: $element.parent()
      });

      analysisFeatureTooltipTarget
        .on('click', function() {
          analysisFeatureTooltipTarget.qtip('hide');
        });
    }

    if (previousTooltip.length) {
      var qtip = previousTooltip.data('qtip');
      _.defer(qtip.destroy.bind(qtip), true);
    }
  }

  function moveAttr(col_ix, table) {
    // Get name of target attribute & attribute to left (if any)
    // NOTE:  Have to convert these from frontId to backId!!!
    var headers = $("tr.headerrow th", table);
    var attr = $(headers[col_ix]).attr("id");
    var left,
        strat,
        step;
    if (col_ix > 0) left = $(headers[col_ix-1]).attr("id");
    // Figure out what step/strategy is currently displayed in results panel
    if ($(table).parents("#strategy_results").length > 0) {
      step = $("div.selected");
      var stepfId = step.attr("id").split('_')[1];
      var stratfId = step.parents(".diagram").attr("id");
      stratfId = stratfId.substring(stratfId.indexOf('_') + 1);
      strat = wdk.strategy.model.getStrategy(stratfId).backId;
      step = step.hasClass('operation')
        ? wdk.strategy.model.getStep(stratfId, stepfId).back_boolean_Id
        : wdk.strategy.model.getStep(stratfId, stepfId).back_step_Id;
    } else {
      step = $(table).attr('step');
    }
    // build url.
    var info = $("#Summary_Views");
    var view = $("#Summary_Views > ul > li.ui-tabs-active").attr("id");
    var url = info.attr("updateUrl") +
        "?strategy=" + strat + "&step=" + step + "&command=arrange" +
        "&attribute=" + attr + "&left=" + left + "&view=" + view;
    updateResultsPage($(table), url, false);
  }

  function updateAttrs($form) {
    var commandUrl = $form.attr('action');
    var command = $form.find('[name=command]').val();
    var attributes = $form.find("input:checked").toArray().map(el => el.value);
    var url = commandUrl + "&command=" + command + "&attribute=" + attributes.join("&attribute=");

    // close the dialog
    $form.parents(".attributesList").dialog("close");

    updateResultsPage($form, url, true);
  }

  function resetAttr(url, button) {
    if (confirm("Are you sure you want to reset the column configuration back to the default?")) {
      url += "&command=reset";
      updateResultsPage($(button), url, true);
    }
  }

  function updateResultsPage(element, url, update) {
    // determine whether to refresh strategy result or basket result
    var tab = $("#strategy_tabs > li#selected > a").attr("id");
    if (tab == "tab_strategy_results") {
      getResultsPage(url, update, true);
    } else if (tab == "tab_basket") {
      wdk.basket.ChangeBasket(url + "&results_only=true");
    }
  }

  function sortResult(attribute, order) {
    var command = "command=sort&attribute=" + attribute + "&sortOrder=" + order;
    updateSummary(command);
  }

  function removeAttribute(attribute) {
    var command = "command=remove&attribute=" + attribute;
    updateSummary(command);
  }

  function updateSummary(command) {
    // var info = workspace("#Summary_Views");
    var info = wdk.findActiveWorkspace().find("#Summary_Views");
    var url = info.attr("updateUrl");
    var strategyId = info.attr("strategy");
    var stepId = info.attr("step");
    var strategy = wdk.strategy.model.getStrategyFromBackId(strategyId);
    var view = $("#Summary_Views > ul > li.ui-tabs-active").attr("id");
    url += "?strategy=" + strategyId + "&strategy_checksum=" +
        strategy.checksum + "&step=" + stepId + "&view=" + view + "&" + command;
    getResultsPage(url, true, true, true);
  }

  function getResultsPage(url, update, ignoreFilters, resultOnly) {
    var s = wdk.util.parseUrlUtil("strategy", url);
    var st = wdk.util.parseUrlUtil("step", url);
    var strat = wdk.strategy.model.getStrategyFromBackId(s[0]);
    var currentDiv = window.wdk.findActiveView();
    var step = null;
    if (!strat) {
      strat = {};
      strat.JSON = {};
      step = {};
      strat.JSON.name = "";
      step.frontId = "n/a";
    } else {
      step = strat.getStep(st[0], false);
    }
    url = url + "&resultsOnly=true";
    currentDiv.find("div.attributesList").hide();
    var parsedUrl = Url.parse(url);
    if (update) currentDiv.block();
    $.ajax({
      url: parsedUrl.pathname,
      method: 'post',
      data: parsedUrl.search.slice(1),
      dataType: "html",
      beforeSend: function() {
        if(strat) wdk.util.showLoading(strat.frontId);
      },
      success: function(data) {
        if (update && wdk.strategy.error.ErrorHandler("Results", data, strat, null)) {
          if (resultOnly === undefined) {
            resultOnly = (url.indexOf('showResult.do') >= 0);
          }
          resultsToGrid(data, ignoreFilters, currentDiv, resultOnly);
          updateResultLabels(currentDiv, strat, step);
          currentDiv.trigger('wdk-results-loaded');
        }
        if(strat) wdk.util.removeLoading(strat.frontId);
      },
      error : function(data, msg, e) {
        alert("ERROR \n "+ msg + "\n" + e + ". \n" +
            "Reloading this page might solve the problem. \n" +
            "Otherwise, please contact site support.");
      }
    });
  }

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

    // convert results table to drag-and-drop flex grid
    createFlexigridFromTable(currentDiv.find(" .Results_Table"));  // moved to tab load success callback

    // check the basket for the page if needed
    wdk.basket.checkPageBasket();

    wdk.util.setDraggable(currentDiv.find("div.attributesList"), ".dragHandle");

    currentDiv.unblock();
  }

  function createFlexigridFromTable(table) {
    table.flexigrid({
      height : 'auto',
      showToggleBtn : false,
      useRp : false,
      singleSelect : true,
      onMoveColumn : moveAttr,
      nowrap : false,
      resizable : false
    });
  }

  /**
   * Add event handlers for Add attributes form. This uses event delegation so
   * that we can handle multiple such forms on a page. An event namespace is
   * used so that prevent handlers can be removed (via .off).
   *
   * @param {jQuery} $container Root results container
   */
  function addAddAttributesListeners() {
    var eventNamespace = '.addAttributes';
    var childSelector = 'form[name="addAttributes"]';
    $(document.body)
      .off(eventNamespace)
      .on('submit' + eventNamespace, childSelector, function(e) {
        e.preventDefault();
        updateAttrs($(e.target));
      })
      .on('change' + eventNamespace, childSelector, function(e, reason) {
        // Update the command element: if reason is default, then we want to
        // reset everything in the table.
        $(e.target).find('[name=command]')
          .val(reason === 'default' ? 'reset' : 'update');
      });
  }

  function updatePageCount(element) {
    var advancedPaging = $(element).parent();
    var resultSize = parseInt($("input.resultSize",advancedPaging).val(), 10);
    var pageSize = $(".pageSize",advancedPaging).val();
    var pageCount = Math.ceil(resultSize / pageSize);
    if (pageCount * pageSize < resultSize) pageCount++;
    var span = $(".pageCount",advancedPaging);
    span.html(pageCount);
  }

  function gotoPage(element) {
    var advancedPaging = $(element).parent();
    var pageNumber = $(".pageNumber",advancedPaging).val();
    var pageSize = $(".pageSize",advancedPaging).val();

    var pageUrl = $(".pageUrl", advancedPaging).val();

    var pageOffset = (pageNumber - 1) * pageSize;
    var gotoPageUrl = pageUrl.replace(/\&pager\.offset=\d+/, "");
    gotoPageUrl = gotoPageUrl.replace(/\&pageSize=\d+/, "");
    gotoPageUrl += "&pager.offset=" + pageOffset;
    gotoPageUrl += "&pageSize=" + pageSize;
    $("div.advanced-paging").hide();
    getResultsPage(gotoPageUrl, true, true);
  }

  function openAttributeList(element, viewName){
    var dialogId = getDialogId(element, "attributesList");
    var $dialog = $("#" + dialogId);
    // first hack for apicommon (overridden addAttributes.tag), see second hack below
    if ($dialog.find('[data-controller="eupathdb.attributeCheckboxTree.setUpCheckboxTree"]').length > 0) {
      eupathdb.attributeCheckboxTree.mountCheckboxTree(viewName);
    }
    openBlockingDialog("#" + dialogId);
    $dialog
      .dialog({
        // Removing re-sizing of Add Columns dialog because jQuery UI resizing
        // is broken - crisl 23FEB2016
        resizable: false,
        width: 'auto',
        maxWidth: '75vw'
      })
      .one({
        // Very ugly kludge to reset checkbox tree to default
        // values when dialog is closed.
        dialogclose(e) {
          var cbt = $(e.target).find('[data-controller="wdk.checkboxTree.setUpCheckboxTree"]');
          if (cbt.length > 0) {
            var cbtId = cbt.data('id');
            wdk.checkboxTree.selectCurrentNodes(cbtId);
          }
          // second hack for apicommon (overridden addAttributes.tag), see first hack below
          cbt = $(e.target).find('[data-controller="eupathdb.attributeCheckboxTree.setUpCheckboxTree"]');
          if (cbt.length > 0) {
            eupathdb.attributeCheckboxTree.unmountCheckboxTree(viewName);
          }
        }
      });
  }

  function openAdvancedPaging(element){
    openBlockingDialog("#" + getDialogId(element, "advanced-paging"));
  }

  function closeAdvancedPaging(submitObj) {
    $(submitObj).parents(".advanced-paging").dialog("close");
  }

  function openBlockingDialog(selector) {
    $(selector).dialog({ modal: true });
    $(selector).dialog("open");
  }

  function getDialogId(element, dialogClass) {
    var containingTabId = $(element).closest(".ui-tabs-panel").attr("id");
    var list = $(element).next("." + dialogClass);
    if (list.length > 0) {
      var id = "dialog" + Math.floor(Math.random() * 1000000000);
      $(element).attr("dialog", id);
      list.attr("id", id).dialog({
        // appendTo: "#" + containingTabId,
        autoOpen: false,
        open: function() {
          var $this = $(this);
          var dialogHeight = $this.height();
          var viewportHeight = $(window).height();
          $this.dialog("option", "height",
              dialogHeight > viewportHeight ? viewportHeight - 36 : "auto");
          $this.dialog("option", "width", list.outerWidth() + 20);
        }
      });
    }
    return $(element).attr("dialog");
  }

  function invokeAttributePlugin(ele, stepId, attributeName) {
    var pluginName = $(ele).attr("plugin");
    var title = $(ele).attr("plugintitle");
    var url = "invokeAttributePlugin.do?step=" + stepId +
        "&attribute=" + attributeName + "&plugin=" + pluginName;
    $.ajax({
      url: url,
      dataType: "html",
      beforeSend: function() {
        $.blockUI();
      },
      success: function(data) {
        $.unblockUI();
        // create a place holder for the result
        if ($("#attribute-plugin-result").length === 0) {
          $("body").append("<div id=\"attribute-plugin-result\"> </div>");
        }
        $("#attribute-plugin-result")
          .html(data)
          .dialog({
            width : 825,
            maxHeight: 800,
            title : title,
            modal : true
          });
      }
    });
  }

  ns.getResultsPage = getResultsPage;
  ns.resultsToGrid = resultsToGrid;
  ns.closeAdvancedPaging = closeAdvancedPaging;
  ns.configureSummaryViews = configureSummaryViews;
  ns.createFlexigridFromTable = createFlexigridFromTable;
  ns.gotoPage = gotoPage;
  ns.invokeAttributePlugin = invokeAttributePlugin;
  ns.openAdvancedPaging = openAdvancedPaging;
  ns.openAttributeList = openAttributeList;
  ns.removeAttribute = removeAttribute;
  ns.resetAttr = resetAttr;
  ns.sortResult = sortResult;
  ns.updatePageCount = updatePageCount;
  ns.updateResultLabels = updateResultLabels;

});
