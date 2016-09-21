define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('alien4cloud-premium-workspace', ['ngResource']).factory('workspaceServices', ['$alresource', function ($alresource) {
    return {
      upload: $alresource('/rest/latest/workspaces/upload'),
      search: $alresource('/rest/latest/workspaces/search')
    };
  }]);
});
