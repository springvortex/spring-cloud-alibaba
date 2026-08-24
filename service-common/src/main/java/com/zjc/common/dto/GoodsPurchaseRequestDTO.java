package com.zjc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品购买请求 DTO。
 *
 * @author jiancai.zhong
 */
@Schema(description = "商品购买请求")
@Data
public class GoodsPurchaseRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -7634053945614089591L;

    @Schema(description = "购买用户ID", example = "1")
    @NotNull(message = "购买用户ID不能为空")
    @Min(value = 1, message = "购买用户ID必须大于0")
    private Long userId;

    @Schema(description = "购买数量", example = "1")
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    @Max(value = 100, message = "购买数量不能超过100")
    private Integer quantity;
}
