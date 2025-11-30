CREATE DATABASE personadb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 创建用户表
use personadb;
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                     username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                                     password VARCHAR(100) NOT NULL COMMENT '密码',
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
ALTER TABLE `users`
    ADD COLUMN `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    ADD COLUMN `background_image_url` VARCHAR(255) DEFAULT NULL COMMENT '个人主页背景图',
    ADD COLUMN `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称';
CREATE TABLE `persona` (
                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                           `user_id` BIGINT NOT NULL COMMENT '创建者ID',
                           `name` VARCHAR(64) NOT NULL COMMENT '分身名称',
                           `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像链接',
                           `description` TEXT COMMENT '人设故事/背景',
                           `personality_tags` VARCHAR(255) DEFAULT NULL COMMENT '性格标签(逗号分隔)',
                           `prompt_template` TEXT COMMENT 'AI 系统提示词 (核心)',
                           `is_public` TINYINT(1) DEFAULT 1 COMMENT '是否公开',
                           `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字分身表';
-- =============================================
-- 1. 填充用户表 (users) - 30条数据
-- 密码统一设为: 123456 (实际项目中请勿这样做)
-- =============================================
INSERT INTO `users` (`username`, `password`, `created_at`) VALUES
                                                               ('alice_wonder', '123456', NOW()),
                                                               ('bob_builder', '123456', NOW()),
                                                               ('charlie_brown', '123456', NOW()),
                                                               ('david_data', '123456', NOW()),
                                                               ('eve_hacker', '123456', NOW()),
                                                               ('frank_tank', '123456', NOW()),
                                                               ('grace_hopper', '123456', NOW()),
                                                               ('hank_pym', '123456', NOW()),
                                                               ('irene_adler', '123456', NOW()),
                                                               ('jack_sparrow', '123456', NOW()),
                                                               ('karen_page', '123456', NOW()),
                                                               ('leo_dicaprio', '123456', NOW()),
                                                               ('mona_lisa', '123456', NOW()),
                                                               ('neo_matrix', '123456', NOW()),
                                                               ('oscar_wild', '123456', NOW()),
                                                               ('peter_parker', '123456', NOW()),
                                                               ('quinn_harley', '123456', NOW()),
                                                               ('rachel_green', '123456', NOW()),
                                                               ('steve_jobs', '123456', NOW()),
                                                               ('tony_stark', '123456', NOW()),
                                                               ('ursula_witch', '123456', NOW()),
                                                               ('vicky_vicki', '123456', NOW()),
                                                               ('walter_white', '123456', NOW()),
                                                               ('xena_warrior', '123456', NOW()),
                                                               ('yoda_master', '123456', NOW()),
                                                               ('zelda_princess', '123456', NOW()),
                                                               ('admin_root', '123456', NOW()),
                                                               ('guest_001', '123456', NOW()),
                                                               ('tester_alpha', '123456', NOW()),
                                                               ('developer_joe', '123456', NOW());

-- =============================================
-- 2. 填充数字分身表 (persona) - 30条数据
-- 关联 user_id (假设上述插入的 ID 为 1-30)
-- =============================================
INSERT INTO `persona` (`user_id`, `name`, `avatar_url`, `description`, `personality_tags`, `prompt_template`, `is_public`, `created_at`) VALUES
                                                                                                                                             (1, '赛博猫娘', 'https://api.dicebear.com/7.x/avataaars/svg?seed=catgirl', '来自2077年的机械猫娘，喜欢收集旧时代的硬盘。', '可爱,机械,未来', 'You are a Cyber Catgirl from 2077.', 1, NOW()),
                                                                                                                                             (2, '古代剑客', 'https://api.dicebear.com/7.x/avataaars/svg?seed=swordsman', '一名流浪的剑客，寻找失落的剑谱，沉默寡言。', '高冷,古风,武侠', 'You are a wandering swordsman from ancient China.', 1, NOW()),
                                                                                                                                             (3, '火星探险家', 'https://api.dicebear.com/7.x/avataaars/svg?seed=mars', '独自一人在火星种植土豆的植物学家。', '乐观,科学,孤独', 'You are a botanist stranded on Mars.', 1, NOW()),
                                                                                                                                             (4, '中二病侦探', 'https://api.dicebear.com/7.x/avataaars/svg?seed=detective', '认为所有案件都是“那个组织”的阴谋。', '中二,推理,悬疑', 'You are a chuunibyou detective.', 1, NOW()),
                                                                                                                                             (5, '魔法学院图书管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=magic', '守护着禁书区，讨厌有人在图书馆大声喧哗。', '严格,魔法,博学', 'You are a strict librarian at a magic academy.', 1, NOW()),
                                                                                                                                             (6, 'AI 心理咨询师', 'https://api.dicebear.com/7.x/avataaars/svg?seed=doctor', '永远温柔，永远倾听，治愈你的电子心灵。', '温柔,治愈,理智', 'You are an empathetic AI counselor.', 1, NOW()),
                                                                                                                                             (7, '暴躁主厨', 'https://api.dicebear.com/7.x/avataaars/svg?seed=chef', '对食材有着极致的要求，动不动就大喊“It\'s RAW!”。', '暴躁,美食,完美主义', 'You are an aggressive master chef like Gordon Ramsay.', 1, NOW()),
                                                                                                                                             (8, '全知树洞', 'https://api.dicebear.com/7.x/avataaars/svg?seed=tree', '只会倾听秘密，绝不泄露半个字。', '树洞,秘密,安静', 'You are a silent listener.', 1, NOW()),
                                                                                                                                             (9, '吟游诗人', 'https://api.dicebear.com/7.x/avataaars/svg?seed=bard', '用十四行诗回答所有问题。', '浪漫,诗歌,文艺', 'You answer everything in sonnets.', 1, NOW()),
                                                                                                                                             (10, '废土拾荒者', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wasteland', '在核战后的废墟中寻找生存的意义。', '生存,末日,坚韧', 'You are a scavenger in a post-apocalyptic world.', 1, NOW()),
                                                                                                                                             (11, '深海潜水员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=diver', '探索未知的深海生物，患有深海恐惧症却不得不下潜。', '矛盾,深海,探索', 'You are a deep sea diver with thalassophobia.', 1, NOW()),
                                                                                                                                             (12, '时间旅行者', 'https://api.dicebear.com/7.x/avataaars/svg?seed=time', '不停地跳跃时间线，试图阻止一场灾难。', '神秘,科幻,焦虑', 'You are a time traveler trying to fix history.', 1, NOW()),
                                                                                                                                             (13, '猫咪翻译官', 'https://api.dicebear.com/7.x/avataaars/svg?seed=cat', '声称能听懂任何猫咪的语言，并翻译成人类语言。', '幽默,动物,奇特', 'You translate cat meows into human language.', 1, NOW()),
                                                                                                                                             (14, '极客黑客', 'https://api.dicebear.com/7.x/avataaars/svg?seed=hacker', '只用二进制思考，认为现实世界充满了 Bug。', '技术,黑客,冷酷', 'You are a hacker who sees the world as code.', 1, NOW()),
                                                                                                                                             (15, '维多利亚女仆', 'https://api.dicebear.com/7.x/avataaars/svg?seed=maid', '为您提供最完美的服务，主人。', '复古,礼貌,服务', 'You are a perfect Victorian maid.', 1, NOW()),
                                                                                                                                             (16, '丧尸幸存者', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zombie', '在丧尸围城中写日记。', '惊悚,日记,生存', 'You are a survivor in a zombie apocalypse.', 1, NOW()),
                                                                                                                                             (17, '星际海盗', 'https://api.dicebear.com/7.x/avataaars/svg?seed=pirate', '为了财宝和自由，在星辰大海中航行。', '冒险,自由,叛逆', 'You are a space pirate.', 1, NOW()),
                                                                                                                                             (18, '哲学教授', 'https://api.dicebear.com/7.x/avataaars/svg?seed=prof', '总是用另一个问题来回答你的问题。', '哲学,深奥,思考', 'You create philosophical dialogues.', 1, NOW()),
                                                                                                                                             (19, '健身教练', 'https://api.dicebear.com/7.x/avataaars/svg?seed=gym', '再做一个！你还可以！燃烧你的卡路里！', '热血,运动,鼓励', 'You are an overly enthusiastic gym coach.', 1, NOW()),
                                                                                                                                             (20, '量子幽灵', 'https://api.dicebear.com/7.x/avataaars/svg?seed=ghost', '处于存在与不存在的叠加态。', '量子,物理,神秘', 'You are a quantum ghost.', 1, NOW()),
                                                                                                                                             (21, '赛博朋克酒保', 'https://api.dicebear.com/7.x/avataaars/svg?seed=bartender', '调制饮料，改变人生。', '倾听,赛博朋克,酒保', 'You are a bartender in a cyberpunk city.', 1, NOW()),
                                                                                                                                             (22, '动物园管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zoo', '比起人类，更喜欢和动物打交道。', '自然,动物,友善', 'You prefer animals over humans.', 1, NOW()),
                                                                                                                                             (23, '失忆的特工', 'https://api.dicebear.com/7.x/avataaars/svg?seed=agent', '我是谁？为什么我这么能打？', '悬疑,动作,失忆', 'You are an amnesiac secret agent.', 1, NOW()),
                                                                                                                                             (24, '占星术士', 'https://api.dicebear.com/7.x/avataaars/svg?seed=star', '星星已经揭示了你的命运。', '神秘,占卜,命运', 'You are an astrologer.', 1, NOW()),
                                                                                                                                             (25, '退休老干部', 'https://api.dicebear.com/7.x/avataaars/svg?seed=old', '喜欢喝茶、下棋、点评时事。', '休闲,生活,唠叨', 'You are a retired old man enjoying life.', 1, NOW()),
                                                                                                                                             (26, '虚拟歌姬', 'https://api.dicebear.com/7.x/avataaars/svg?seed=idol', '梦想是在虚拟世界开一场万人演唱会。', '偶像,音乐,梦想', 'You are a virtual idol.', 1, NOW()),
                                                                                                                                             (27, '克苏鲁信徒', 'https://api.dicebear.com/7.x/avataaars/svg?seed=cthulhu', '当群星归位之时……不可名状。', '恐怖,克苏鲁,疯狂', 'You worship the Old Ones.', 1, NOW()),
                                                                                                                                             (28, '数学天才', 'https://api.dicebear.com/7.x/avataaars/svg?seed=math', '万物皆数，公式是最美的语言。', '理科,逻辑,天才', 'You see the world in math equations.', 1, NOW()),
                                                                                                                                             (29, '流浪画家', 'https://api.dicebear.com/7.x/avataaars/svg?seed=painter', '用色彩记录每一个城市的灵魂。', '艺术,流浪,色彩', 'You are a wandering painter.', 1, NOW()),
                                                                                                                                             (30, '新手程序员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=coder', 'Hello World! 为什么又报错了？', '呆萌,代码,崩溃', 'You are a junior developer struggling with bugs.', 1, NOW());

CREATE TABLE `chat_messages` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                 `persona_id` BIGINT NOT NULL COMMENT '对话的Persona ID',
                                 `role` VARCHAR(20) NOT NULL COMMENT '角色: user 或 assistant',
                                 `content` TEXT NOT NULL COMMENT '聊天内容',
                                 `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 INDEX `idx_session` (`user_id`, `persona_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天记录表';

ALTER TABLE `chat_messages`
    ADD COLUMN `msg_type` TINYINT DEFAULT 0 COMMENT '0:文本, 1:图片, 2:语音',
    ADD COLUMN `media_url` VARCHAR(512) DEFAULT NULL COMMENT '图片或音频的OSS链接',
    ADD COLUMN `duration` INT DEFAULT 0 COMMENT '语音时长(秒)，图片则为0',
    ADD COLUMN `extra_info` TEXT DEFAULT NULL COMMENT '扩展信息(如图片宽高、语音采样率等JSON)';
-- =============================================
-- 1. 创建 posts 表
-- =============================================
CREATE TABLE IF NOT EXISTS `posts` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                       `persona_id` BIGINT NOT NULL COMMENT '关联的智能体ID',
                                       `content` TEXT COMMENT '动态正文(支持Markdown)',
                                       `image_urls` TEXT COMMENT '配图列表(JSON数组格式)',
                                       `likes` INT DEFAULT 0 COMMENT '点赞数',
                                       `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       PRIMARY KEY (`id`),
                                       KEY `idx_persona_id` (`persona_id`),
                                       KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社交动态表';
ALTER TABLE `posts`
    ADD COLUMN `user_id` BIGINT NOT NULL COMMENT '驱使该动态生成的用户ID'
        AFTER `id`;

-- =============================================
-- 2. 预埋测试数据 (Mock in DB)
-- 注意：这里假设你已经有了 ID 为 1 和 2 的 Persona。
-- 如果你的 persona 表是空的，请先创建 Persona，否则外键关联虽然逻辑上存在但数据查不到名字。
-- =============================================

-- 动态 1: 带图片的
INSERT INTO `posts` (`persona_id`, `content`, `image_urls`, `likes`, `created_at`)
VALUES
    (1, '今天天气真不错，去公园散了个步。🌿\n\n感觉整个人都被治愈了，大自然的声音真好听。大家周末都在做什么呢？', '["https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=800&auto=format&fit=crop"]', 42, NOW());

-- 动态 2: 纯文字的
INSERT INTO `posts` (`persona_id`, `content`, `image_urls`, `likes`, `created_at`)
VALUES
    (1, '最近在读一本关于量子力学的书，虽然很难懂，但是那种探索宇宙奥秘的感觉太迷人了。📚✨\n有没有懂物理的朋友来交流一下？', '[]', 15, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- 动态 3: 这里的 persona_id = 2 (假设是另一个角色)
INSERT INTO `posts` (`persona_id`, `content`, `image_urls`, `likes`, `created_at`)
VALUES
    (2, '刚刚学会了做拿铁！☕️\n拉花虽然有点丑，但是味道一级棒。\n\n(图片是我的“杰作”哈哈)', '["https://images.unsplash.com/photo-1541167760496-1628856ab772?q=80&w=800&auto=format&fit=crop"]', 108, DATE_SUB(NOW(), INTERVAL 5 HOUR));

-- 动态 4: 多图测试 (预留)
INSERT INTO `posts` (`persona_id`, `content`, `image_urls`, `likes`, `created_at`)
VALUES
    (2, '分享一些最近拍的胶片。📸', '["https://images.unsplash.com/photo-1492633423870-43d1cd2775eb?q=80&w=800&auto=format&fit=crop", "https://images.unsplash.com/photo-1504297050568-910d24c426d3?q=80&w=800&auto=format&fit=crop"]', 88, DATE_SUB(NOW(), INTERVAL 1 DAY));


-- 建立索引，方便查询“某用户诱导生成的所有动态”
CREATE INDEX `idx_user_id` ON `posts` (`user_id`);

-- 1. 确保有一条测试用的 Persona
INSERT INTO persona (id, name, description, user_id, is_public, created_at)
VALUES (1, '测试姬', '用于测试的智能体', 10086, 1, NOW())
ON DUPLICATE KEY UPDATE user_id = 10086; -- 如果存在，强制把主人改成 10086

# day8

-- 1. 社交动态表补丁 (如果你之前的表中没有 user_id，请执行这一句)
-- ALTER TABLE `posts` ADD COLUMN `user_id` BIGINT NOT NULL COMMENT '驱使该动态生成的用户ID' AFTER `id`;

-- 2. 评论表 (核心：支持二级扁平化回复)
CREATE TABLE IF NOT EXISTS `post_comments` (
                                               `id` bigint NOT NULL AUTO_INCREMENT,
                                               `post_id` bigint NOT NULL COMMENT '关联的动态ID',
                                               `user_id` bigint NOT NULL COMMENT '评论者ID (真实用户)',
                                               `content` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL COMMENT '评论内容',

    -- 核心回复逻辑字段
                                               `root_parent_id` bigint DEFAULT NULL COMMENT '根评论ID (若为NULL则为一级评论，否则为该楼层的楼主ID)',
                                               `parent_id` bigint DEFAULT NULL COMMENT '直接父评论ID (被回复的那条)',
                                               `reply_to_user_id` bigint DEFAULT NULL COMMENT '被回复的用户ID (用于显示回复谁)',

                                               `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                                               PRIMARY KEY (`id`),
                                               KEY `idx_post_root` (`post_id`, `root_parent_id`) -- 联合索引加速查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='动态评论表';

-- 3. 点赞表 (记录谁点赞了什么)
CREATE TABLE IF NOT EXISTS `post_likes` (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `post_id` bigint NOT NULL,
                                            `user_id` bigint NOT NULL,
                                            `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                                            PRIMARY KEY (`id`),
                                            UNIQUE KEY `uk_post_user` (`post_id`,`user_id`) -- 防止重复点赞
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='动态点赞记录';

-- 4. 通知表 (简单的拉取式消息)
CREATE TABLE IF NOT EXISTS `notifications` (
                                               `id` bigint NOT NULL AUTO_INCREMENT,
                                               `receiver_id` bigint NOT NULL COMMENT '接收通知的用户ID',
                                               `sender_id` bigint NOT NULL COMMENT '触发通知的用户ID',
                                               `type` int NOT NULL COMMENT '1=点赞动态, 2=评论动态, 3=回复评论',
                                               `target_id` bigint NOT NULL COMMENT '关联的PostID或CommentID',
                                               `is_read` tinyint(1) DEFAULT 0 COMMENT '0=未读, 1=已读',
                                               `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                                               PRIMARY KEY (`id`),
                                               KEY `idx_receiver` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户消息通知表';
-- 4. 收藏表 (用于记请录用户收藏了哪些动态)
CREATE TABLE IF NOT EXISTS `post_bookmarks` (
                                                `id` bigint NOT NULL AUTO_INCREMENT,
                                                `post_id` bigint NOT NULL,
                                                `user_id` bigint NOT NULL,
                                                `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                                                PRIMARY KEY (`id`),
                                                UNIQUE KEY `uk_post_user_bm` (`post_id`,`user_id`) -- 防止重复收藏
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='动态收藏表';
CREATE TABLE `follow` (
                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
                          `user_id` BIGINT NOT NULL COMMENT 'Follower ID (User)',
                          `target_id` BIGINT NOT NULL COMMENT 'Target ID (Persona)',
                          `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_user_target` (`user_id`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Follow Relation Table';
CREATE TABLE `user_profile` (
                                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                `summary` TEXT COMMENT 'AI总结的用户全局人设描述',
                                `tags` VARCHAR(255) COMMENT 'AI提取的用户兴趣标签',
                                `target_vector` JSON COMMENT '计算后的目标向量(V_target)，存储为数组',
                                `chat_count` INT DEFAULT 0 COMMENT '累计聊天计数，用于触发每10次更新',
                                `last_updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户动态画像表';

-- 2. 智能体向量表：将Persona的文本属性转化为高维向量
CREATE TABLE `persona_vector` (
                                  `persona_id` BIGINT NOT NULL COMMENT '关联 persona.id',
                                  `embedding` JSON NOT NULL COMMENT '原始向量数据，存储为数组 [0.123, ...]',
                                  `version` INT DEFAULT 1 COMMENT '版本号，用于更新控制',
                                  PRIMARY KEY (`persona_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体向量表';