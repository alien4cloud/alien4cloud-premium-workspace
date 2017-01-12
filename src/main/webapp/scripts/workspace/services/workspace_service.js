define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('alien4cloud-premium-workspace', ['ngResource']).factory('workspaceServices', ['$alresource', function ($alresource) {
    var resource = $alresource('rest/latest/workspaces');
    var promotionImpact = $alresource('rest/latest/workspaces/promotion-impact');
    var promotions = $alresource('rest/latest/workspaces/promotions');
    return {
      resource: resource,
      promotionImpact: promotionImpact,
      promotions: promotions,
      process: function (workspaces, writeRoles) {
        var result = {
          writeWorkspaces: [],
          staticFacets: {},
          readWorkspaces: []
        };
        _.each(workspaces, function (workspace) {
          if ((_.isArray(writeRoles) && _.intersection(writeRoles, workspace.roles).length > 0) || _.includes(workspace.roles, writeRoles)) {
            result.writeWorkspaces.push(workspace);
          }
          if (_.includes(workspace.roles, 'COMPONENTS_BROWSER')) {
            if (_.undefined(result.staticFacets.workspace)) {
              result.staticFacets.workspace = [];
            }
            result.staticFacets.workspace.push({facetValue: workspace.id, count: ''});
            result.readWorkspaces.push(workspace.id);
          }
        });
        return result;
      }
    };
  }]);
});
