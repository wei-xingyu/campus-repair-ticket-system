package com.weixingyu.campusrepairticketsystem.utils;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import org.springframework.stereotype.Component;

import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class AliyunOSSOperator {
    private final AliyunOSSProperties properties;

    public AliyunOSSOperator(AliyunOSSProperties properties) {
        this.properties = properties;
    }

    private static final String UPLOADER_METADATA = "ticket-uploader-id";

    public String upload(byte[] content, String suffix, Long uploaderId) throws Exception {
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String objectName = dir + "/" + UUID.randomUUID() + suffix;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        metadata.addUserMetadata(UPLOADER_METADATA, uploaderId.toString());

        OSS client = createClient();
        try {
            client.putObject(properties.getBucketName(), objectName, new ByteArrayInputStream(content), metadata);
        } finally {
            client.shutdown();
        }
        return objectUrlPrefix() + objectName;
    }

    public boolean isOwnedTicketImage(String url, Long uploaderId) {
        if (url == null || url.length() > 500) {
            return false;
        }
        String prefix = objectUrlPrefix();
        if (!url.startsWith(prefix)) {
            return false;
        }
        String objectName = url.substring(prefix.length());
        if (!objectName.matches("[0-9]{4}/[0-9]{2}/[0-9a-f-]{36}\\.(jpe?g|png|webp)")) {
            return false;
        }

        try {
            OSS client = createClient();
            try {
                ObjectMetadata metadata = client.getObjectMetadata(properties.getBucketName(), objectName);
                return uploaderId.toString().equals(metadata.getUserMetadata().get(UPLOADER_METADATA));
            } finally {
                client.shutdown();
            }
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                return false;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "图片存储服务校验失败，请稍后重试", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "图片存储服务校验失败，请稍后重试", e);
        }
    }

    private String objectUrlPrefix() {
        String endpoint = properties.getEndpoint();
        int hostStart = endpoint.indexOf("//") + 2;
        return endpoint.substring(0, hostStart) + properties.getBucketName() + "."
                + endpoint.substring(hostStart) + "/";
    }

    private OSS createClient() throws Exception {
        EnvironmentVariableCredentialsProvider credentialsProvider =
                CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setSignatureVersion(SignVersion.V4);

        return OSSClientBuilder.create()
                .endpoint(properties.getEndpoint())
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(configuration)
                .region(properties.getRegion())
                .build();
    }
}
