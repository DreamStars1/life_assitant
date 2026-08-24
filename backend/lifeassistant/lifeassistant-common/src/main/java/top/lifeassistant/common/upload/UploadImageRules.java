package top.lifeassistant.common.upload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.continew.starter.core.exception.BadRequestException;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Shared upload / persisted-image URL validation for comment and partner-message flows.
 */
public final class UploadImageRules {

    public static final int MAX_IMAGES = 9;
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UploadImageRules() {}

    public static void validateImageUrls(List<String> imageUrls, String urlPrefix) {
        List<String> urls = imageUrls == null ? List.of() : imageUrls;
        if (urls.size() > MAX_IMAGES) {
            throw new BadRequestException("最多 " + MAX_IMAGES + " 张图片");
        }
        for (String url : urls) {
            if (url == null || url.isBlank() || !url.startsWith(urlPrefix)) {
                throw new BadRequestException("非法图片路径");
            }
        }
    }

    public static void validateFileSize(long size) {
        if (size > MAX_BYTES) {
            throw new BadRequestException("单张图片不能超过 5MB");
        }
    }

    public static void validateExtension(String filename) {
        String ext = extension(filename);
        if (ext.isEmpty() || !ALLOWED_EXT.contains(ext)) {
            throw new BadRequestException("仅支持 jpg、png、webp、gif 格式");
        }
    }

    public static String safeFilename(String original) {
        if (original == null || original.isBlank()) {
            throw new BadRequestException("文件名无效");
        }
        return Paths.get(original).getFileName().toString();
    }

    public static String resolveContentForSave(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String serializeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(imageUrls);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("图片路径格式无效");
        }
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }
}
