/**
 * Extensions to the DataTables plugin.
 */

import $ from 'jquery';


// Custom types
// ------------

// Example: 1.04e-3
$.extend( $.fn.dataTableExt.oSort, {
  "scientific-pre": function ( a ) {
    return Number(a);
  },

  "scientific-asc": function ( a, b ) {
    return ((a < b) ? -1 : ((a > b) ? 1 : 0));
  },

  "scientific-desc": function ( a, b ) {
    return ((a < b) ? 1 : ((a > b) ? -1 : 0));
  }
} );


// Custom plugin wrapper
// ---------------------

$.fn.wdkDataTable = function(opts) {
  return this.each(function() {
    var $this = $(this),
      sorting = $this.data("sorting"),
      dataTableOpts = {
        columns: null,
        scrollX: "100%",
        scrollY: "600px",
        scrollCollapse: true,
        paging: false,
        jQueryUI: true,
        language: {
          search: "Filter:"
        }
      };

      if ($this.length === 0) return;

      if (sorting) {
        dataTableOpts.aoColumns = $.map(sorting, function(s) {
          var column = {},
            types = ['string', 'numeric', 'data', 'html'];
          if (s === true) {
            // if true, use defaults -- map wants [null]
            column = [null];
          } else {
            // bSortable must be a Boolean
            column.bSortable = Boolean(s);
            // only set sType if a valid type
            if (types.join("@~@").indexOf(s) > -1) column.sType = s;
          }
          return column;
        });
      }

      // allow options to be passed like in the default dataTable function
      return $this.dataTable($.extend(dataTableOpts, opts));
  });
};


// Global event handlers
// ---------------------

// need to call draw on dataTables that are children of a tab panel
$(document).on('tabsactivate', function() {
  $($.fn.dataTable.tables(true)).DataTable().columns.adjust();
});
