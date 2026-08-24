package top.lifeassistant.partner.service;

import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.upload.UploadImageRules;

import java.util.List;
import java.util.Set;

final class PartnerMessageImageRules {

    static final String MESSAGE_IMAGE_PREFIX = "/uploads/partner-messages/";
    static final int MAX_IMAGES = UploadImageRules.MAX_IMAGES;
    static final long MAX_BYTES = UploadImageRules.MAX_BYTES;
    static final Set<String> ALLOWED_EXT = UploadImageRules.ALLOWED_EXT;

    private PartnerMessageImageRules() {}

    static void validateCreatePayload(String content, List<String> imageUrls) {
        String trimmed = content == null ? "" : content.trim();
        List<String> urls = imageUrls == null ? List.of() : imageUrls;
        if (trimmed.isEmpty() && urls.isEmpty()) {
            throw new BadRequestException("留言不能为空");
        }
        if (!trimmed.isEmpty() && !urls.isEmpty()) {
            throw new BadRequestException("文字与图片不能同时发送");
        }
        UploadImageRules.validateImageUrls(urls, MESSAGE_IMAGE_PREFIX);
    }

    static void validatePublishFlags(String content, List<String> imageUrls,
            boolean shared, boolean todo, boolean points) {
        List<String> urls = imageUrls == null ? List.of() : imageUrls;
        if (!urls.isEmpty() && (shared || todo || points)) {
            throw new BadRequestException("图片留言不支持同步记事/待办/积分");
        }
        if ((shared || todo || points) && (content == null || content.isBlank())) {
            throw new BadRequestException("同步操作需要文字内容");
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
            if (file == null || file.isEmpty()) {
                throw new BadRequestException("图片不能为空");
            }
            UploadImageRules.validateFileSize(file.getSize());
            UploadImageRules.validateExtension(file.getOriginalFilename());
        }
    }

    static String safeFilename(MultipartFile file) {
        return UploadImageRules.safeFilename(file.getOriginalFilename());
    }

    static String resolveContentForSave(String content) {
        return UploadImageRules.resolveContentForSave(content);
    }

    static String serializeImageUrls(List<String> imageUrls) {
        return UploadImageRules.serializeImageUrls(imageUrls);
    }

    static String truncateTitle(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
