package org.alien4cloud.workspace.dao;

import java.beans.IntrospectionException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.alien4cloud.workspace.model.PromotionRequest;
import org.springframework.stereotype.Component;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.dao.ElasticSearchMapper;
import alien4cloud.exception.IndexingServiceException;

@Component("workspace-dao")
public class WorkspaceDAO extends ESGenericSearchDAO {

    @PostConstruct
    public void initEnvironment() {
        // init ES annotation scanning
        try {
            getMappingBuilder().initialize(PromotionRequest.class.getPackage().getName());
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // init indices and mapped classes
        setJsonMapper(ElasticSearchMapper.getInstance());
        initIndices(PromotionRequest.class.getSimpleName().toLowerCase(), null, PromotionRequest.class);
        initCompleted();
    }
}
