package com.zjc.provider.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.GoodsDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.converter.GoodsConverter;
import com.zjc.provider.entity.Goods;
import com.zjc.provider.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品管理 REST 接口。
 *
 * <p>对外统一返回 {@link GoodsDTO}，{@link Goods} 实体不直接暴露，
 * 避免数据库结构（逻辑删除字段、更新时间等）泄露到接口契约中。
 * 所有方法返回值包装在 {@link ApiResponse} 里，保证响应结构一致。
 *
 * <p>CRUD 直接复用 {@link GoodsService}（继承 MyBatis-Plus 的 IService）
 * 自带的能力，无需在 service 层重复定义通用方法。
 * Entity <-> DTO 转换使用 {@link GoodsConverter}（MapStruct 编译期生成，零反射）。
 *
 * @author jiancai.zhong
 */
@Tag(name = "商品管理", description = "商品的增删改查")
@RestController
@Validated
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    @Resource
    private GoodsConverter goodsConverter;

    @Operation(summary = "根据ID查询单个商品")
    @GetMapping("/goods/{id}")
    public ApiResponse<GoodsDTO> getGoods(
            @Parameter(description = "商品主键") @PathVariable("id") Long id) {
        return ApiResponse.success(goodsConverter.entityToDto(goodsService.getById(id)));
    }

    @Operation(summary = "查询全部有效商品")
    @GetMapping("/goods/list")
    public ApiResponse<List<GoodsDTO>> list() {
        return ApiResponse.success(goodsConverter.entityListToDtoList(goodsService.list()));
    }

    @Operation(summary = "分页查询有效商品")
    @GetMapping("/goods/page")
    public ApiResponse<Page<GoodsDTO>> page(
            @Parameter(description = "当前页码，从1开始")
            @Min(value = 1, message = "当前页码必须从1开始")
            @RequestParam(value = "current", defaultValue = "1") long current,
            @Parameter(description = "每页条数，范围1-100")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100")
            @RequestParam(value = "size", defaultValue = "10") long size) {
        Page<Goods> page = goodsService.page(new Page<>(current, size));
        Page<GoodsDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(goodsConverter.entityListToDtoList(page.getRecords()));
        return ApiResponse.success(result);
    }

    @Operation(summary = "新增商品")
    @PostMapping("/goods")
    public ApiResponse<GoodsDTO> add(@Valid @RequestBody GoodsDTO dto) {
        Goods goods = goodsConverter.dtoToEntity(dto);
        goodsService.save(goods);
        return ApiResponse.success(goodsConverter.entityToDto(goods));
    }

    @Operation(summary = "根据ID修改商品")
    @PutMapping("/goods")
    public ApiResponse<Void> update(@Valid @RequestBody GoodsDTO dto) {
        boolean updated = goodsService.updateById(goodsConverter.dtoToEntity(dto));
        return updated ? ApiResponse.success() : ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
    }

    @Operation(summary = "根据ID删除商品（逻辑删除）")
    @DeleteMapping("/goods/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "商品主键") @PathVariable("id") Long id) {
        boolean removed = goodsService.removeById(id);
        return removed ? ApiResponse.success() : ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
    }
}
