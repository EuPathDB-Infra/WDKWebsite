wdk.namespace("window.wdk.publicStrats", function(ns, $) {
  "use strict";

  var publicStratDescriptionWarning = "Before making your strategy public, you " +
      "must add a description so others know how it can be used.";

  function showPublicStrats() {
    $("body").block();
    $.ajax({
      url: "showPublicStrats.do",
      dataType: "html",
      success: function(data) {

        // put retrieved html in the correct div and configure data table
        $("#public_strat").html(data);
        configurePublicStratTable();

        // update the number of public strategies
        $("#strategy_tabs li a#tab_public_strat font.subscriptCount")
            .html("(" + $("#public_strat span#publicStrategyCount").html() + ")");

        // unblock the UI
        $("body").unblock();
      },
      error: function(data, msg, e) {
        $("body").unblock();
        alert("ERROR\n"+ msg + "\n" + e +
            "\nReloading this page might solve the problem.\n" +
            "Otherwise, please contact site support.");
      }
    });
  }

  function configurePublicStratTable() {
    var drawFromSecondarySort = false;
    // these are the initial sort params (though always put project examples at top)
    var lastSortCol = 6;
    var lastSortDir = "desc";
    $("#public_strat table.datatables").dataTable({
      "bAutoWidth": false,
      "bJQueryUI": true,
      "bScrollCollapse": true,
      "bPaginate": false,
      "aoColumns": [ { "bSearchable": false, "bVisible": false },
        null,
        null,
        { "bSortable": false },
        null,
        null,
        null ],
      "aaSorting": [[0, "desc"], [ lastSortCol, lastSortDir ]],
      // The purpose of the following function is to enable the "Always push to top" checkbox
      // After the user clicks a sortable column header, we re-sort examples to top if
      // necessary, but must also detect if this is a direction change for that column.
      "fnDrawCallback": function() {
        if ($('#sampleToTopCheckbox').prop('checked') && !drawFromSecondarySort) {
          var sortSettings = getDataTable().fnSettings().aaSorting;
          var newSortCol = sortSettings[0][0];
          var newSortDir = sortSettings[0][1];
          var secondarySort;
          if (newSortCol == lastSortCol && newSortDir == lastSortDir) {
            // set secondary sort to reverse direction of previous sort
            newSortDir = (newSortDir == 'asc' ? 'desc' : 'asc');
            secondarySort = [ newSortCol, newSortDir ];
          }
          lastSortCol = newSortCol;
          lastSortDir = newSortDir;
          drawFromSecondarySort = true;
          sortSampleToTop(secondarySort);
        } else {
          drawFromSecondarySort = false;
        }
      }
    });
    // make search textbox appear where we want
    $('#public_strat table.datatables').parent().parent().parent().find('.dataTables_filter')
       .css("display","inline-block").css("margin","0").css("padding","5px 0 0 0");
  }

  /* FIXME: First attempt at adding 'make public' link on right side of strategy display;
   *        Not called and does not work!  See also view.js, search 'publicizeStrat'
  function togglePublicFromLink(atag, stratId) {
    wdk.strategy.model.getStrategyOBJ(stratId)
      .then(function(stratObj) {
        var isPublic = stratObj.isPublic;
        //var description = atag.parents('.strategy-data').data('description');
        //wdk.history.showUpdateDialog(this, false, true, true);
        wdk.history.showUpdateDialog(atag, false, false, !isPublic);
        //this, false, true, true
        //showUpdateDialog(, save, fromHist, strat.isPublic);
      });
  }
  */

  function togglePublic(checkbox, stratId) {
    var isPublic = $(checkbox).prop('checked');
    var description = $(checkbox).parent().parent().find('.strategy_description div').html();
    if (description == "Click to add a description" || description.trim() === "") {
      alert(publicStratDescriptionWarning);
      $(checkbox).prop('checked', !isPublic);
      return;
    }
    $(checkbox).parent().find('img').css('display','inline-block');
    $.ajax({
      type : "POST",
      url : "processPublicStratStatus.do",
      data : { "stratId" : stratId, "isPublic" : isPublic },
      dataType : "json",
      success : function() {
        // set data on row to updated value
        $(checkbox).parents(".strategy-data").data("isPublic", ($(checkbox).prop('checked') ? "true" : "false"));
        // remove spinner; operation complete
        $(checkbox).parent().find('img').css('display','none');
        // do nothing else to inform user
        //var publicStatus = isPublic ? "public" : "private";
        //alert("Successfully set strat with ID " + stratId + " to " + publicStatus + ".");
      },
      error : function() {
        $(checkbox).parent().find('img').css('display','none');
        alert("We are unable to change the status of this strategy at this time.  " +
              "Please try again later.  If the problem persists, please use the " +
              "'Contact Us' link above to inform us.");
        $(checkbox).prop('checked', !isPublic);
      }
    });
  }

  function goToPublicStrats() {
    // first set cookie to show public strats tab
    wdk.stratTabCookie.setCurrentTabCookie('application', 'public_strat');
    // then move location to strategies workspace
    window.location = "showApplication.do";
  }

  function toggleSampleOnly(checkbox, authorFilterValue) {
    var authorColumnNumber = 4;
    // if box is checked then filter on this project's configured author; else clear filter
    var filterVal = ($(checkbox).prop('checked') ? authorFilterValue : '');
    getDataTable().fnFilter(filterVal,authorColumnNumber,false,true);
  }

  function toggleSampleToTop(checkbox) {
    if ($(checkbox).prop('checked')) {
      sortSampleToTop();
    }
  }

  function sortSampleToTop(secondarySort) {
    var newSortArray = (typeof secondarySort === 'undefined' ?
            [[0, 'desc']] : [[0, 'desc'], secondarySort]);
    getDataTable().fnSort(newSortArray);
  }

  function getDataTable() {
    return $("#public_strat table.datatables").dataTable();
  }

  // make the following methods "public" (i.e. available in the namespace)
  ns.showPublicStrats = showPublicStrats;
  ns.configurePublicStratTable = configurePublicStratTable;
  ns.togglePublic = togglePublic;
  //ns.togglePublicFromLink = togglePublicFromLink;
  ns.goToPublicStrats = goToPublicStrats;
  ns.publicStratDescriptionWarning = publicStratDescriptionWarning;
  ns.toggleSampleOnly = toggleSampleOnly;
  ns.toggleSampleToTop = toggleSampleToTop;
  ns.sortSampleToTop = sortSampleToTop;

});
