package com.hth.api.response;

public interface IDCCService {
    Response<Boolean> updateConfig(String key, String value);
}
