package com.sky.utils;

import com.sky.properties.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
@AllArgsConstructor
@Data
@Slf4j
public class MinioUtil {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param  bytes
     * @param objectName 对象名称（文件路径）
     * @return 文件访问 URL
     */
    public String upload(byte[] bytes,  String objectName) {
        // 创建 MinioClient 实例
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        try {
            // 将字节数组转换为输入流
            InputStream inputStream = new ByteArrayInputStream(bytes);
            // 创建PutObject请求并上传
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, bytes.length, -1) // -1表示不限制流的大小
                            .build()
            );
        } catch (MinioException e) {
            System.out.println("MinIO server error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }

        //文件访问路径规则 MinIO：http://Endpoint/BucketName/ObjectName
        // 确保endpoint以/结尾
        if (!endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }
        // 处理objectName开头的/，如有就删除以免后面又添加一个构成双斜杠
        if (objectName.startsWith("/")) {
            //截取字符串从第2个字符开始到末尾的子字符串（即去掉第一个字符）
            objectName = objectName.substring(1);
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(endpoint)
                .append(bucketName)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", urlBuilder.toString());
        return urlBuilder.toString();

    }


}