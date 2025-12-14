package com.kuru.delivery.order.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud.name:}") String cloudName,
            @Value("${cloudinary.api.key:}") String apiKey,
            @Value("${cloudinary.api.secret:}") String apiSecret,
            @Value("${cloudinary.secure:true}") boolean secure) {
        
        if (cloudName == null || cloudName.isEmpty() || 
            apiKey == null || apiKey.isEmpty() || 
            apiSecret == null || apiSecret.isEmpty()) {
            // Cloudinary not configured, will use local storage
            this.cloudinary = null;
        } else {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", secure
            ));
        }
    }

    public boolean isConfigured() {
        return cloudinary != null;
    }

    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary is not configured. Please set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET environment variables.");
        }

        Map<String, Object> uploadParams = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "overwrite", true,
                "invalidate", true
        );

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        return (String) uploadResult.get("secure_url");
    }

    public void deleteImage(String imageUrl) throws IOException {
        if (!isConfigured() || imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return; // Not a Cloudinary URL or not configured
        }

        // Extract public_id from URL
        String publicId = extractPublicId(imageUrl);
        if (publicId != null) {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        }
    }

    private String extractPublicId(String imageUrl) {
        try {
            // Cloudinary URL format: https://res.cloudinary.com/{cloud_name}/image/upload/{version}/{public_id}.{format}
            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                String afterUpload = parts[1];
                // Remove version if present (v1234567890/)
                if (afterUpload.contains("/")) {
                    String[] segments = afterUpload.split("/", 2);
                    if (segments.length > 1) {
                        String publicIdWithExt = segments[1];
                        // Remove file extension
                        return publicIdWithExt.substring(0, publicIdWithExt.lastIndexOf('.'));
                    }
                }
            }
        } catch (Exception e) {
            // If extraction fails, return null
        }
        return null;
    }
}

