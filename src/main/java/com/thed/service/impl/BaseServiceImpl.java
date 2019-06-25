package com.thed.service.impl;

import com.thed.service.BaseService;
import com.thed.service.ZephyrRestService;

/**
 * Created by prashant on 25/6/19.
 */
public class BaseServiceImpl implements BaseService {

    protected static ZephyrRestService zephyrRestService = new ZephyrRestServiceImpl();

    public BaseServiceImpl() {
    }

}
