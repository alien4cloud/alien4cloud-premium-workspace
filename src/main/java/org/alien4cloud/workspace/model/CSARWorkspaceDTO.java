package org.alien4cloud.workspace.model;

import java.util.List;

import org.alien4cloud.tosca.model.Csar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CSARWorkspaceDTO {

    private Csar csar;

    private List<Workspace> availablePromotionTargets;
}
