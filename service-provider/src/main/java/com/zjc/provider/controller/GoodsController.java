package com.zjc.provider.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.dto.GoodsDTO;
import com.zjc.common.dto.GoodsPurchaseRequestDTO;
import com.zjc.common.dto.GoodsPurchaseResponseDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.converter.GoodsConverter;
import com.zjc.provider.entity.Goods;
import com.zjc.provider.service.GoodsService;
import com.zjc.provider.service.GoodsPurchaseService;
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
    private GoodsPurchaseService goodsPurchaseService;

    @Resource
    private GoodsConverter goodsConverter;

    /**
     * 查询单个有效商品，不存在时 data 为 null。
     *
     * @param id 商品主键
     * @return 商品 DTO
     */
    @Operation(summary = "根据ID查询单个商品")
    @GetMapping("/goods/{id}")
    public ApiResponse<GoodsDTO> getGoods(
            @Parameter(description = "商品主键") @PathVariable("id") Long id) {
        return ApiResponse.success(goodsService.getGoods(id));
    }

    /**
     * 查询全部有效商品。
     *
     * @return 商品 DTO 列表
     */
    @Operation(summary = "查询全部有效商品")
    @GetMapping("/goods/list")
    public ApiResponse<List<GoodsDTO>> list() {
        return ApiResponse.success(goodsConverter.entityListToDtoList(goodsService.list()));
    }

    /**
     * 分页查询有效商品。
     *
     * @param current 页码，从 1 开始
     * @param size    每页数量，范围 1-100
     * @return 商品分页结果
     */
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

    /**
     * 新增商品，并返回数据库回填主键后的 DTO。
     *
     * @param dto 商品信息
     * @return 新增后的商品 DTO
     */
    @Operation(summary = "新增商品")
    @PostMapping("/goods")
    public ApiResponse<GoodsDTO> add(@Valid @RequestBody GoodsDTO dto) {
        Goods goods = goodsConverter.dtoToEntity(dto);
        goodsService.save(goods);
        return ApiResponse.success(goodsConverter.entityToDto(goods));
    }

    /**
     * 购买指定商品。
     *
     * <p>服务层会按商品维度获取分布式锁，并在事务内完成扣库存和创建订单。
     *
     * @param id      商品主键
     * @param request 购买请求
     * @return 订单与剩余库存信息
     */
    @Operation(summary = "购买商品", description = "按商品维度使用分布式锁扣减库存并创建待支付订单")
    @PostMapping("/goods/{id}/purchase")
    public ApiResponse<GoodsPurchaseResponseDTO> purchase(
            @Parameter(description = "商品主键") @PathVariable("id") Long id,
            @Valid @RequestBody GoodsPurchaseRequestDTO request) {
        return ApiResponse.success("购买成功", goodsPurchaseService.purchase(id, request));
    }

    /**
     * 根据商品 ID 修改有效商品。
     *
     * @param dto 商品信息，必须包含商品主键
     * @return 修改成功返回成功响应，商品不存在返回 NOT_FOUND
     */
    @Operation(summary = "根据ID修改商品")
    @PutMapping("/goods")
    public ApiResponse<Void> update(@Valid @RequestBody GoodsDTO dto) {
        boolean updated = goodsService.updateGoods(goodsConverter.dtoToEntity(dto));
        return updated ? ApiResponse.success() : ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
    }

    /**
     * 逻辑删除指定商品。
     *
     * @param id 商品主键
     * @return 删除成功返回成功响应，商品不存在返回 NOT_FOUND
     */
    @Operation(summary = "根据ID删除商品（逻辑删除）")
    @DeleteMapping("/goods/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "商品主键") @PathVariable("id") Long id) {
        boolean removed = goodsService.deleteGoods(id);
        return removed ? ApiResponse.success() : ApiResponse.failure(ApiResponseEnum.NOT_FOUND);
    }
}
