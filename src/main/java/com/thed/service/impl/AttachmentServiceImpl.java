package com.thed.service.impl;

import com.thed.model.Attachment;
import com.thed.model.GenericAttachmentDTO;
import com.thed.service.AttachmentService;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by prashant on 6/1/20.
 */
public class AttachmentServiceImpl extends BaseServiceImpl implements AttachmentService {

    public AttachmentServiceImpl() {
        super();
    }

    @Override
    public void addAttachments(ItemType itemType, Map<Long, List<String>> itemIdFilePathMap, Map<Long, GenericAttachmentDTO> statusAttachmentMap) throws IOException, URISyntaxException {

        for (Long itemId : itemIdFilePathMap.keySet()) {
            List<String> attachmentFilePaths = itemIdFilePathMap.get(itemId);
            List<GenericAttachmentDTO> attachmentDTOs = new ArrayList<>();
            for (String filePath : attachmentFilePaths) {

                Path path = Paths.get(filePath);
                String fileName = path.getFileName().toString();
                String mimeType = Files.probeContentType(path);
                byte[] bytes = Files.readAllBytes(Paths.get(filePath));

                GenericAttachmentDTO attachmentDTO = new GenericAttachmentDTO();
                attachmentDTO.setFieldName(itemType.toString().toLowerCase());
                attachmentDTO.setFileName(fileName);
                attachmentDTO.setContentType(mimeType);
                attachmentDTO.setByteData(bytes);

                attachmentDTOs.add(attachmentDTO);
            }
            if(statusAttachmentMap.containsKey(itemId)) {
                attachmentDTOs.add(statusAttachmentMap.get(itemId));
            }
            List<GenericAttachmentDTO> newAttachmentDTOs = zephyrRestService.uploadAttachments(attachmentDTOs);

            List<Attachment> attachments = new ArrayList<>();

            for (GenericAttachmentDTO genericAttachmentDTO : newAttachmentDTOs) {
                Attachment attachment = new Attachment();
                attachment.setContentType(genericAttachmentDTO.getContentType());

                GenericAttachmentDTO oldAttachment = attachmentDTOs.stream().filter(attachmentDTO -> genericAttachmentDTO.getFileName().equals(attachmentDTO.getFileName())).findAny().orElse(null);

                attachment.setFileSize(oldAttachment != null ? (long)oldAttachment.getByteData().length : 0);
                attachment.setItemId(itemId);
                attachment.setItemType(itemType.toString().toLowerCase());
                attachment.setName(genericAttachmentDTO.getFileName());
                attachment.setTempPath(genericAttachmentDTO.getTempFilePath());
                attachment.setCreatedBy(zephyrRestService.getCurrentUser().getId());
                attachment.setTimeStamp(System.currentTimeMillis());

                attachments.add(attachment);
            }

            zephyrRestService.addAttachment(attachments);

        }

    }

}
