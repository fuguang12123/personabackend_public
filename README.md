# Persona Backend - 智能角色扮演与社交平台后端

## 项目概述

Persona Backend 是一个基于Spring Boot的智能角色扮演与社交平台后端系统，集成了多种AI能力，包括语音合成、语音识别、图像生成、智能推荐和用户画像分析等功能。系统通过豆包大模型、智谱AI和Moonshot等AI服务，为用户提供沉浸式的角色扮演体验和个性化社交推荐。

## 技术栈

- **后端框架**: Spring Boot 3.5.7
- **Java版本**: Java 21
- **数据库**: MySQL
- **ORM框架**: MyBatis-Plus
- **认证授权**: JWT (JSON Web Token)
- **AI服务集成**:
  - 豆包大模型2.0 (火山引擎) - 语音合成与识别
  - 智谱AI - 图像生成 (CogView-4) 和文本嵌入
  - Moonshot (Kimi) - 文本生成与推理
- **对象存储**: 阿里云OSS
- **HTTP客户端**: OkHttp
- **JSON处理**: FastJSON2
- **日志框架**: SLF4J + Logback

## 项目结构

```
src/main/java/com/example/persona_backend/
├── PersonaBackendApplication.java     # Spring Boot 应用入口
├── common/                            # 通用组件
│   └── Result.java                    # 统一API响应封装
├── config/                            # 配置类
│   ├── SecurityConfig.java           # Spring Security配置
│   └── WebConfig.java                 # Web配置（CORS、过滤器等）
├── controller/                        # 控制器层 - 处理HTTP请求
│   ├── AdminController.java           # 管理员接口（如向量同步）
│   ├── AiController.java             # AI能力相关接口
│   ├── AuthController.java           # 认证相关接口（登录、注册）
│   ├── ChatController.java           # 聊天相关接口（文本、语音聊天）
│   ├── FeedController.java           # 动态广场接口
│   ├── FollowController.java          # 关注/粉丝相关接口
│   ├── PersonaController.java        # 角色相关接口
│   ├── PostController.java            # 动态发布相关接口
│   ├── TestController.java            # 测试接口
│   ├── UploadController.java          # 文件上传接口
│   └── UserController.java            # 用户相关接口
├── dto/                               # 数据传输对象
│   ├── AuthDto.java                   # 认证相关DTO
│   ├── ConversationDto.java           # 对话DTO
│   ├── CreatePostRequest.java         # 创建动态请求DTO
│   ├── GenerateImageRequest.java      # 图像生成请求DTO
│   ├── MagicEditRequest.java          # 魔法编辑请求DTO
│   ├── PersonaRecommendationDto.java  # 角色推荐DTO
│   ├── PostDetailVo.java              # 动态详情VO
│   ├── PostDto.java                   # 动态DTO
│   └── PublishPostRequest.java        # 发布动态请求DTO
├── entity/                            # 实体类 - 数据库表映射
│   ├── ChatMessage.java               # 聊天消息实体
│   ├── Follow.java                    # 关注关系实体
│   ├── Notification.java              # 通知实体
│   ├── Persona.java                   # 角色实体
│   ├── PersonaVector.java             # 角色向量实体（用于推荐）
│   ├── Post.java                      # 动态实体
│   ├── PostBookmark.java              # 动态收藏实体
│   ├── PostComment.java               # 动态评论实体
│   ├── PostLike.java                  # 动态点赞实体
│   ├── User.java                      # 用户实体
│   └── UserProfile.java               # 用户画像实体
├── filter/                            # 过滤器
│   └── JwtFilter.java                 # JWT认证过滤器
├── mapper/                            # MyBatis数据访问层
│   ├── ChatMessageMapper.java         # 聊天消息数据访问
│   ├── FollowMapper.java              # 关注关系数据访问
│   ├── NotificationMapper.java        # 通知数据访问
│   ├── PersonaMapper.java             # 角色数据访问
│   ├── PersonaVectorMapper.java       # 角色向量数据访问
│   ├── PostBookmarkMapper.java        # 动态收藏数据访问
│   ├── PostCommentMapper.java         # 动态评论数据访问
│   ├── PostLikeMapper.java            # 动态点赞数据访问
│   ├── PostMapper.java                # 动态数据访问
│   ├── UserMapper.java                # 用户数据访问
│   └── UserProfileMapper.java         # 用户画像数据访问
├── service/                           # 业务逻辑层
│   ├── AiService.java                 # AI服务集成（图像生成、用户画像分析等）
│   ├── ChatService.java               # 聊天服务（文本、语音、图像聊天）
│   ├── FeedService.java               # 动态广场服务
│   ├── RecommendationService.java     # 推荐服务（向量召回+大模型精排）
│   └── UserProfileService.java       # 用户画像服务（画像分析、进化）
└── utils/                             # 工具类
    ├── AliyunOSSOperator.java        # 阿里云OSS操作工具
    ├── AliyunOSSProperties.java       # 阿里云OSS配置属性
    ├── CaptchaUtils.java              # 验证码工具
    ├── JwtUtils.java                  # JWT工具类
    ├── VolcEngineUtils.java          # 火山引擎(豆包)工具类（语音合成与识别）
    ├── VolcProtocol.java              # 火山引擎协议定义
    └── ZhipuAiUtils.java              # 智谱AI工具类（图像生成、文本嵌入）
```

