package org.alien4cloud.workspace.model;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.Csar;

import alien4cloud.model.common.Usage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CSARPromotionImpact {

    /**
     * map of csar's id to its current usage in Alien
     */
    private Map<String, List<Usage>> currentUsages;

    /**
     * map of csar's id to its content
     */
    private Map<String, Csar> impactedCsars;

    /**
     * true if the user has write access on the target workspace
     */
    private boolean hasWriteAccessOnTarget;
}
