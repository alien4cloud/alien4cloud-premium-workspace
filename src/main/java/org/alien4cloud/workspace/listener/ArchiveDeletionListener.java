package org.alien4cloud.workspace.listener;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.events.BeforeArchiveDeleted;
import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.workspace.model.PromotionRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;

@Component
public class ArchiveDeletionListener implements ApplicationListener<BeforeArchiveDeleted> {
    @Inject
    private CsarService csarService;
    @Resource(name = "workspace-dao")
    private IGenericSearchDAO workspaceDAO;

    @Override
    public void onApplicationEvent(BeforeArchiveDeleted beforeArchiveDeleted) {
        Csar csar = csarService.getOrFail(beforeArchiveDeleted.getArchiveId());
        QueryBuilder deleteQuery = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("csarName", csar.getName()))
                .must(QueryBuilders.termQuery("csarVersion", csar.getVersion()));
        workspaceDAO.delete(PromotionRequest.class, deleteQuery);
    }
}
