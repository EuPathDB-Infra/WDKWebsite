import _ from 'lodash';

wdk.namespace("window.wdk.util", function(ns, $) {
  "use strict";

  //show the loading icon in the upper right corner of the strategy that is being operated on
  function showLoading(divId) {
    var d;
    var le;
    var t;
    var l_gif;
    var sz;

    if (divId === undefined) {
      d = $("#Strategies");
      le = "10px";
      t = "15px";
      l_gif = "loading.gif";
      sz = "35";
    } else if($("#diagram_" + divId).length > 0) {
      d = $("#diagram_" + divId);
      le = "10px";
      t = "12px";
      l_gif = "loading.gif";
      sz = "35";
    } else {
      d = $("#" + divId);
      le = "405px";
      t = "160px";
      l_gif = "loading.gif";
      sz = "50";
    }

    var l = document.createElement('span');
    $(l).attr("id","loadingGIF");
    var i = document.createElement('img');
    $(i).attr("src",wdk.assetsUrl("wdk/images/" + l_gif));
    $(i).attr("height",sz);
    $(i).attr("width",sz);
    $(l).prepend(i);
    $(l).css({
      "text-align": "center",
      position: "absolute",
      left: le,
      top: t
    });
    $(d).append(l);
  }

  // remove the loading icon for the given strategy
  function removeLoading(divId) {
    if (divId === undefined) {
      $("#Strategies span#loadingGIF").remove();
    } else {
      $("#diagram_" + divId + " span#loadingGIF").remove();
    }
  }

  function checkEnter(ele,evt) {
    var charCode = (evt.which) ? evt.which : evt.keyCode;
    if(charCode == 13) $(ele).blur();
  }

  // parse the value of the `name` param from the `url`
  function parseUrlUtil(name,url) {
     name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]"); // jshint ignore:line
     var regexS = "[\\?&]" + name + "=([^&#]*)";
     var regex = new RegExp( regexS,"g" );
     var res = [];
     var results = regex.exec( url );
     if ( results !== null ) {
       res.push(results[1]);
     }
     if (res.length === 0) {
       return "";
     } else {
       return res;
     }
  }

/* cris 3-26-13: added function below using values from backend
  function getDisplayType(type, number) {
    if (number == 1) {
      return type;
    } else if (type.charAt(type.length-1) === 'y') {
      return type.replace(/y$/,'ies');

    } else {
      return type + 's';
    }
  }
*/
  /**
   * If result size is 1, return singular name, otherwise plural.
   *
   * Examples:
   *     -1 => Loading Genes
   *     0  => 0 Genes
   *     1  => 1 Gene
   *     10 => 10 Genes
   */
  function getDisplayType(myStep) {
    return myStep.results == 1
      ? myStep.shortDisplayType
      : myStep.shortDisplayTypePlural;
  }

  function initShowHide(details) {
    $(".param-group[type='ShowHide']",details).each(function() {
      // register the click event
      var name = $(this).attr("name") + "_details";
      var expire = 365;   // in days
      $(this).find(".group-handle").unbind('click').click(function() {
        var handle = this;
        var path = handle.src.substr(0, handle.src.lastIndexOf("/"));
        var detail = $(this).parents(".param-group").children(".group-detail");
        detail.toggle();
        if (detail.css("display") == "none") {
          handle.src = path + "/plus.gif";
          wdk.createCookie(name, "hide", expire);
        } else {
          handle.src = path + "/minus.gif";
          wdk.createCookie(name, "show", expire);
        }
      });

      // decide whether need to change the display or not
      var showFlag = wdk.readCookie(name);
      if (showFlag === null) return;
          
      var status = $(this).children(".group-detail").css("display");
      if ((showFlag == "show") && (status == "none")) {   
        // should show, but it is hidden
        $(this).find(".group-handle").trigger("click");
      } else if ((showFlag == "hide") && (status != "none")) {
        // should hide, bit it is shown
        $(this).find(".group-handle").trigger("click");
      }
    });
  }

  function setFrontAction(action, strat, step) {
    $("#loginForm form[name=loginForm]").append("<input type='hidden' name='action' value='" + action + "'/>");
    $("#loginForm form[name=loginForm]").append("<input type='hidden' name='actionStrat' value='" + strat + "'/>");
    $("#loginForm form[name=loginForm]").append("<input type='hidden' name='actionStep' value='" + step + "'/>");
  }

  function setDraggable(e, handle) {
    var rlimit,
        tlimit,
        blimit;
    rlimit = $("div#contentwrapper").width() - e.width() - 18;
    if (rlimit < 0) rlimit = 525;
    blimit = $("body").height();
    tlimit = $("div#contentwrapper").offset().top;
    $(e).draggable({
      handle: handle,
      containment: [0, tlimit, rlimit, blimit]
    });
  }

  // Credit to Jason Bunting and Alex Nazarov for this helpful function
  // See: http://stackoverflow.com/questions/359788/how-to-execute-a-javascript-function-when-i-have-its-name-as-a-string
  function executeFunctionByName(functionName, ns, context /*, args... */) {
      var args = Array.prototype.slice.call(arguments, 3);
      var namespaces = functionName.split(".");
      var func = namespaces.pop();
      for (var i = 0; i < namespaces.length; i++) {
          ns = ns[namespaces[i]];
      }
      if (ns[func] instanceof Function &&
          (typeof context !== "undefined" && context !== null )) {
        return ns[func].apply(context, args);
      } else {
        if (typeof console !== "undefined" && console.error) {
          console.error("Reference error: " + functionName + " is not a function");
        }
        return false;
      }
  }
  
  function executeOnloadFunctions(selector) {
    $(selector).find(".onload-function").each(function() {
      var data = $(this).data();
      if (data.invoked) return true;
      executeFunctionByName(data["function"], window, this, data.arguments);
      $(this).data("invoked", true).removeClass("onload-function");
    });
   }

  function sendContactRequest(form, successCallback) {
    // send request
    $.post($(form).attr("action"), $(form).serialize(), function(data) {
      switch (data.status) {
    
        case "success":
          successCallback(data);
          break;
    
        case "error":
          var response = "<h3>Please try to correct any errors below</h3>" +
              "<br/>" + data.message;
          $("<div></div>").html(response).dialog({
            title: "Oops! An error occurred.",
            buttons: [{
              text: "OK",
              click: function() { $(this).dialog("close"); }
            }],
            modal: true
          });
          break;
      }
    }, "json").error(function(jqXHR, textStatus) {
      var response = "<h3>A " + textStatus + " error occurred.</h3><br/>" +
          "<p>This indicates a problem with our server. Please email " +
          "support directly.";
      $("<div></div>").html(response).dialog({
        title: "Oops! An error occurred.",
        buttons: [{
          text: "OK",
          click: function() { $(this).dialog("close"); }
        }],
        modal: true
      });
    });
  }

  function playSadTrombone() {
    $('body').append("<iframe width=\"0px\" height=\"0px\" src=\"http://sadtrombone.com/?play=true\"></iframe>");
  }

  function submitError() {
    var errorForm = $('#error-submission-form')[0];
    sendContactRequest(errorForm, function(){
      $('#open-error-thanks-link').click();
    });
  }

  function toggleErrorDetails() {
    var jqExceptionDiv = $('#exception-information');
    jqExceptionDiv.toggle();
    $('#exception-details-link').html(jqExceptionDiv.is(':hidden') ?
        "Show Details" : "Hide Details");
  }

  // determine if element is within 500px of the viewport
  function elementNearViewport(el) {
    var boundaryBuffer = 0,
        viewportHeight = $(window).height(),
        boundaryTop = $(document).scrollTop() - boundaryBuffer,
        boundaryBottom = boundaryTop + viewportHeight + boundaryBuffer,
        elTop = $(el).offset().top,
        elBottom = elTop + $(el).height(),
        topInBoundary = boundaryTop < elTop && elTop < boundaryBottom,
        bottomInBoundary = boundaryTop < elBottom && elBottom < boundaryBottom;

    return (topInBoundary || bottomInBoundary);

  }

  // Adds a form element whose value will be the elapsed
  // time since the form was loaded.
  //
  // This works in coordination with org.gusdb.wdk.controller.actionutils.WdkAction.shouldCheckSpam()
  function addSpamTimestamp(form) {
    var $__ts = $('<input type="hidden" name="__ts"/>');
    $(form).append($__ts);

    (function setElapsedTime($input, start, now) {
      $input.val(Math.floor((now - start) / 1000));
      setTimeout(setElapsedTime.bind(this, $input, start, _.now()), 1000);
    }($__ts, _.now(), _.now()));
  }

  /**
   * Utility for escaping meta-characters in a CSS selector component. This is
   * useful for both jQuery and DOM API methods (querySelector and
   * querySelectorAll);
   *
   * Example:
   *
   *    var questionableId = getIdFromSomewhere();
   *    $(node).find('#' + escapeSelectorComponent(questionableId));
   */
  var cssSpecialChars = [
    '!', '"', '#', '$', '%',
    '&', '\'', '(', ')', '*',
    '+', ',', '.', '/', ':',
    ';', '<', '=', '>', '?',
    '@', '[', '\\', ']', '^',
    '`', '{', '|', '}', '~', ' '];

  var cssSpecialCharsReString = _.escapeRegExp(cssSpecialChars.join(''));
  var cssSpecialCharsRe = new RegExp('([' + cssSpecialCharsReString + '])', 'g');

  function escapeSelectorComponent(str) {
    return str.replace(cssSpecialCharsRe, '\\$1');
  }

  ns.getDisplayType = getDisplayType;
  ns.initShowHide = initShowHide;
  ns.parseUrlUtil = parseUrlUtil;
  ns.removeLoading = removeLoading;
  ns.setDraggable = setDraggable;
  ns.setFrontAction = setFrontAction;
  ns.showLoading = showLoading;
  ns.checkEnter = checkEnter;
  ns.executeFunctionByName = executeFunctionByName;
  ns.executeOnloadFunctions = executeOnloadFunctions;
  ns.sendContactRequest = sendContactRequest;
  ns.playSadTrombone = playSadTrombone;
  ns.submitError = submitError;
  ns.toggleErrorDetails = toggleErrorDetails;
  ns.elementNearViewport = elementNearViewport;
  ns.addSpamTimestamp = addSpamTimestamp;
  ns.escapeSelectorComponent = escapeSelectorComponent;

});
