package com.thed.service;

import com.thed.model.TCRCatalogTreeDTO;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 28/6/19.
 */
public interface TCRCatalogTreeService extends BaseService {

    /**
     * Get tree nodes such as phase for given releaseId.
     * @param type
     * @param releaseId
     * @return
     * @throws URISyntaxException
     */
    List<TCRCatalogTreeDTO> getTCRCatalogTreeNodes(String type, Long releaseId) throws URISyntaxException;

    /**
     * Get tcrCatalogTree node for given id.
     * @param tcrCatalogTreeId
     * @return
     * @throws URISyntaxException
     */
    TCRCatalogTreeDTO getTCRCatalogTreeNode(Long tcrCatalogTreeId) throws URISyntaxException;

    /**
     * Create a tree node such as phase.
     * @param tcrCatalogTreeDTO
     * @return
     * @throws URISyntaxException
     */
    TCRCatalogTreeDTO createTCRCatalogTreeNode(TCRCatalogTreeDTO tcrCatalogTreeDTO) throws URISyntaxException;

    /**
     * Create a phase tree node for given details.
     * @param name
     * @param description
     * @param releaseId
     * @param parentId
     * @return
     * @throws URISyntaxException
     */
    TCRCatalogTreeDTO createPhase(String name, String description, Long releaseId, Long parentId) throws URISyntaxException;

}
