package com.thed.model;

import java.util.Set;

/**
 * Created by prashant on 28/6/19.
 */
public class TCRCatalogTreeDTO extends BaseEntity {

    private String name;
    private String description;
    private String type;
    private Long releaseId;
    private Set<TCRCatalogTreeDTO> categories;
    private Long parentId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(Long releaseId) {
        this.releaseId = releaseId;
    }

    public Set<TCRCatalogTreeDTO> getCategories() {
        return categories;
    }

    public void setCategories(Set<TCRCatalogTreeDTO> categories) {
        this.categories = categories;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
