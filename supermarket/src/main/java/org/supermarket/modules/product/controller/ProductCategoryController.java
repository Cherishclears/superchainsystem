package org.supermarket.modules.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.result.Result;
import org.supermarket.modules.product.entity.ProductCategory;
import org.supermarket.modules.product.service.ProductCategoryService;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    @GetMapping("/list")
    public Result<List<ProductCategory>> list() {
        return Result.ok(categoryService.listAll());
    }

    @PostMapping
    public Result<String> add(@RequestBody ProductCategory category) {
        categoryService.save(category);
        return Result.ok("添加成功");
    }

    @PutMapping
    public Result<String> update(@RequestBody ProductCategory category) {
        categoryService.updateById(category);
        return Result.ok("修改成功");
    }
}