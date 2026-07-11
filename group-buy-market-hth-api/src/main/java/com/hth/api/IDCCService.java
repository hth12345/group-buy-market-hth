package com.hth.api;

import com.hth.api.response.Response;

public interface IDCCService {
    Response<Boolean> updateConfig(String key, String value);
}
