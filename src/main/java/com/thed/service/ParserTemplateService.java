package com.thed.service;

import com.thed.model.ParserTemplate;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by tarun on 17/01/2020.
 */
public interface ParserTemplateService extends BaseService {

    /**
     * Get the list of all the parser templates
     * @return
     * @throws URISyntaxException
     */
    List<ParserTemplate> getAllParserTemplates() throws URISyntaxException;

    /**
     * Get parser template by ID
     * @param templateId
     * @return
     * @throws URISyntaxException
     */
    ParserTemplate getParserTemplateById(Long templateId) throws URISyntaxException;

}