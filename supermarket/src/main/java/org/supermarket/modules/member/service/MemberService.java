package org.supermarket.modules.member.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.supermarket.modules.member.dto.MemberDTO;
import org.supermarket.modules.member.entity.Member;

public interface MemberService extends IService<Member> {

    // 注册会员
    Member register(MemberDTO dto);

    // 根据手机号查询会员
    Member getByPhone(String phone);

    // 消费后更新积分和等级
    void addPoints(Long memberId, Integer points, String orderNo);

    // 分页查询
    Page<Member> pageMember(int pageNum, int pageSize, String phone, String realName);
}