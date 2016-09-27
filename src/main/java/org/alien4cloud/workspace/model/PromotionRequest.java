package org.alien4cloud.workspace.model;

import java.util.Date;

import javax.validation.constraints.NotNull;

import org.elasticsearch.annotation.DateField;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.TimeStamp;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ESObject
public class PromotionRequest {

    @TermsFacet
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String requestUser;

    @TermsFacet
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String processUser;

    @TermFilter
    @DateField
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    private Date requestDate;

    @TermFilter
    @DateField
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    private Date processDate;

    @NotNull
    @TermsFacet
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String csarName;

    @NotNull
    @TermsFacet
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String csarVersion;

    @NotNull
    @TermsFacet
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String targetWorkspace;

    @TermsFacet
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private PromotionStatus status;

    @Id
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    public String getId() {
        return getCsarName() + ":" + getCsarVersion() + ":" + getTargetWorkspace();
    }

    public void setId(String id) {
        // Ignore
    }
}
