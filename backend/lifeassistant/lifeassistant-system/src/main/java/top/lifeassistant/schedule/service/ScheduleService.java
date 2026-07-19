package top.lifeassistant.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.schedule.mapper.ScheduleEventMapper;
import top.lifeassistant.schedule.mapper.ScheduleInviteMapper;
import top.lifeassistant.schedule.model.entity.ScheduleEventDO;
import top.lifeassistant.schedule.model.entity.ScheduleInviteDO;
import top.lifeassistant.schedule.model.req.ScheduleEventCreateReq;
import top.lifeassistant.schedule.model.req.ScheduleEventUpdateReq;
import top.lifeassistant.schedule.model.resp.ScheduleEventResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final Set<String> RECURRENCES = Set.of("none", "daily", "weekly");

    private final ScheduleEventMapper eventMapper;
    private final ScheduleInviteMapper inviteMapper;
    private final OwnerValidator ownerValidator;

    public ScheduleEventResp create(UserDO user, ScheduleEventCreateReq req) {
        String recurrence = normalizeRecurrence(req.getRecurrence());
        validateTimes(req.getStartAt(), req.getEndAt());
        validateRecurrence(recurrence, req.getStartAt(), req.getRecurrenceEndDate());

        ScheduleEventDO event = new ScheduleEventDO();
        event.setUserId(user.getId());
        event.setTitle(req.getTitle());
        event.setStartAt(req.getStartAt());
        event.setEndAt(req.getEndAt());
        event.setNote(req.getNote());
        event.setRecurrence(recurrence);
        event.setRecurrenceEndDate("none".equals(recurrence) ? null : req.getRecurrenceEndDate());
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdateTime(LocalDateTime.now());

        eventMapper.insert(event);
        return ScheduleEventResp.from(event);
    }

    public ScheduleEventResp update(UserDO user, String id, ScheduleEventUpdateReq req) {
        ScheduleEventDO event = ownerValidator.requireOwner(() -> eventMapper.selectById(id), user.getId());

        if (req.getTitle() != null) {
            event.setTitle(req.getTitle());
        }
        if (req.getStartAt() != null) {
            event.setStartAt(req.getStartAt());
        }
        if (req.getEndAt() != null) {
            event.setEndAt(req.getEndAt());
        }
        if (req.getNote() != null) {
            event.setNote(req.getNote());
        }
        if (req.getRecurrence() != null) {
            event.setRecurrence(req.getRecurrence());
            if ("none".equals(req.getRecurrence())) {
                event.setRecurrenceEndDate(null);
            }
        }
        if (req.getRecurrenceEndDate() != null) {
            event.setRecurrenceEndDate(req.getRecurrenceEndDate());
        }

        validateTimes(event.getStartAt(), event.getEndAt());
        validateRecurrence(
            normalizeRecurrence(event.getRecurrence()),
            event.getStartAt(),
            event.getRecurrenceEndDate()
        );

        event.setUpdateTime(LocalDateTime.now());
        eventMapper.updateById(event);
        return ScheduleEventResp.from(event);
    }

    @Transactional
    public void delete(UserDO user, String id) {
        ScheduleEventDO event = ownerValidator.requireOwner(() -> eventMapper.selectById(id), user.getId());

        inviteMapper.delete(new LambdaQueryWrapper<ScheduleInviteDO>()
            .eq(ScheduleInviteDO::getSourceEventId, id)
            .eq(ScheduleInviteDO::getStatus, "pending"));

        if (event.getLinkedEventId() != null) {
            eventMapper.update(null, new LambdaUpdateWrapper<ScheduleEventDO>()
                .eq(ScheduleEventDO::getId, event.getLinkedEventId())
                .set(ScheduleEventDO::getLinkedEventId, null));
        }
        eventMapper.update(null, new LambdaUpdateWrapper<ScheduleEventDO>()
            .eq(ScheduleEventDO::getLinkedEventId, id)
            .set(ScheduleEventDO::getLinkedEventId, null));

        eventMapper.deleteById(id);
    }

    public List<ScheduleEventResp> listMine(UserDO user, LocalDateTime from, LocalDateTime to) {
        assertRange(from, to);
        List<ScheduleEventResp> events = expandAll(findCandidates(user.getId(), from, to), from, to);
        attachPendingInvites(events, user.getId(), true);
        return events;
    }

    public List<ScheduleEventResp> listPartner(UserDO user, LocalDateTime from, LocalDateTime to) {
        if (user.getPartnerId() == null) {
            throw new BadRequestException("请先绑定伴侣");
        }
        assertRange(from, to);
        List<ScheduleEventResp> events = expandAll(findCandidates(user.getPartnerId(), from, to), from, to);
        attachPendingInvites(events, user.getId(), false);
        return events;
    }

    public void invite(UserDO user, String eventId) {
        if (user.getPartnerId() == null) {
            throw new BadRequestException("请先绑定伴侣");
        }
        ownerValidator.requireOwner(() -> eventMapper.selectById(eventId), user.getId());

        Long pending = inviteMapper.selectCount(new LambdaQueryWrapper<ScheduleInviteDO>()
            .eq(ScheduleInviteDO::getSourceEventId, eventId)
            .eq(ScheduleInviteDO::getStatus, "pending"));
        if (pending > 0) {
            throw new BadRequestException("已有待处理邀约");
        }

        LocalDateTime now = LocalDateTime.now();
        ScheduleInviteDO invite = new ScheduleInviteDO();
        invite.setSourceEventId(eventId);
        invite.setInviterUserId(user.getId());
        invite.setInviteeUserId(user.getPartnerId());
        invite.setStatus("pending");
        invite.setCreatedAt(now);
        inviteMapper.insert(invite);
    }

    @Transactional
    public ScheduleEventResp acknowledge(UserDO user, String inviteId, String action) {
        ScheduleInviteDO invite = ownerValidator.findAndCheck(
            () -> inviteMapper.selectById(inviteId),
            "资源不存在",
            i -> user.getId().equals(i.getInviteeUserId()));
        if (!"pending".equals(invite.getStatus())) {
            throw new BadRequestException("邀约已处理");
        }

        if (!"accept".equals(action) && !"reject".equals(action)) {
            throw new BadRequestException("无效操作，action 必须为 accept 或 reject");
        }

        LocalDateTime now = LocalDateTime.now();
        if ("reject".equals(action)) {
            invite.setStatus("rejected");
            invite.setRespondedAt(now);
            inviteMapper.updateById(invite);
            return null;
        }

        ScheduleEventDO source = eventMapper.selectById(invite.getSourceEventId());
        if (source == null) {
            throw new BadRequestException("源事件不存在");
        }

        ScheduleEventDO mirror = new ScheduleEventDO();
        mirror.setUserId(user.getId());
        mirror.setTitle(source.getTitle());
        mirror.setStartAt(source.getStartAt());
        mirror.setEndAt(source.getEndAt());
        mirror.setNote(source.getNote());
        mirror.setRecurrence("none");
        mirror.setRecurrenceEndDate(null);
        mirror.setLinkedEventId(source.getId());
        mirror.setCreatedAt(now);
        mirror.setUpdateTime(now);
        eventMapper.insert(mirror);

        source.setLinkedEventId(mirror.getId());
        source.setUpdateTime(now);
        eventMapper.updateById(source);

        invite.setStatus("accepted");
        invite.setRespondedAt(now);
        inviteMapper.updateById(invite);

        return ScheduleEventResp.from(mirror);
    }

    private void assertRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new BadRequestException("时间区间无效");
        }
        if (from.plusDays(62).isBefore(to)) {
            throw new BadRequestException("查询区间不能超过62天");
        }
    }

    private List<ScheduleEventDO> findCandidates(String userId, LocalDateTime from, LocalDateTime to) {
        LocalDate fromDate = from.toLocalDate();
        LambdaQueryWrapper<ScheduleEventDO> qw = new LambdaQueryWrapper<>();
        qw.eq(ScheduleEventDO::getUserId, userId);
        qw.and(w -> w
            .and(r -> r.in(ScheduleEventDO::getRecurrence, "daily", "weekly")
                .lt(ScheduleEventDO::getStartAt, to)
                .and(e -> e.isNull(ScheduleEventDO::getRecurrenceEndDate)
                    .or()
                    .ge(ScheduleEventDO::getRecurrenceEndDate, fromDate)))
            .or(n -> n.eq(ScheduleEventDO::getRecurrence, "none")
                .lt(ScheduleEventDO::getStartAt, to)
                .gt(ScheduleEventDO::getEndAt, from)));
        return eventMapper.selectList(qw);
    }

    private List<ScheduleEventResp> expandAll(
        List<ScheduleEventDO> candidates, LocalDateTime from, LocalDateTime to
    ) {
        return candidates.stream()
            .flatMap(e -> ScheduleRecurrenceExpander.expand(e, from, to).stream())
            .sorted(Comparator.comparing(ScheduleEventResp::getStartAt))
            .toList();
    }

    /** 批量挂载待处理邀约 ID（sourceEventId → invite.id）。 */
    private void attachPendingInvites(List<ScheduleEventResp> events, String userId, boolean mine) {
        if (events.isEmpty()) {
            return;
        }
        Set<String> eventIds = events.stream().map(ScheduleEventResp::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<ScheduleInviteDO> qw = new LambdaQueryWrapper<ScheduleInviteDO>()
            .eq(ScheduleInviteDO::getStatus, "pending")
            .in(ScheduleInviteDO::getSourceEventId, eventIds);
        if (mine) {
            qw.and(w -> w.eq(ScheduleInviteDO::getInviterUserId, userId)
                .or()
                .eq(ScheduleInviteDO::getInviteeUserId, userId));
        } else {
            qw.eq(ScheduleInviteDO::getInviteeUserId, userId);
        }
        Map<String, String> inviteBySource = inviteMapper.selectList(qw).stream()
            .collect(Collectors.toMap(ScheduleInviteDO::getSourceEventId, ScheduleInviteDO::getId, (a, b) -> a));
        for (ScheduleEventResp event : events) {
            String inviteId = inviteBySource.get(event.getId());
            if (inviteId != null) {
                event.setPendingInviteId(inviteId);
            }
        }
    }

    private String normalizeRecurrence(String recurrence) {
        return recurrence == null ? "none" : recurrence;
    }

    private void validateTimes(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BadRequestException("结束时间必须晚于开始时间");
        }
    }

    private void validateRecurrence(String recurrence, LocalDateTime startAt, LocalDate recurrenceEndDate) {
        if (!RECURRENCES.contains(recurrence)) {
            throw new BadRequestException("不支持的重复规则: " + recurrence);
        }
        if ("none".equals(recurrence)) {
            if (recurrenceEndDate != null) {
                throw new BadRequestException("非重复日程不能设置重复结束日期");
            }
            return;
        }
        if (recurrenceEndDate != null && recurrenceEndDate.isBefore(startAt.toLocalDate())) {
            throw new BadRequestException("重复结束日期不能早于开始日期");
        }
    }
}
