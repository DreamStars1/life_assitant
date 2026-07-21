package top.lifeassistant.sharedmedia.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BadRequestException;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaCommentImageRulesTest {

    @Test
    void rejectsEmptyComment() {
        BadRequestException ex = assertThrows(
            BadRequestException.class,
            () -> MediaCommentImageRules.validateCreateComment(null, null)
        );
        assertEquals("评论不能为空", ex.getMessage());
    }

    @Test
    void rejectsMoreThanNine() {
        List<String> urls = IntStream.range(0, 10)
            .mapToObj(i -> MediaCommentImageRules.COMMENT_IMAGE_PREFIX + i + ".jpg")
            .toList();

        BadRequestException ex = assertThrows(
            BadRequestException.class,
            () -> MediaCommentImageRules.validateCreateComment(null, urls)
        );
        assertEquals("最多 9 张图片", ex.getMessage());
    }

    @Test
    void rejectsExternalUrl() {
        BadRequestException ex = assertThrows(
            BadRequestException.class,
            () -> MediaCommentImageRules.validateCreateComment(
                null,
                List.of("https://example.com/a.jpg")
            )
        );
        assertEquals("非法图片路径", ex.getMessage());
    }

    @Test
    void acceptsValidPrefix() {
        assertDoesNotThrow(() -> MediaCommentImageRules.validateCreateComment(
            "  hello  ",
            List.of(MediaCommentImageRules.COMMENT_IMAGE_PREFIX + "a.jpg")
        ));
        assertEquals("hello", MediaCommentImageRules.resolveContentForSave("  hello  "));
        assertEquals(
            "[\"/uploads/media-comments/a.jpg\"]",
            MediaCommentImageRules.serializeImageUrls(List.of(MediaCommentImageRules.COMMENT_IMAGE_PREFIX + "a.jpg"))
        );
        assertEquals(null, MediaCommentImageRules.resolveContentForSave(null));
    }
}
