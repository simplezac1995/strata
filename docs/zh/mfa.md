# TOTP 多因素认证（MFA）

> 按用户开启的一次性口令第二因素，框架内置（无需额外模块）：管理端完成绑定，登录时返回 MFA challenge 而不是会话，
> 密钥加密存储。完整细节见 [英文 MFA 指南](../mfa.md)。

## 已提供能力

- RFC 6238 TOTP（HMAC-SHA1、6 位、30 秒、前后各 1 步时钟偏移），无第三方 TOTP 依赖。
- 绑定：管理端展示二维码（服务端生成的 PNG data URL）+ 手动 Base32 密钥。
- **登录二次校验**：开启 MFA 后 `POST /auth/login` 返回 `mfaRequired=true` 与短期 `mfaToken`（tokens 为 null）；
  前端调用 `POST /auth/mfa/verify`（challenge + 6 位码）换取正式令牌。
- TOTP 密钥通过 `FieldEncryptor` 以 AES-256-GCM 加密落库，不写日志、绑定完成后不再返回、实体 `toString()` 排除。
- 自助关闭（需当前验证码）+ 管理员重置（用户详情页，`system:user:update` 权限，审计）。
- 审计 `auth:mfa` enable/disable/reset；验证接口限流。

## 配置

| 配置 | 环境变量 | 默认 | 说明 |
| :--- | :--- | :--- | :--- |
| `strata.encryption.key` | `STRATA_ENCRYPTION_KEY` | 空（临时开发密钥） | Base64 编码的 32 字节 AES-256 密钥；**生产启用 MFA 必须设置**，否则重启后无法解密。 |
| `strata.mfa.issuer` | `STRATA_MFA_ISSUER` | `Strata Admin` | 认证器 App 中显示的名称，并写入 otpauth URI / 二维码。 |

生成密钥：`openssl rand -base64 32`。

## 接口

| 方法 | 路径 | 认证 | 说明 |
| :--- | :--- | :--- | :--- |
| POST | `/api/v1/system/auth/mfa/enroll` | 已登录 | 开始绑定，返回 secret / otpauth URI / 二维码 |
| POST | `/api/v1/system/auth/mfa/enroll/confirm` | 已登录 | `{"code":"123456"}` 确认绑定并开启 |
| POST | `/api/v1/system/auth/mfa/verify` | 公开 | `{"mfaToken":"…","code":"123456"}` 完成登录 |
| POST | `/api/v1/system/auth/mfa/disable` | 已登录 | `{"code":"123456"}` 关闭 |
| POST | `/api/v1/system/users/{id}/mfa/reset` | `system:user:update` | 管理员重置丢失设备 |

错误码：`40112 MFA_REQUIRED`、`40113 MFA_INVALID_CODE`、`40114 MFA_CHALLENGE_EXPIRED`、`40115 MFA_NOT_ENROLLED`。
challenge 一次性、5 分钟过期。

## 限制与恢复

- **0.x 无恢复码**：丢失设备由管理员重置（审计）。
- challenge 存储为单 JVM 内存（5 分钟 TTL）：单实例部署可用；多副本前需迁 Redis（已知技术债，与登录锁定同类）。
- MFA 按用户可选，没有"全局强制开启"开关；如需强制，请在入职/权限流程中要求或自行加守卫。
