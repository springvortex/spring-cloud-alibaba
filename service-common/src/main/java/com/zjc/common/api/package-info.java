/**
 * 跨服务共享的 Feign API 契约包。
 *
 * <h2>约定</h2>
 * <ul>
 *   <li><b>跨服务共享的 API</b>：放在此包（{@code com.zjc.common.api}）下，
 *       按服务名分子包（如 {@code mail/}、{@code user/}、{@code test/}），
 *       所有依赖 common 的服务直接注入复用。</li>
 *   <li><b>服务私有的 API</b>：放在服务自己的本地包
 *       （如 {@code com.zjc.xxx.feign}）下，不进入 common。</li>
 * </ul>
 *
 * <h2>命名规范</h2>
 * <ul>
 *   <li>共享接口：{@code <Domain>FeignApi}（如 {@code MailFeignApi}、{@code UserFeignApi}）</li>
 *   <li>{@code contextId} 必填，用于隔离指向同一服务的多个客户端配置</li>
 *   <li>降级逻辑跟随接口一起放在子包 {@code factory/} 下</li>
 * </ul>
 *
 * <h2>子包结构</h2>
 * <pre>
 * com.zjc.common.api
 *   ├─ mail/          # 邮件服务 Feign 契约
 *   ├─ user/          # 用户服务 Feign 契约（含降级工厂）
 *   │   └─ factory/
 *   └─ test/          # 测试用 Feign 契约
 * </pre>
 *
 * @author jiancai.zhong
 */
package com.zjc.common.api;
