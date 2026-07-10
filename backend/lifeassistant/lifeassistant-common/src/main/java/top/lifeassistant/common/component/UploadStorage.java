package top.lifeassistant.common.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件上传根目录。上传与静态资源映射共用同一绝对路径。
 */
@Component
public class UploadStorage {

    private static final String SHARED_MEDIA_SUBDIR = "shared-media";

    private final Path root;

    public UploadStorage(@Value("${lifeassistant.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path sharedMediaDir() {
        return root.resolve(SHARED_MEDIA_SUBDIR);
    }

    public String sharedMediaUrl(String filename) {
        return "/uploads/" + SHARED_MEDIA_SUBDIR + "/" + filename;
    }

    public String resourceLocation() {
        String location = root.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
