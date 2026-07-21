package com.zidio.keystone.service.storage;

import java.io.InputStream;

public interface ObjectStorageService {
    String upload(String key, InputStream data, String contentType, long length);
    InputStream download(String key);
}
