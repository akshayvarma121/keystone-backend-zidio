package com.zidio.keystone.service.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import java.io.InputStream;

public class S3CompatibleStorageService implements ObjectStorageService {

    private final AmazonS3 s3Client;
    private final String bucketName;

    public S3CompatibleStorageService(AmazonS3 s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        if (!s3Client.doesBucketExistV2(bucketName)) {
            s3Client.createBucket(bucketName);
        }
    }

    @Override
    public String upload(String key, InputStream data, String contentType, long length) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(length);
        s3Client.putObject(bucketName, key, data, metadata);
        return key;
    }

    @Override
    public InputStream download(String key) {
        S3Object object = s3Client.getObject(bucketName, key);
        return object.getObjectContent();
    }
}
