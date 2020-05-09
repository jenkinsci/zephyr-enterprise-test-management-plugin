package com.thed.service;

import com.thed.model.User;

import java.net.URISyntaxException;

/**
 * Created by prashant on 24/6/19.
 */
public interface UserService extends BaseService {

    /**
     * Validates given credentials and clears token if validated.
     * @param serverAddress
     * @param username
     * @param password
     * @return
     */
    public Boolean verifyCredentials(String serverAddress, String username, String password) throws URISyntaxException;

    /**
     * Validates given credentials and clears token if validated.
     * @param serverAddress
     * @param secretText
     * @return
     */
    public Boolean verifyCredentials(String serverAddress, String secretText) throws URISyntaxException;

    /**
     * Validates given credentials and keeps token and server address if verified.
     * @param serverAddress
     * @param username
     * @param password
     * @return
     */
    public Boolean login(String serverAddress, String username, String password) throws URISyntaxException;

    /**
     * Validates given credentials and keeps token and server address if verified.
     * @param serverAddress
     * @param secretText
     * @return
     */
    public Boolean login(String serverAddress, String secretText) throws URISyntaxException;

    /**
     * Returns current logged in user.
     * @return
     */
    User getCurrentUser();

}
