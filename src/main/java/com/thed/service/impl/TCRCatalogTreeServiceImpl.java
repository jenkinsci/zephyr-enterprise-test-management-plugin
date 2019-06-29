package com.thed.service.impl;

import com.thed.model.TCRCatalogTreeDTO;
import com.thed.service.TCRCatalogTreeService;
import com.thed.utils.ZephyrConstants;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by prashant on 28/6/19.
 */
public class TCRCatalogTreeServiceImpl extends BaseServiceImpl implements TCRCatalogTreeService {

    public TCRCatalogTreeServiceImpl() {
        super();
    }

    @Override
    public List<TCRCatalogTreeDTO> getTCRCatalogTreeNodes(String type, Long releaseId) throws URISyntaxException {
        return zephyrRestService.getTCRCatalogTreeNodes(type, 0l, releaseId);
    }

    @Override
    public TCRCatalogTreeDTO createTCRCatalogTreeNode(TCRCatalogTreeDTO tcrCatalogTreeDTO) throws URISyntaxException {
        return zephyrRestService.createTCRCatalogTreeNode(tcrCatalogTreeDTO);
    }

    @Override
    public TCRCatalogTreeDTO createPhase(String name, String description, Long releaseId, Long parentId) throws URISyntaxException {
        TCRCatalogTreeDTO tcrCatalogTreeDTO = new TCRCatalogTreeDTO();
        tcrCatalogTreeDTO.setName(name);
        tcrCatalogTreeDTO.setDescription(description);
        tcrCatalogTreeDTO.setReleaseId(releaseId);
        tcrCatalogTreeDTO.setParentId(parentId);
        tcrCatalogTreeDTO.setType((parentId == null || parentId == 0) ? ZephyrConstants.TCR_CATALOG_TREE_TYPE_PHASE : ZephyrConstants.TCR_CATALOG_TREE_TYPE_MODULE);

        return createTCRCatalogTreeNode(tcrCatalogTreeDTO);
    }

}
