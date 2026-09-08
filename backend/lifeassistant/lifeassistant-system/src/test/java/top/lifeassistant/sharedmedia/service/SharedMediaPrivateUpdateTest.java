package top.lifeassistant.sharedmedia.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.sharedmedia.model.entity.SharedMediaDO;
import top.lifeassistant.system.model.entity.user.UserDO;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedMediaPrivateUpdateTest {

    @Test
    void partnerCanUpdateOtherFieldsWhenIsPrivateUnchanged() {
        SharedMediaDO media = media(false, "creator");
        UserDO partner = user("partner");

        assertDoesNotThrow(() -> SharedMediaService.applyIsPrivateUpdate(media, partner, false));
        assertFalse(media.getIsPrivate());
    }

    @Test
    void partnerCannotTogglePrivate() {
        SharedMediaDO media = media(false, "creator");
        UserDO partner = user("partner");

        assertThrows(
            BadRequestException.class,
            () -> SharedMediaService.applyIsPrivateUpdate(media, partner, true)
        );
    }

    @Test
    void creatorCanTogglePrivate() {
        SharedMediaDO media = media(true, "creator");
        UserDO creator = user("creator");

        SharedMediaService.applyIsPrivateUpdate(media, creator, false);
        assertFalse(media.getIsPrivate());
    }

    private static SharedMediaDO media(boolean isPrivate, String createdBy) {
        SharedMediaDO media = new SharedMediaDO();
        media.setIsPrivate(isPrivate);
        media.setCreatedBy(createdBy);
        return media;
    }

    private static UserDO user(String id) {
        UserDO user = new UserDO();
        user.setId(id);
        return user;
    }
}
