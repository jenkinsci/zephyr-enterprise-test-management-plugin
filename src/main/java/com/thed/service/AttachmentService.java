package com.thed.service;

import com.thed.model.GenericAttachmentDTO;
import com.thed.service.impl.AttachmentServiceImpl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by prashant on 6/1/20.
 */
public interface AttachmentService extends BaseService {

    public enum ItemType {
        requirement,
        testcase,
        releaseTestSchedule
    }

    /**
     * Uploads attachment provided with given itemType. Returns logs for attachments with errors.
     * @param itemType
     * @param itemIdFilePathMap
     * @param failureAttachmentMap
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    List<String> addAttachments(ItemType itemType, Map<Long, List<String>> itemIdFilePathMap, Map<Long, GenericAttachmentDTO> failureAttachmentMap) throws IOException, URISyntaxException;

}
