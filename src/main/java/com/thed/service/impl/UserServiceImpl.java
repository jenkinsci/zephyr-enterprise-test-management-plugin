package com.thed.service.impl;

import com.thed.service.UserService;
import com.thed.service.ZephyrRestService;

import java.net.URISyntaxException;

/**
 * Created by prashant on 24/6/19.
 */
public class UserServiceImpl extends BaseServiceImpl implements UserService {

    public UserServiceImpl() {
        super();
    }

    @Override
    public Boolean verifyCredentials(String serverAddress, String username, String password) throws URISyntaxException {
        return zephyrRestService.verifyCredentials(serverAddress, username, password);
    }

    @Override
    public Boolean login(String serverAddress, String username, String password) throws URISyntaxException {
        return zephyrRestService.login(serverAddress, username, password);
    }
}