## 核心功能详解

### 1. 用户管理与认证

#### 功能描述
提供完整的用户生命周期管理，包括注册、登录、资料管理和认证授权。

#### 详细功能
- **用户注册**: 支持用户名/邮箱注册，包含验证码验证
- **用户登录**: 基于JWT的无状态认证，支持多设备登录
- **密码管理**: 密码修改功能，确保账户安全
- **资料管理**: 用户头像、背景图、昵称等个人信息管理
- **个人内容管理**: 
  - 查看个人创建的角色列表
  - 查看个人发布的动态列表
  - 查看个人点赞的动态列表
  - 查看个人收藏的动态列表

#### 技术实现
- 使用JWT进行无状态认证，支持分布式部署
- 密码加密存储，确保用户信息安全
- 基于Spring Security的权限控制

### 2. 角色系统 (Persona)

#### 功能描述
核心功能模块，允许用户创建和管理虚拟角色，每个角色具有独特的人设和性格。

#### 详细功能
- **角色创建**: 
  - 自定义角色名称、头像、描述
  - 设置角色性格标签
  - 配置角色提示词(Prompt Template)，用于AI对话
- **角色编辑**: 修改角色信息，包括名称、描述、提示词等
- **角色查询**: 获取角色详情，包括角色信息和统计数据
- **角色广场**: 
  - 展示公开角色列表，支持分页浏览
  - 按创建时间排序，展示最新创建的角色
- **角色推荐系统**:
  - 基于用户画像和行为数据的个性化推荐
  - 结合向量相似度和大模型推理的混合推荐策略
- **角色AI能力**:
  - AI生成角色描述和性格标签
  - 基于角色人设的文本生成和图像生成

#### 技术实现
- 角色数据存储在MySQL中，支持复杂的角色属性
- 角色向量存储用于推荐系统，提高推荐准确性
- 集成多种AI服务，为角色提供智能能力

### 3. 智能聊天系统

#### 功能描述
多模态聊天系统，支持文本、图像和语音交互，基于角色人设提供沉浸式对话体验。

#### 详细功能
- **文本聊天**:
  - 基于角色人设的智能对话
  - 支持长对话上下文管理
  - 支持Markdown和Emoji表达
- **图像生成聊天**:
  - 根据对话内容生成相关图像
  - 支持多种风格和尺寸的图像生成
  - 图像自动上传到OSS并返回URL
- **语音聊天**:
  - 语音识别：将用户上传的音频转换为文本
  - 语音合成：将AI回复转换为语音文件
  - 支持多种音色和语调
