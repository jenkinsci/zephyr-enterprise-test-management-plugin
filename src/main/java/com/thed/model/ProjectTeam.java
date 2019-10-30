package com.thed.model;

/**
 * Created by prashant on 3/7/19.
 */
public class ProjectTeam extends BaseEntity {

    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
