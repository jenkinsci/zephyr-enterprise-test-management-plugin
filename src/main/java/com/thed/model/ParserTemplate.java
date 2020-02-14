package com.thed.model;

import java.util.Date;

/**
 * Created by tarun on 17/01/2020.
 */
public class ParserTemplate extends BaseEntity {

    private String name;
    private String jsonTemplate;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJsonTemplate() {
        return jsonTemplate;
    }

    public void setJsonTemplate(String jsonTemplate) {
        this.jsonTemplate = jsonTemplate;
    }

}