/* global wdk, customBasketPage */
wdk.namespace("window.wdk.basket", function(ns, $) {
  "use strict";

  function configureBasket() {
    // determine the default tab
    var current = wdk.stratTabCookie.getCurrentTabCookie('basket');
    var tab = $("#basket-menu > ul > li#" + current);
    var index = (tab.length > 0) ? tab.index() : 0;
    
    $("#basket-menu").tabs({
      active: index,
      load: function(event, ui) {
        basketTabSelected(event, ui);
      }
    });
  }
  
  function basketTabSelected(event, ui) {
    var currentTab = getCurrentBasketTab();
    var workspace = window.wdk.findActiveWorkspace();
    workspace.prepend($("#basket-control-panel #basket-control").clone());
    // store the selection cookie
    var currentId = currentTab.attr("id");
    wdk.stratTabCookie.setCurrentTabCookie('basket', currentId);
    var currentDiv = $(ui.panel);
    var control = workspace.children("#basket-control");
    
    if (currentDiv.find("table").length > 0) {
      control.find("input#empty-basket-button").attr("disabled",false);
      control.find("input#make-strategy-from-basket-button").attr("disabled",false);
      control.find("input#export-basket-button").attr("disabled",false);
      checkPageBasket();
      try {
        customBasketPage();
      } catch(err) {
        //Do nothing
      }
    } else {
      control.find("input#empty-basket-button").attr("disabled",true);
      control.find("input#make-strategy-from-basket-button").attr("disabled",true);
      control.find("input#export-basket-button").attr("disabled",true);
    }
  }
  
  function showBasket() {
    var url = "showBasket.do";
    var d = {};
    
    $.ajax({
      url: url,
      data: d,
      type: "post",
      dataType: "html",
      beforeSend: function(){
        $("body").block();
      },
      success: function(data){
        $("div#basket").html(data);
        $("body").unblock();
      },
      error: function(){
        alert("Error occured in showBasket() function!!");
        $("body").unblock();
      }
    });
  }
  
  function refreshBasket() {
    var tabs = $("#basket #basket-menu");
    var index = tabs.tabs("option", "active");
    tabs.tabs("load", index, { skipCache: true });
  }
  
  function ChangeBasket(url, noUpdate) {
    $("body").block();
    $.ajax({
      url: url,
      dataType: "html",
      success: function(){
        $("body").unblock();  //Gets blocked again by the next line anyway
          
        if (!noUpdate) { // For things like moving columns, don't need to refresh
          showBasket();
        }
      },
      error : function(data, msg, e){
        alert("ERROR \n "+ msg + "\n" + e + ". \n" +
          "Reloading this page might solve the problem. \n" +
          "Otherwise, please contact site support.");
      }
    });
  }
  
  function emptyBasket() {
    var currentTab = getCurrentBasketTab();
    var recordClass = currentTab.data("recordclass");
    var display = currentTab.text();
    var message = $("#basketConfirmation");
    $("#basketName", message).text(display);
    $("form", message).submit(function() {
      updateBasket(this,'clear',0,0,recordClass);
    });    
    $.blockUI({message : message});
  }
  
  function saveBasket() {
    var recordClass = getCurrentBasketTab().data("recordclass");
    recordClass = recordClass.replace('.', '_');  
    window.location='processQuestion.do?questionFullName=InternalQuestions.' +
      recordClass + 'BySnapshotBasket&' + recordClass +
      'Dataset_type=basket&questionSubmit=Run+Step';
  }
  
  //Shopping basket on clickFunction
  function updateBasket(ele, type, pk, pid, recordType) {
    
    var i = $(ele);
    if (ele.tagName != "IMG") {
      i = $("img",ele);
    }
    
    //several rows might need update (in this and other views): the matching <img> are kept in elemArray
    var basketId = wdk.util.escapeSelectorComponent("basket" + pk);
    var elemArray = jQuery("a[id=" + basketId + "] img");
    
    // show processing icon, will remove it when the process is completed.
    var oldImage = i.attr("src");
    i.attr("src",wdk.assetsUrl("wdk/images/loading.gif"));
    
    elemArray.each(function(){
      $(this).attr("src",wdk.assetsUrl("wdk/images/loading.gif"));
    });
    
    // generate....
    var a = [];
    var action = null;
    var da = null;
    var currentDiv;
    
    if (type != 'recordPage') {
      // results view we are in
      currentDiv = getCurrentBasketRegion();
    }
    
    var o = {};
    var pkDiv;
    if (type == "recordPage") {
      pkDiv = $(i).parents(".wdk-record").find("span.primaryKey");
      $("span", pkDiv).each(function() {
        o[$(this).attr("key")] = $(this).text();
      });
      a.push(o);
      da = JSON.stringify(a);
      action = (i.attr("value") == '0') ? "add" : "remove";
      
    } else if (type == "single") {
      pkDiv = $(ele).parents("tr").find("div.primaryKey");
      $("span", pkDiv).each(function(){
        o[$(this).attr("key")] = $(this).text();
      });
      a.push(o);
      da = JSON.stringify(a);
      //console.log(da);  contains the data (all the fields in prim key)
      action = (i.attr("value") == '0') ? "add" : "remove";
      
    } else if (type == "page") {
      currentDiv.find(".Results_Div div.primaryKey").each(function(){
        var o =  { };
        $("span",this).each(function(){
          o[$(this).attr("key")] = $(this).text();
        });
        a.push(o);
      });
      action = (i.attr("value") == '0') ? "add" : "remove";
      da = JSON.stringify(a);
    } else if (type == "clear") {
      action = "clear";
    } else {
      da = type;
      action = "add-all";//(i.attr("value") == '0') ? "add-all" : "remove-all";
    }
    
    var d = "action="+action+"&type="+recordType+"&data="+da;
    console.log(d);
    // action=remove&
    // type=TranscriptRecordClasses.TranscriptRecordClass&
    // data=[{"gene_source_id":"PF3D7_0313700","source_id":"PF3D7_0313700.1","project_id":"PlasmoDB"}]
    
    $.ajax({
      url: "processBasket.do",
      type: "post",
      data: d,
      dataType: "json",
      beforeSend: function() {
      if (action == 'add-all' || type == 'page') {
        currentDiv.block();
      }
      //$("body").block();
      },
      success: function(data) {
        //$("body").unblock();
        if (action == 'add-all' || type == 'page') {
          currentDiv.unblock();
        }
          
        if (type == "single" || type == "recordPage") {
          if (action == "add") {
            elemArray.each(function(){
              $( this ).attr("src",wdk.assetsUrl("wdk/images/basket_color.png"));
              $( this ).attr("value", "1");
              $( this ).attr("title","Click to remove this item from the basket.");
            });
          } else {
            elemArray.each(function(){
              $( this ).attr("src",wdk.assetsUrl("wdk/images/basket_gray.png"));
              $( this ).attr("value", "0");
              $( this ).attr("title","Click to add this item to the basket.");
            });
          }
            
          if (type == "recordPage") {
            if(action == "add")
              i.parent().prev().html("Remove from Basket");
            else
              i.parent().prev().html("Add to Basket");
            }
            // the image has been updated, no need to restore it again.
            oldImage = null;
            
          } else if(type == "clear") {
            showBasket();
            
          } else { //which case is this?
            var image = currentDiv.find(".Results_Div img.basket");
            if (action == "add-all" || action == "add") {
              image.attr("src",wdk.assetsUrl("wdk/images/basket_color.png"));
              image.attr("title","Click to remove this item from the basket.");
              image.attr("value", "1");
            } else {
              image.attr("src",wdk.assetsUrl("wdk/images/basket_gray.png"));
              image.attr("title","Click to add this item to the basket.");
              image.attr("value", "0");
            }
          }
          
          updateBasketCount(data, recordType);
          if (type != 'recordPage') {
            checkPageBasket();
            // the image has been updated, no need to restore it again.
            oldImage = null;
          }
          
          var section = $("#strategy_tabs > #selected > a").attr("id");
          
          if (section == "tab_basket") {
            //Using cookie to determine that the results need to be updated when the 'Opened' tab is selected
            $.cookie("refresh_results", "true", { path : '/' });
          }
          
          if (oldImage) {
            //sth went wrong
            i.attr("src", oldImage);
          }
        },
          error: function(){
          //$("body").unblock();
          alert("Error adding item to basket!");
          i.attr("src", oldImage);
        }
      });
  }
  
  function updateBasketCount(counts, recordType) {
    // menu bar
    $("#menu a#mybasket span.subscriptCount").text("(" + counts.all + ")");
    
    // basket tab
    var $basketTabs = $('#basket-menu');
    var $basketTab = $basketTabs.find('> .ui-tabs-nav ' +
                                      '> li[data-recordclass="' + recordType + '"]');
    var $basketPanel = $basketTabs.find('.ui-tabs-panel').eq($basketTab.index());
    
    var recordTypeCount = counts.records[recordType];
    
    $basketTab.find('.count').text(recordTypeCount);
    $basketPanel.find('#text_step_count').text(recordTypeCount);
    $basketPanel.find('.record-count').text(recordTypeCount);
  }
  
  function checkPageBasket() {
    var currentDiv = getCurrentBasketRegion();
    var headImage = currentDiv.find(".Results_Div img.head.basket");
    
    if (wdk.user.isGuest()) {
      
      headImage.attr("src",wdk.assetsUrl("wdk/images/basket_gray.png"));
      headImage.attr("title","Please log in to use the basket.");
      
    } else {
      
      var allIn = true;
      currentDiv.find(".Results_Div img.basket").each(function() {
        if (!($(this).hasClass("head"))) {
          if ($(this).attr("value") === '0') {
            allIn = false;
          }
        }
      });
      
      if (allIn) {
        headImage.attr("src",wdk.assetsUrl("wdk/images/basket_color.png"));
        headImage.attr("title","Click to remove the items in this page from the basket.");
        headImage.attr("value", "1");
        
      } else {
        headImage.attr("src",wdk.assetsUrl("wdk/images/basket_gray.png"));
        headImage.attr("title","Click to add the items in this page to the basket.");
        headImage.attr("value", "0");
        
      }
    }
  }
  
  function getCurrentBasketRegion() {
    return window.wdk.findActiveView();
  }
  
  function getCurrentBasketTab() {
    var $basketMenu = $('#basket-menu');
    var index = $basketMenu.tabs('option', 'active');
    var $tab = $basketMenu.find('> ul > li').eq(index);
    return $tab;
  }
  
  /***************** Basket functions to support basket manipulation from GBrowse ********************/
  
  function performIfItemInBasket(projectId, primaryKey, recordType, yesFunction, noFunction) {
    doAjaxBasketRequest('check', projectId, primaryKey, recordType, function(result) {
      if (result.countProcessed > 0) {
        yesFunction();
      } else {
        noFunction();
      }
    });
  }
  
  function addToBasket(projectId, primaryKey, recordType, successFunction) {
    doAjaxBasketRequest('add', projectId, primaryKey, recordType, successFunction);
  }
  
  function removeFromBasket(projectId, primaryKey, recordType, successFunction) {
    doAjaxBasketRequest('remove', projectId, primaryKey, recordType, successFunction);
  }
  
  function doAjaxBasketRequest(action, projectId, primaryKey, recordType, successFunction) {
    var data = "[{\"source_id\":\"" + primaryKey + "\",\"project_id\":\"" + projectId + "\"}]";
    var requestParams = "action=" + action + "&type=" + recordType + "&data=" + data; // single id data string
    $.ajax({
      url: wdk.webappUrl("processBasket.do"),
      type: "post",
      data: requestParams,
      dataType: "json",
      beforeSend: function(){ /* do nothing here */ },
      success: successFunction,
      error: function(){ alert("Error occurred while executing this operation!"); }
    });
  }
  
  
  ns.configureBasket = configureBasket;
  ns.basketTabSelected = basketTabSelected;
  ns.showBasket = showBasket;
  ns.refreshBasket = refreshBasket;
  ns.ChangeBasket = ChangeBasket;
  ns.emptyBasket = emptyBasket;
  ns.saveBasket = saveBasket;
  ns.updateBasket = updateBasket;
  ns.checkPageBasket = checkPageBasket;
  ns.getCurrentBasketRegion = getCurrentBasketRegion;
  ns.getCurrentBasketTab = getCurrentBasketTab;
  ns.performIfItemInBasket = performIfItemInBasket;
  ns.addToBasket = addToBasket;
  ns.removeFromBasket = removeFromBasket;
  ns.doAjaxBasketRequest = doAjaxBasketRequest; 
  
});
