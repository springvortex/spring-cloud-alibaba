package com.zjc.provider.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjc.common.dto.GoodsDTO;
import com.zjc.common.web.ApiResponse;
import com.zjc.provider.entity.Goods;
import com.zjc.provider.service.GoodsService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
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
 *
 * @author jiancai.zhong
 */
@RestController
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    /**
     * 根据ID查询单个商品。
     *
     * @param id 商品主键
     * @return 商品信息；记录不存在时 data 为 null
     */
    @GetMapping("/goods/{id}")
    public ApiResponse<GoodsDTO> getGoods(@PathVariable("id") Long id) {
        return ApiResponse.success(toDTO(goodsService.getById(id)));
    }

    /**
     * 查询全部有效商品。
     *
     * <p>逻辑删除的记录会被 MyBatis-Plus 自动过滤（依赖 logic-delete 配置），
     * 返回结果仅包含 is_deleted = 0 的商品。
     *
     * @return 商品列表，已按 DTO 过滤内部字段；无数据时返回空列表
     */
    @GetMapping("/goods/list")
    public ApiResponse<List<GoodsDTO>> list() {
        List<GoodsDTO> list = goodsService.list().stream().map(this::toDTO).toList();
        return ApiResponse.success(list);
    }

    /**
     * 分页查询有效商品。
     *
     * <p>依赖分页拦截器自动改写 SQL，返回带总数与分页信息的 {@link Page}。
     * 逻辑删除记录同样会被自动过滤。
     *
     * @param current 当前页码，从 1 开始，默认 1
     * @param size    每页条数，默认 10
     * @return 分页结果，records 已转为 DTO
     */
    @GetMapping("/goods/page")
    public ApiResponse<Page<GoodsDTO>> page(
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        Page<Goods> page = goodsService.page(new Page<>(current, size));
        Page<GoodsDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toDTO).toList());
        return ApiResponse.success(result);
    }

    /**
     * 新增商品。
     *
     * <p>DTO 转实体后入库，数据库自增主键会回填到实体，
     * 因此返回的 DTO 含生成后的 goodsId，调用方无需再查一次。
     *
     * @param dto 商品信息，无需传 goodsId、createTime 等系统生成字段
     * @return 含生成主键的完整商品信息
     */
    @PostMapping("/goods")
    public ApiResponse<GoodsDTO> add(@RequestBody GoodsDTO dto) {
        Goods goods = new Goods();
        BeanUtils.copyProperties(dto, goods);
        goodsService.save(goods);
        return ApiResponse.success(toDTO(goods));
    }

    /**
     * 根据ID修改商品。
     *
     * <p>基于 MyBatis-Plus 的 updateById，按 goodsId 定位记录。
     * 默认只更新 DTO 中非 null 的字段，传 null 的字段保持原值不变。
     *
     * @param dto 待更新信息，必须携带 goodsId
     * @return 无业务数据
     */
    @PutMapping("/goods")
    public ApiResponse<Void> update(@RequestBody GoodsDTO dto) {
        Goods goods = new Goods();
        BeanUtils.copyProperties(dto, goods);
        goodsService.updateById(goods);
        return ApiResponse.success();
    }

    /**
     * 根据ID删除商品。
     *
     * <p>执行逻辑删除：将 is_deleted 置为 1，而非物理删除记录。
     *
     * @param id 商品主键
     * @return 无业务数据
     */
    @DeleteMapping("/goods/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        goodsService.removeById(id);
        return ApiResponse.success();
    }

    /**
     * 实体转对外 DTO。
     *
     * <p>同名属性自动拷贝；isDeleted、updateTime 等内部字段因 DTO 不含，
     * 自然被忽略，从而实现内部字段不对外暴露。
     *
     * @param goods 商品实体，允许为 null
     * @return 对外 DTO；入参为 null 时返回 null
     */
    private GoodsDTO toDTO(Goods goods) {
        if (goods == null) {
            return null;
        }
        GoodsDTO dto = new GoodsDTO();
        BeanUtils.copyProperties(goods, dto);
        return dto;
    }
}