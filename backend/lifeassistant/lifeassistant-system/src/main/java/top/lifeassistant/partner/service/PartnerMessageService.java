package top.lifeassistant.partner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.component.UploadStorage;
import top.lifeassistant.partner.mapper.PartnerMessageMapper;
import top.lifeassistant.partner.model.entity.PartnerMessageDO;
import top.lifeassistant.partner.model.req.PartnerMessageCreateReq;
import top.lifeassistant.partner.model.resp.PartnerMessageImagesUploadResp;
import top.lifeassistant.partner.model.resp.PartnerMessageResp;
import top.lifeassistant.sharedrecord.model.req.SharedRecordCreateReq;
import top.lifeassistant.sharedrecord.model.resp.SharedRecordResp;
import top.lifeassistant.sharedrecord.service.SharedRecordService;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.todo.model.req.TodoCreateReq;
import top.lifeassistant.todo.model.resp.TodoResp;
import top.lifeassistant.todo.service.TodoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerMessageService {

    private final PartnerMessageMapper mapper;
    private final UploadStorage uploadStorage;
    private final SharedRecordService sharedRecordService;
    private final TodoService todoService;
    private final PartnerPointsService partnerPointsService;

    private void requirePartner(UserDO user) {
        if (user.getPartnerId() == null) {
            throw new BadRequestException("请先绑定伴侣");
        }
    }

    public List<PartnerMessageResp> list(UserDO user, int page, int size) {
        requirePartner(user);
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        Page<PartnerMessageDO> mp = new Page<>(p, s);
        LambdaQueryWrapper<PartnerMessageDO> qw = new LambdaQueryWrapper<>();
        qw.in(PartnerMessageDO::getCreatedBy, List.of(user.getId(), user.getPartnerId()))
            .orderByAsc(PartnerMessageDO::getCreatedAt);
        return mapper.selectPage(mp, qw).getRecords().stream()
            .map(PartnerMessageResp::from).toList();
    }

    public PartnerMessageImagesUploadResp uploadImages(UserDO user, MultipartFile[] files) throws IOException {
        requirePartner(user);
        PartnerMessageImageRules.validateUploadFiles(files);
        Path dir = uploadStorage.partnerMessagesDir();
        Files.createDirectories(dir);
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = UUID.randomUUID() + "_" + PartnerMessageImageRules.safeFilename(file);
            Path filePath = dir.resolve(filename);
            file.transferTo(filePath.toFile());
            urls.add(uploadStorage.partnerMessageUrl(filename));
        }
        return PartnerMessageImagesUploadResp.builder().urls(urls).build();
    }

    @Transactional
    public PartnerMessageResp create(UserDO user, PartnerMessageCreateReq req) {
        requirePartner(user);
        PartnerMessageImageRules.validateCreatePayload(req.getContent(), req.getImageUrls());
        boolean pubShared = Boolean.TRUE.equals(req.getPublishSharedRecord());
        boolean pubTodo = Boolean.TRUE.equals(req.getPublishTodo());
        boolean pubPoints = Boolean.TRUE.equals(req.getPublishPoints());
        PartnerMessageImageRules.validatePublishFlags(req.getContent(), req.getImageUrls(),
            pubShared, pubTodo, pubPoints);

        PartnerMessageDO msg = new PartnerMessageDO();
        msg.setId(UUID.randomUUID().toString());
        msg.setCreatedBy(user.getId());
        msg.setContent(PartnerMessageImageRules.resolveContentForSave(req.getContent()));
        msg.setImageUrls(PartnerMessageImageRules.serializeImageUrls(req.getImageUrls()));
        msg.setCreatedAt(LocalDateTime.now());
        mapper.insert(msg);
        if (pubShared) {
            SharedRecordCreateReq sr = new SharedRecordCreateReq();
            sr.setTitle(PartnerMessageImageRules.truncateTitle(msg.getContent(), 255));
            SharedRecordResp created = sharedRecordService.create(user, sr);
            msg.setSharedRecordId(created.getId());
        }
        if (pubTodo) {
            String assign = req.getTodoAssignedTo() == null ? "none" : req.getTodoAssignedTo();
            TodoCreateReq tr = new TodoCreateReq();
            tr.setTitle(PartnerMessageImageRules.truncateTitle(msg.getContent(), 255));
            tr.setPriority("medium");
            if ("partner".equals(assign)) {
                tr.setAssignedTo(user.getPartnerId());
            }
            TodoResp created = todoService.create(user, tr);
            msg.setTodoId(created.getId());
        }
        if (pubPoints) {
            if (req.getPointsChange() == null || req.getPointsChange() == 0) {
                throw new BadRequestException("积分变更不能为 0");
            }
            String reason = (req.getPointsReason() == null || req.getPointsReason().isBlank())
                ? msg.getContent() : req.getPointsReason().trim();
            String pointsId = partnerPointsService.addPoints(
                user.getId(), req.getPointsChange(), reason, LocalDate.now());
            msg.setPointsId(pointsId);
        }
        if (pubShared || pubTodo || pubPoints) {
            mapper.updateById(msg);
        }
        return PartnerMessageResp.from(msg);
    }

    public void delete(UserDO user, String id) {
        requirePartner(user);
        PartnerMessageDO msg = mapper.selectById(id);
        if (msg == null) {
            throw new BadRequestException("留言不存在");
        }
        if (!user.getId().equals(msg.getCreatedBy())) {
            throw new BadRequestException("只能删除自己的留言");
        }
        mapper.deleteById(id);
    }

    public void deleteByCreatedBy(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<PartnerMessageDO>()
            .in(PartnerMessageDO::getCreatedBy, userId1, userId2));
    }
}
