package org.supermarket.modules.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.supermarket.common.exception.BusinessException;
import org.supermarket.modules.member.dto.MemberDTO;
import org.supermarket.modules.member.entity.Member;
import org.supermarket.modules.member.entity.MemberLevel;
import org.supermarket.modules.member.entity.PointsRecord;
import org.supermarket.modules.member.mapper.MemberLevelMapper;
import org.supermarket.modules.member.mapper.MemberMapper;
import org.supermarket.modules.member.mapper.PointsRecordMapper;
import org.supermarket.modules.member.service.MemberService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member>
        implements MemberService {

    private final MemberLevelMapper levelMapper;
    private final PointsRecordMapper pointsRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member register(MemberDTO dto) {
        // 手机号唯一校验
        long count = count(new LambdaQueryWrapper<Member>()
                .eq(Member::getPhone, dto.getPhone()));
        if (count > 0) throw new BusinessException("该手机号已注册");

        Member member = new Member();
        member.setMemberNo("M" + System.currentTimeMillis());
        member.setPhone(dto.getPhone());
        member.setRealName(dto.getRealName());
        member.setGender(dto.getGender() != null ? dto.getGender() : 0);
        member.setBirthday(dto.getBirthday());
        member.setLevelId(1L); // 默认普通会员
        member.setPoints(0);
        member.setTotalPoints(0);
        member.setTotalAmount(java.math.BigDecimal.ZERO);
        member.setRegisterStore(dto.getRegisterStore());
        member.setStatus(1);
        member.setCreateTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        member.setIsDeleted(0);
        save(member);
        return member;
    }

    @Override
    public Member getByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getPhone, phone)
                .eq(Member::getIsDeleted, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long memberId, Integer points, String orderNo) {
        Member member = getById(memberId);
        if (member == null) throw new BusinessException("会员不存在");

        // 更新积分
        member.setPoints(member.getPoints() + points);
        member.setTotalPoints(member.getTotalPoints() + points);
        member.setUpdateTime(LocalDateTime.now());
        updateById(member);

        // 记录积分流水
        PointsRecord record = new PointsRecord();
        record.setMemberId(memberId);
        record.setChangeType(1); // 消费获得
        record.setPoints(points);
        record.setRefOrderNo(orderNo);
        record.setRemark("消费获得积分");
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);

        // 检查是否需要升级
        checkAndUpgradeLevel(member);
    }

    @Override
    public Page<Member> pageMember(int pageNum, int pageSize,
                                   String phone, String realName) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Member::getIsDeleted, 0);
        wrapper.eq(StringUtils.hasText(phone), Member::getPhone, phone);
        wrapper.like(StringUtils.hasText(realName), Member::getRealName, realName);
        wrapper.orderByDesc(Member::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    // 检查并升级会员等级
    private void checkAndUpgradeLevel(Member member) {
        List<MemberLevel> levels = levelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>()
                        .orderByDesc(MemberLevel::getMinAmount));
        for (MemberLevel level : levels) {
            if (member.getTotalAmount().compareTo(level.getMinAmount()) >= 0) {
                if (!level.getId().equals(member.getLevelId())) {
                    member.setLevelId(level.getId());
                    updateById(member);
                    log.info("会员[{}]升级为：{}", member.getMemberNo(), level.getLevelName());
                }
                break;
            }
        }
    }
}