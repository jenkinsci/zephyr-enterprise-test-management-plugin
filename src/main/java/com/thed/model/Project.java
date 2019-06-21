package com.thed.model;

import java.io.Serializable;

/**
 * Created by prashant on 21/6/19.
 */
public class Project extends BaseEntity {

    private Long id;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "{id: " + id + ", name: " + name + "}";
    }
}
