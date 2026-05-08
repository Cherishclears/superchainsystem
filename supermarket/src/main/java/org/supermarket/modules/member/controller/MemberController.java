package org.supermarket.modules.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.result.Result;
import org.supermarket.modules.member.dto.MemberDTO;
import org.supermarket.modules.member.entity.Member;
import org.supermarket.modules.member.service.MemberService;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 注册会员
    @PostMapping("/register")
    public Result<Member> register(@Valid @RequestBody MemberDTO dto) {
        return Result.ok(memberService.register(dto));
    }

    // 根据手机号查询
    @GetMapping("/phone/{phone}")
    public Result<Member> getByPhone(@PathVariable String phone) {
        Member member = memberService.getOne(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getPhone, phone)
                        .eq(Member::getStatus, 1));
        if (member == null) return Result.fail("会员不存在");
        return Result.ok(member);
    }

    // 分页查询
    @GetMapping("/page")
    public Result<Page<Member>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String realName) {
        return Result.ok(memberService.pageMember(pageNum, pageSize, phone, realName));
    }

    // 查询会员详情
    @GetMapping("/{id}")
    public Result<Member> getById(@PathVariable Long id) {
        return Result.ok(memberService.getById(id));
    }


}