- **聊天历史管理**:
  - 保存完整的聊天记录
  - 支持查看历史对话
  - 支持清空聊天历史

#### 技术实现
- 多模态消息类型支持（文本、图像、语音）
- 基于WebSocket的实时通信
- 集成豆包大模型2.0提供语音能力
- 集成智谱AI提供图像生成能力
- 阿里云OSS存储多媒体文件

### 4. 社交功能

#### 功能描述
完整的社交功能模块，支持动态发布、互动和关系管理。

#### 详细功能
- **动态发布**:
  - 支持图文混合动态发布
  - AI辅助内容生成和优化
  - 自动关联角色信息
- **互动功能**:
  - 点赞/取消点赞动态
  - 收藏/取消收藏动态
  - 评论动态（支持多级评论）
- **关注系统**:
  - 关注/取消关注角色
  - 查看关注列表
  - 查看粉丝列表
- **动态广场**:
  - 展示所有公开动态
  - 支持按时间排序和推荐排序
  - 支持分页浏览

#### 技术实现
- 基于关系型数据库存储
- 异步处理点赞、收藏等操作

### 5. AI能力集成

#### 5.1 豆包大模型2.0 (火山引擎)

##### 功能描述
提供高质量的语音合成和语音识别能力，支持多种音色和语言。

##### 详细功能
- **语音合成 (TTS)**:
  - 基于WebSocket的流式语音合成
  - 支持多种音色和语调
  - 支持情感化语音合成
  - 支持SSML标记语言控制语音细节
- **语音识别 (ASR)**:
  - 支持多种音频格式（mp3, wav, m4a, pcm）
  - 一句话识别和实时流式识别
  - 支持噪声环境下的高精度识别
  - 支持自动标点添加和文本规范化

##### 技术实现
- 基于WebSocket的实时通信
- 支持音频压缩和分片传输
- 完善的错误处理和重试机制

#### 5.2 智谱AI

##### 功能描述
提供高质量的图像生成和文本嵌入能力，支持多种艺术风格和语义理解。

##### 详细功能
- **图像生成**:
  - 使用CogView-4模型生成高质量图像
  - 支持多种尺寸和风格
  - 支持图像编辑和变体生成
  - 支持基于文本提示的创意生成
- **文本嵌入**:
  - 生成高维文本向量
  - 支持语义相似度计算
  - 用于推荐系统和搜索功能

##### 技术实现
- 使用官方SDK和API
- 支持异步处理和批量操作
- 完善的错误处理和降级策略

#### 5.3 Moonshot (Kimi)

##### 功能描述
提供强大的文本生成和推理能力，支持复杂任务和长上下文理解。

##### 详细功能
- **文本生成**:
  - 基于角色人设的对话生成
  - 内容创作和优化
  - 推荐理由生成
- **用户画像分析**:
  - 从聊天记录中提取用户性格特征
  - 分析用户兴趣和偏好
  - 生成用户标签和描述
- **推荐推理**:
  - 基于用户和角色特征生成推荐理由
  - 支持多维度推荐评分
  - 支持推荐结果解释

##### 技术实现
- 使用结构化提示词确保输出格式
- 支持JSON模式输出
- 完善的错误处理和重试机制

### 6. 推荐系统

#### 功能描述
基于向量相似度和大模型推理的混合推荐系统，为用户提供个性化的角色推荐。

#### 详细功能
- **L0召回**:
  - 基于向量相似度的初步筛选
  - 支持多种相似度计算方法
  - 支持批量向量计算
- **L1精排**:
  - 使用大模型进行认知推理
  - 生成个性化推荐理由
  - 支持多维度评分
- **多源融合**:
  - 结合用户画像和行为数据
  - 支持动态权重调整
  - 支持冷启动处理

#### 技术实现
- 基于余弦相似度的向量召回
- 使用大模型进行认知推理
- 支持实时和离线推荐
- 支持A/B测试和效果评估

### 7. 用户画像系统

