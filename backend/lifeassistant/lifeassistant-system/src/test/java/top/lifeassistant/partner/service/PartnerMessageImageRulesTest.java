package top.lifeassistant.partner.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BadRequestException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartnerMessageImageRulesTest {

    @Test
    void rejectsEmpty() {
        assertThrows(BadRequestException.class,
            () -> PartnerMessageImageRules.validateCreatePayload(null, null));
    }

    @Test
    void rejectsTextAndImagesTogether() {
        assertThrows(BadRequestException.class,
            () -> PartnerMessageImageRules.validateCreatePayload("hi",
                List.of(PartnerMessageImageRules.MESSAGE_IMAGE_PREFIX + "a.jpg")));
    }

    @Test
    void rejectsPublishFlagsOnImageOnly() {
        assertThrows(BadRequestException.class,
            () -> PartnerMessageImageRules.validatePublishFlags(
                null, List.of(PartnerMessageImageRules.MESSAGE_IMAGE_PREFIX + "a.jpg"),
                true, false, false));
    }

    @Test
    void truncatesTitle() {
        String t = "x".repeat(300);
        assertEquals(255, PartnerMessageImageRules.truncateTitle(t, 255).length());
    }
}
