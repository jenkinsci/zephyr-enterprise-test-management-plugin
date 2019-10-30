package com.thed.model;

/**
 * Created by prashant on 25/6/19.
 */
public class User extends BaseEntity {

    private String title;
    private String username;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