#### 功能描述
基于用户行为和聊天记录，动态构建和更新用户画像，支持个性化推荐和服务。

#### 详细功能
- **画像构建**:
  - 从聊天记录中提取用户特征
  - 生成用户性格标签和描述
  - 构建用户向量表示
- **画像进化**:
  - 基于用户行为动态更新画像
  - 支持增量更新和全量更新
  - 支持画像版本管理
- **画像应用**:
  - 用于个性化推荐
  - 用于内容生成优化
  - 用于用户行为预测

#### 技术实现
- 基于大模型的画像分析
- 支持异步处理和批量更新
- 使用向量数据库存储画像向量

### 8. 文件管理系统

#### 功能描述
基于阿里云OSS的分布式文件存储系统，支持多种文件类型和访问控制。

#### 详细功能
- **文件上传**:
  - 支持多种文件格式上传
  - 自动生成唯一文件名
  - 支持文件压缩和优化
- **文件访问**:
  - 支持公开和私有访问
  - 支持临时URL生成
  - 支持CDN加速
- **文件管理**:
  - 支持文件元数据管理
  - 支持文件生命周期管理
  - 支持文件备份和恢复

#### 技术实现
- 使用阿里云OSS SDK
- 支持分片上传和断点续传
- 完善的错误处理和重试机制

## 配置说明

### 应用配置 (application.yml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/persona_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

# 火山引擎配置
volc:
  appId: your_app_id
  accessToken: your_access_token
  asr:
    cluster: volc_sms_status
  tts:
    defaultVoice: saturn_zh_female_cancan_tob

# 智谱AI配置
zhipu:
  api:
    key: your_zhipu_api_key

# Moonshot API配置
moonshot:
  api:
    key: your_moonshot_api_key
    url: https://api.moonshot.cn/v1/chat/completions

# 阿里云OSS配置
aliyun:
  oss:
    endpoint: https://oss-cn-beijing.aliyuncs.com
    accessKeyId: your_access_key_id
    accessKeySecret: your_access_key_secret
    bucketName: your_bucket_name
