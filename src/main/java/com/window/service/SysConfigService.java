package com.window.service;

import java.util.Map;

public interface SysConfigService {
    Map<String, String> getAll();
    String get(String key);
    void set(String key, String value, String remark);
    void resetSequences();
}
