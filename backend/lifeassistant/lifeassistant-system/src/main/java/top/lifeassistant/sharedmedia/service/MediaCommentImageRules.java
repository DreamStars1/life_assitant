package top.lifeassistant.sharedmedia.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BadRequestException;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

final class MediaCommentImageRules {

    static final String COMMENT_IMAGE_PREFIX = "/uploads/media-comments/";
    static final int MAX_IMAGES = 9;
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MediaCommentImageRules() {}

    static void validateCreateComment(String content, List<String> imageUrls) {
        String trimmed = content == null ? "" : content.trim();
        List<String> urls = imageUrls == null ? List.of() : imageUrls;

        if (trimmed.isEmpty() && urls.isEmpty()) {
            throw new BadRequestException("评论不能为空");
        }
        if (urls.size() > MAX_IMAGES) {
            throw new BadRequestException("最多 " + MAX_IMAGES + " 张图片");
        }
        for (String url : urls) {
            if (url == null || url.isBlank() || !url.startsWith(COMMENT_IMAGE_PREFIX)) {
                throw new BadRequestException("非法图片路径");
            }
        }
    }

    static void validateUploadFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BadRequestException("请选择图片");
        }
        if (files.length > MAX_IMAGES) {
            throw new BadRequestException("最多 " + MAX_IMAGES + " 张图片");
        }
        for (MultipartFile file : files) {
            validateUploadFile(file);
        }
    }

    static String safeFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new BadRequestException("文件名无效");
        }
        return Paths.get(original).getFileName().toString();
    }

    static String resolveContentForSave(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String serializeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(imageUrls);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("图片路径格式无效");
        }
    }

    private static void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("图片不能为空");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("单张图片不能超过 5MB");
        }
        String ext = extension(file.getOriginalFilename());
        if (ext.isEmpty() || !ALLOWED_EXT.contains(ext)) {
            throw new BadRequestException("仅支持 jpg、png、webp、gif 格式");
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