```

## API文档

### 认证相关

- `POST /auth/login` - 用户登录
  ```json
  // 请求体
  {
    "username": "user123",
    "password": "password123"
  }
  
  // 响应
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "user": {
        "id": 1,
        "username": "user123",
        "nickname": "昵称",
        "avatarUrl": "头像URL"
      }
    }
  }
  ```
- `POST /auth/register` - 用户注册
- `POST /auth/captcha` - 获取验证码

### 用户相关

- `GET /users/me` - 获取当前用户信息
- `PUT /users/me` - 更新用户信息
- `POST /users/me/password` - 修改密码
- `GET /users/me/personas` - 获取我的角色列表
- `GET /users/me/posts` - 获取我的动态列表
- `GET /users/me/likes` - 获取我的点赞列表
- `GET /users/me/bookmarks` - 获取我的收藏列表

### 角色相关

- `POST /personas` - 创建角色
  ```json
  // 请求体
  {
    "name": "角色名称",
    "description": "角色描述",
    "avatarUrl": "头像URL",
    "personalityTags": "标签1,标签2,标签3",
    "promptTemplate": "角色提示词",
    "isPublic": true
  }
  ```
- `PUT /personas/{id}` - 更新角色
- `GET /personas/{id}` - 获取角色详情
- `GET /personas/feed` - 获取角色广场（支持分页）
  ```json
  // 请求参数
  // GET /personas/feed?page=1&size=20
  
  // 响应
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "name": "角色名称",
        "description": "角色描述",
        "avatarUrl": "头像URL",
        "personalityTags": "标签1,标签2,标签3",
        "createdAt": "2023-01-01T00:00:00"
      }
    ]
  }
  ```
- `GET /personas/recommend` - 获取角色推荐
- `POST /personas/ai/image` - AI生成图像
  ```json
  // 请求体
  {
    "prompt": "生成图像的提示词"
  }
  
  // 响应
  {
    "code": 200,
    "message": "操作成功",
    "data": "图像URL"
  }
  ```
- `POST /personas/ai/magic-edit` - AI魔法编辑
  ```json
  // 请求体
  {
    "content": "要编辑的内容",
    "personaName": "角色名称",
    "description": "角色描述",
    "tags": "角色标签"
  }
  
  // 响应
  {
    "code": 200,
    "message": "操作成功",
    "data": "编辑后的内容"
  }
  ```
- `POST /personas/{id}/posts` - 发布角色动态

### 聊天相关

- `POST /chat/{personaId}` - 文本聊天
  ```json
  // 请求体
  {
    "content": "聊天内容",
    "isImageGen": false
  }
  
  // 响应
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1,
      "role": "assistant",
      "content": "AI回复内容",
      "msgType": 0,
      "mediaUrl": null,
      "duration": 0,
      "createdAt": "2023-01-01T00:00:00"
    }
  }
  ```
- `POST /chat/{personaId}/audio` - 语音聊天
  ```json
  // 请求体 (multipart/form-data)
  // audioFile: 音频文件
  // duration: 音频时长（秒）
  
  // 响应
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1,
      "role": "assistant",
      "content": "语音识别后的文本",
      "msgType": 2,
      "mediaUrl": "语音文件URL",
      "duration": 10,
      "createdAt": "2023-01-01T00:00:00"
    }
  }
  ```
- `GET /chat/{personaId}/history` - 获取聊天历史
- `DELETE /chat/{personaId}/history` - 清空聊天历史

### 社交相关

- `POST /follow/{personaId}` - 关注角色
- `DELETE /follow/{personaId}` - 取消关注
- `GET /follow/following` - 获取关注列表
- `GET /follow/followers` - 获取粉丝列表

### 动态相关

- `GET /posts` - 获取动态广场
- `GET /posts/{id}` - 获取动态详情
- `POST /posts/{id}/like` - 点赞动态
- `DELETE /posts/{id}/like` - 取消点赞
- `POST /posts/{id}/bookmark` - 收藏动态
- `DELETE /posts/{id}/bookmark` - 取消收藏

## 部署说明

### 环境要求

- Java 21+
- MySQL 8.0+
- Maven 3.6+

### 本地开发

1. 克隆代码库
2. 修改`application.yml`中的数据库连接和API密钥配置
3. 运行数据库初始化脚本
4. 执行`mvn spring-boot:run`启动应用
5. 若数据库存在智能体并没有对应向量，需要发送/admin/sync-persona-vectors

### Docker部署

```bash
# 构建镜像
docker build -t persona-backend .

# 运行容器
docker run -d -p 8080:8080 --name persona-backend persona-backend
```

### 生产部署

1. 配置生产环境数据库和API密钥
2. 打包应用: `mvn clean package`
3. 运行应用: `java -jar persona-backend.jar`

## 注意事项

1. **API密钥安全**: 生产环境中请确保所有API密钥通过环境变量或安全配置中心管理，不要直接写入代码或提交到版本控制系统
2. **数据库安全**: 生产环境请使用强密码，并限制数据库访问权限
3. **资源监控**: 监控AI API调用频率和成本，设置合理的限流和降级策略
4. **日志管理**: 生产环境建议使用ELK或其他日志收集系统，避免敏感信息泄露
5. **跨域配置**: 根据前端部署情况调整CORS配置
6. **性能优化**: 
   - 合理使用缓存减少数据库压力
   - 对AI API调用进行限流和熔断处理
   - 使用连接池管理数据库连接
   - 对大文件上传使用分片上传

## 许可证

本项目采用 [MIT License](LICENSE) 许可证。

## 贡献指南

欢迎提交Issue和Pull Request来改进本项目。在提交代码前，请确保：

1. 代码符合项目编码规范
2. 添加必要的单元测试
3. 更新相关文档

## 联系方式

如有问题或建议，请通过以下方式联系：

- 邮箱: 3326498228@qq.com