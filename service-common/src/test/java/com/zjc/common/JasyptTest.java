package com.zjc.common;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Jasypt 加密工具，用于将明文加密为 {@code ENC(密文)} 格式。
 *
 * <p>运行此测试类需要提供加密密钥，支持两种方式（优先级从高到低）：
 * <ol>
 *   <li>命令行参数：{@code -Djasypt.encryptor.password=your-secret}</li>
 *   <li>环境变量：{@code JASYPT_ENCRYPTOR_PASSWORD=your-secret}</li>
 * </ol>
 *
 * <p>使用步骤：
 * <ol>
 *   <li>通过命令行或环境变量传入密钥后运行 main 方法</li>
 *   <li>输入需要加密的明文（如数据库密码、SMTP 密码等）</li>
 *   <li>将输出的 {@code ENC(xxx)} 字符串粘贴到业务服务的环境 Profile 中</li>
 * </ol>
 *
 * <p>IDEA 运行：在 Run Configuration 的 VM Options 中添加
 * {@code -Djasypt.encryptor.password=your-secret}
 *
 * @author jiancai.zhong
 */
public class JasyptTest {

    /**
     * 加密算法，与各服务的 config/application-jasypt.yaml 保持一致。
     */
    private static final String ALGORITHM = "PBEWithHMACSHA512AndAES_256";

    /**
     * 输出前缀，与 Jasypt 默认 {@code ENC(} 格式一致。
     */
    private static final String ENC_PREFIX = "ENC(";

    /**
     * 输出后缀。
     */
    private static final String ENC_SUFFIX = ")";

    /**
     * 从系统属性或环境变量获取加密密钥。
     *
     * @return 加密密钥
     * @throws IllegalStateException 密钥未提供时抛出
     */
    private static String resolvePassword() {
        // 优先从系统属性读取（命令行 -D 参数）
        String password = System.getProperty("jasypt.encryptor.password");
        if (password != null && !password.isBlank()) {
            return password;
        }
        // 其次从环境变量读取
        password = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        if (password != null && !password.isBlank()) {
            return password;
        }
        throw new IllegalStateException(
                "加密密钥未提供。请通过以下方式之一传入：\n" + "  命令行: -Djasypt.encryptor.password=your-secret\n" + "  环境变量: JASYPT_ENCRYPTOR_PASSWORD=your-secret"
        );
    }

    /**
     * 构建 Jasypt 加密器。
     *
     * @param password 加密密钥
     * @return 配置好的加密器
     */
    private static PooledPBEStringEncryptor createEncryptor(String password) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm(ALGORITHM);
        config.setKeyObtentionIterations("100000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    /**
     * 交互式加密工具入口。
     *
     * @param args 命令行参数（未使用，密钥通过 -D 系统属性传入）
     */
    public static void main(String[] args) {
        String password;
        try {
            password = resolvePassword();
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return;
        }

        PooledPBEStringEncryptor encryptor = createEncryptor(password);

        System.out.println("======================================");
        System.out.println("  Jasypt 加密工具");
        System.out.println("  算法: " + ALGORITHM);
        System.out.println("======================================");
        System.out.print("请输入要加密的明文: ");

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        String plainText = scanner.nextLine();

        String encrypted = encryptor.encrypt(plainText);
        String wrapped = ENC_PREFIX + encrypted + ENC_SUFFIX;

        System.out.println();
        System.out.println("明文: " + plainText);
        System.out.println("密文: " + wrapped);
        System.out.println();

        // 验证：解密回来确认一致
        String decrypted = encryptor.decrypt(encrypted);
        System.out.println("验证解密: " + decrypted);
        System.out.println("匹配: " + plainText.equals(decrypted));
        System.out.println();
        System.out.println("将上面的 ENC(xxx) 复制到业务服务的环境 Profile 中即可。");
    }
}
