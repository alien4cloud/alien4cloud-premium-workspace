define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('alien4cloud-premium-workspace', ['ngResource']).factory('workspaceServices', ['$alresource', function ($alresource) {
    return {
      resource: $alresource('rest/latest/workspaces'),
      promotionImpact: $alresource('rest/latest/workspaces/promotions/impact'),
      promotions: $alresource('rest/latest/workspaces/promotions'),
      process: function (workspaces, writeRole) {
        var result = {
          writeWorkspaces: [],
          staticFacets: {},
          defaultWorkspaces: []
        };
        _.each(workspaces, function (workspace) {
          if (_.includes(workspace.roles, writeRole)) {
            result.writeWorkspaces.push(workspace);
          }
          if (_.includes(workspace.roles, 'COMPONENTS_BROWSER')) {
            if (_.undefined(result.staticFacets.workspace)) {
              result.staticFacets.workspace = [];
            }
            result.staticFacets.workspace.push({facetValue: workspace.id, count: ''});
            result.defaultWorkspaces.push(workspace.id);
          }
        });
        return result;
      }

    };
  }]);
});
