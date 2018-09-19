wdk.namespace("window.wdk.tooltips", function(ns, $) {
  "use strict";

  /**
   * Allows developer assign some functionality and easily let individual
   * functions (or a calling function) assign the rest via a flat hash.
   */
  function makeConf(hash) {
    hash.getOrDefault = function(key, defaultVal) {
      return this[key] || defaultVal;
    };
    return hash;
  }

  function getConf(hash) {
    return hash ? makeConf(hash) : makeConf({});
  }

  /**
   * Basic HTML tooltip functionality.  Pass in selector and optional
   * configuration object.  Currently supported options (with defaults):
   *  - xOffset (0): how far right or left to place the tooltip
   *  - tipPos ('top-center'): where on the tip the "arrow" is
   *  - targetPos ('bottom-center'): where on the target the tip extends from
   */
  function setUpTooltips(selector, conf) {
    $(selector).qtip({
      position: {
        adjust: {
          x: conf.getOrDefault('xOffset', 0),
          y: 3
        },
        viewport: $(window),
        my: conf.getOrDefault('tipPos','top-center'),
        at: conf.getOrDefault('targetPos','bottom-center')
      },
      show: {
        solo: true,
        event: 'mouseenter'
      },
      hide: {
        event: 'mouseleave'
      },
      style: {
        classes: 'qtip-wdk'
      }
    });
  }

  /************* convenience function for basic tooltips *************/

  // places tip, centered, below the target
  function assignTooltips(selector, xOffset) {
    setUpTooltips(selector, makeConf({
      xOffset: xOffset,
      tipPos: 'top-center',
      targetPos: 'bottom-center'
    }));
  }

  // places tip to the left of and below the target, keyed off the bottom left corner 
  function assignTooltipsLeft(selector, xOffset) {
    setUpTooltips(selector, makeConf({
      xOffset: xOffset,
      tipPos: 'top-right',
      targetPos: 'bottom-left'
    }));
  }

  // places tip to the right of and below the target, keyed off the bottom right corner 
  function assignTooltipsRight(selector, xOffset) {
    setUpTooltips(selector, makeConf({
      xOffset: xOffset,
      tipPos: 'top-left',
      targetPos: 'bottom-right'
    }));
  }

  //places tip below the target but left justified (so tip does not spread further left than target) 
  function assignTooltipsLeftJustified(selector, xOffset) {
    setUpTooltips(selector, makeConf({
      xOffset: xOffset,
      tipPos: 'top-left',
      targetPos: 'bottom-left'
    }));
  }

  /**
   * "Sticky" HTML tooltip functionality.  Pass in selector, content, and optional
   * configuration object.  Currently supported options (with defaults):
   *  - xOffset (0): how far right or left to place the tooltip
   *  - yOffset (0): how far up or down (vertically) to place the tooltip
   *  - tipPos ('top-center'): where on the tip the "arrow" is
   *  - targetPos ('bottom-center'): where on the target the tip extends from
   *  - stickySecs (1): how long the tooltip will appear on screen if mouse is no longer interacting with it
   */
  function setUpStickyTooltip(tipTarget, tipContent, conf) {
    var width;

    // use default config is it is not passed in
    conf = conf || getConf();

    // obtain the width from content
    if (typeof tipContent !== "string") {
      width = $(tipContent).width();
    }
    width = width || 200;

    $(tipTarget).qtip({
      content : tipContent,
      show: {
        solo: true,
        event: 'click mouseenter'
      },
      hide: {
        event: 'click'
      },
      style: {
        width: width,
        classes: 'qtip-wdk'
      },
      position: {
        adjust: {
          x: conf.getOrDefault('xOffset', 0),
          y: conf.getOrDefault('yOffset', 0)
        },
        viewport: $(window),
        my: conf.getOrDefault('tipPos','top-left'),
        at: conf.getOrDefault('targetPos','bottom-right')
      },
      events: {
        show: function(event, api) {
          // qtip2 assigns an ID of "qtip-<id>" to the tooltip div
          var tipSelector = '#qtip-' + api.get('id');

          // reach into the qtip attributes to retrieve the target element
          var target = $(this).closest('.qtip').data('qtip').options.position.target;

          // define functions for hiding tooltip timer.
          var cancelDelayedHide = function() {
            // clear the previous timers
            var timer = $(tipSelector).attr("timer");
            if (timer !== undefined) {
              clearTimeout(timer);
              $(tipSelector).removeAttr("timer");
            }
          };

          var hide = function() {
            $(tipSelector).qtip('hide'); 
            cancelDelayedHide();
          };

          var delayedHide = function() {
            cancelDelayedHide();
            var timer = setTimeout(function() {
              $(tipSelector).qtip('hide');
            }, conf.getOrDefault('stickySecs', 1) * 1000);
            $(tipSelector).attr("timer", timer);
          };

          // when mouseover the span, canel delayed hiding, if there's any;
          // when mouseout the span, start delayed hiding tip.
          target.mouseover(cancelDelayedHide)
              .mouseout(delayedHide);
          // when mouse in tip, cancel delayed hiding; 
          // when clicking tip, hide tip immediately;
          // when mouse out tip, start delayed hiding.
          $(tipSelector).mouseover(cancelDelayedHide)
              .click(hide)
              .mouseout(delayedHide);
        }
      }
    });
  }

  /************* convenience function for sticky tooltips *************/

  // provides content from the title attribute of the target
  function assignStickyTooltipByTitle(selector, config) {
    $(selector).each(function() {
      var content = $(this).attr("title");
      setUpStickyTooltip(this, content, getConf(config));
    });
  }

  // provides content from a child div with class "tooltip"
  function assignStickyTooltipByElement(selector, config) {
    $(selector).each(function() {
      var content = $(this).children(".tooltip");
      setUpStickyTooltip(this, content, getConf(config));
    });
  }

  // special options for question param help on the question page
  function assignParamTooltips(selector) {
    assignStickyTooltipByTitle(selector, {
      yOffset: 3,
      tipPos: 'top-right',
      targetPos: 'bottom-center',
      stickySecs: 7
    });
  }

  ns.assignTooltips = assignTooltips;
  ns.assignTooltipsLeft = assignTooltipsLeft;
  ns.assignTooltipsLeftJustified = assignTooltipsLeftJustified;
  ns.assignTooltipsRight = assignTooltipsRight;
  ns.assignStickyTooltipByTitle = assignStickyTooltipByTitle;
  ns.assignStickyTooltipByElement = assignStickyTooltipByElement;
  ns.assignParamTooltips = assignParamTooltips;
  ns.setUpStickyTooltip = setUpStickyTooltip;
});
