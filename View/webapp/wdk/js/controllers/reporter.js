wdk.namespace("wdk.reporter", function(ns, $) {
  "use strict";

  var $form, $fields, $defaultFields;

  var init = function() {
    $form = $(this);
    $fields = $form.find("[name='selectedFields'][value!='default']");
    $defaultFields = $form.find("[name='selectedFields'][value='default']");

    // attach handlers
    $form.find("input[value='select all']").click(function() {
      selectFields(1);
    });
    $form.find("input[value='clear all']").click(function() {
      selectFields(0);
    });
    $form.find("input[value='select inverse']").click(function() {
      selectFields(-1);
    });

    $defaultFields.click(function() {
      defaultFields(this.checked);
    });
    $fields.click(function() {
      defaultFields(false);
    });
  };

  var defaultFields = function(/* Boolean */ use) {
    if (use) {
      // select default, unselect non-default
      $defaultFields.attr("checked", true);
      $fields.attr("checked", false);
    } else {
      // unselect default
      $defaultFields.attr("checked", false);
    }
  };

  var selectFields = function(state) {
    $fields.each(function(idx, cb) {
      cb.checked = state === -1 ? !cb.checked : Boolean(state);
    });
    $defaultFields.attr("checked", false);
  };

  ns.init = init;

});
