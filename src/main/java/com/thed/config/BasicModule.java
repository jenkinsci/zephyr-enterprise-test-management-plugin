package com.thed.config;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.thed.service.HttpClientService;
import com.thed.service.impl.HttpClientServiceImpl;

/**
 * Created by prashant on 19/6/19.
 */
public class BasicModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(HttpClientService.class).to(HttpClientServiceImpl.class).in(Singleton.class);
    }
}
