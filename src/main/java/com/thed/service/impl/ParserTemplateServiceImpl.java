package com.thed.service.impl;

import com.thed.model.ParserTemplate;
import com.thed.service.ParserTemplateService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by tarun on 17/01/2020.
 */
public class ParserTemplateServiceImpl extends BaseServiceImpl implements ParserTemplateService {

    public ParserTemplateServiceImpl() {
        super();
    }

    @Override
    public List<ParserTemplate> getAllParserTemplates() throws URISyntaxException, IOException {
        return zephyrRestService.getAllParserTemplates();
    }

    @Override
    public ParserTemplate getParserTemplateById(Long templateId) throws URISyntaxException, IOException {
        return zephyrRestService.getParserTemplateById(templateId);
    }
}