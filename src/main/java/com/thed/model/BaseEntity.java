package com.thed.model;

import java.io.Serializable;

/**
 * Created by prashant on 21/6/19.
 */
public class BaseEntity implements Serializable {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
