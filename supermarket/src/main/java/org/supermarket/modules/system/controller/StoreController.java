package org.supermarket.modules.system.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.result.Result;
import org.supermarket.modules.system.entity.Store;
import org.supermarket.modules.system.service.StoreService;

import java.util.List;

@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // 查询所有门店（总部用）
    @GetMapping("/list")
    public Result<List<Store>> list() {
        return Result.ok(storeService.list());
    }

    // 新增门店
    @PostMapping
    public Result<String> add(@RequestBody Store store) {
        storeService.save(store);
        return Result.ok("添加成功");
    }

    // 修改门店
    @PutMapping
    public Result<String> update(@RequestBody Store store) {
        storeService.updateById(store);
        return Result.ok("修改成功");
    }
}