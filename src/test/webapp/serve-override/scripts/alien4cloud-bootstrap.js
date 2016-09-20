// override the default alien 4 cloud loading in order to automatically add the plugin so that grunt serve takes in account the local-files.
define(function (require) {
  'use strict';
  // require jquery and load plugins from the server
  var mods = {
    'nativeModules': require('a4c-native')
  };
  var alien4cloud = require('alien4cloud');
  var prefixer = require('scripts/plugin-url-prefixer');
  prefixer.enabled = false;

  return {
    startup: function() {
      //some common directives directives
      require(mods.nativeModules , function() {
        // load all plugins and then start alien 4 cloud.
        require(['scripts/plugin-require.config.js'], function() {
          require(['alien4cloud-premium-workspace'], function() {
            alien4cloud.startup();
          });
        });
      });
    }
  };
});
