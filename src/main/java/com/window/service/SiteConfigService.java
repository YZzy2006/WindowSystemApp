package com.window.service;

import com.window.entity.SiteConfig;

public interface SiteConfigService {

    SiteConfig get();

    void saveOrUpdate(String configJson);

}
