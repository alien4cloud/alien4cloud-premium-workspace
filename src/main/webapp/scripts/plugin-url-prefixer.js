// this module is used to prefix the plugins url with the path added by alien 4 cloud plugin file server.

/* global define */

'use strict';

define(function (require) {
  // plugins module from alien 4 cloud
  var plugins = require('plugins');
  var pluginId = 'alien4cloud-premium-workspace';
  return {
    enabled: true,
    prefix: function (url) {
      if (this.enabled) {
        return plugins.base(pluginId) + url;
      } else {
        return url;
      }
    }
  };
});
