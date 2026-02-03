package com.bxt.picturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bxt.picturebackend.aliYunAi.ChatCompletionResponse;
import com.bxt.picturebackend.aliYunAi.DashScopeClient;
import com.bxt.picturebackend.constant.RedisKeyConstant;
import com.bxt.picturebackend.constant.UserConstant;
import com.bxt.picturebackend.dto.chat.ChatPictureItem;
import com.bxt.picturebackend.dto.chat.ChatResponse;
import com.bxt.picturebackend.dto.picture.PictureQueryRequest;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import com.bxt.picturebackend.model.entity.Picture;
import com.bxt.picturebackend.model.entity.User;
import com.bxt.picturebackend.model.enums.PictureReviewStatusEnum;
import com.bxt.picturebackend.service.AgentChatService;
import com.bxt.picturebackend.service.PictureService;
import com.bxt.picturebackend.service.UserService;
import com.bxt.picturebackend.vo.PictureVo;
import com.bxt.picturebackend.vo.UserLoginVo;
import com.bxt.picturebackend.vo.UserUploadRankVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AgentChatServiceImpl implements AgentChatService {

    private static final String SYSTEM_PROMPT = "你是本图库平台的智能客服。你可以通过工具帮用户：1）按关键词或分类搜索图片；2）查看当前用户最近上传的图片；3）查看用户上传数量排行榜；4）对指定图片进行分析（描述内容、主体、风格）；5）编辑用户自己的图片信息（名称 name、简介 introduction、标签 tags）。当用户说「修改图片名称」「改简介」「改标签」「把图片 123 的名字/简介/标签改成 xxx」等并给出图片ID时，请调用 edit_picture 工具。仅图片所有者或管理员可编辑。若用户未登录却询问「我的上传」或要编辑图片，请提示先登录。";
    private static final int MAX_TOOL_ROUNDS = 5;
    private static final int SESSION_TTL_MINUTES = 30;

    @Autowired
    private DashScopeClient dashScopeClient;
    @Autowired
    private PictureService pictureService;
    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public ChatResponse chat(String message, String sessionId, HttpServletRequest request) {
        if (StrUtil.isBlank(message)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息不能为空");
        }
        List<Map<String, Object>> tools = buildTools();
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        if (StrUtil.isNotBlank(sessionId)) {
            List<Map<String, Object>> history = loadSession(sessionId);
            if (!history.isEmpty()) {
                messages.addAll(history);
            }
        }
        messages.add(Map.of("role", "user", "content", message));

        List<Long> pictureIds = new ArrayList<>();
        int rounds = 0;
        ChatCompletionResponse.Message lastMessage = null;

        while (rounds < MAX_TOOL_ROUNDS) {
            lastMessage = callDashScope(messages, tools);
            if (lastMessage.getToolCalls() == null || lastMessage.getToolCalls().isEmpty()) {
                break;
            }
            messages.add(buildAssistantMessageWithToolCalls(lastMessage));
            for (ChatCompletionResponse.ToolCall tc : lastMessage.getToolCalls()) {
                String toolResult = executeTool(tc.getFunction().getName(), tc.getFunction().getArguments(), request);
                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", tc.getId(),
                        "content", toolResult
                ));
                collectPictureIdsFromToolResult(tc.getFunction().getName(), toolResult, pictureIds);
            }
            rounds++;
        }

        String reply = lastMessage != null && StrUtil.isNotBlank(lastMessage.getContent())
                ? lastMessage.getContent()
                : "抱歉，我暂时无法回复，请稍后再试。";
        if (StrUtil.isNotBlank(sessionId)) {
            saveSession(sessionId, message, messages);
        }

        ChatResponse response = new ChatResponse();
        response.setReply(reply);
        response.setPictureIds(pictureIds.isEmpty() ? null : pictureIds);
        response.setPictures(buildPictureListFromIds(pictureIds));
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "search_pictures",
                        "description", "根据关键词或分类搜索已审核通过的图片。用于用户说「找图」「搜图」「风景」「猫」等。",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "keyword", Map.of("type", "string", "description", "搜索关键词，如风景、猫、人物"),
                                        "category", Map.of("type", "string", "description", "分类，可选")
                                ),
                                "required", List.of("keyword")
                        )
                )
        ));
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "get_my_recent_uploads",
                        "description", "获取当前登录用户最近上传的图片列表。仅当用户已登录且询问「我的上传」「我上传的图」时调用。",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "limit", Map.of("type", "integer", "description", "返回条数，默认10")
                                ),
                                "required", List.of()
                        )
                )
        ));
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "get_upload_rank",
                        "description", "获取用户上传图片数量的排行榜。用于用户问「上传排行榜」「谁上传最多」等。",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "top", Map.of("type", "integer", "description", "前 N 名，默认 10")
                                ),
                                "required", List.of()
                        )
                )
        ));
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "analyze_image",
                        "description", "对指定图片进行视觉分析：描述内容、主体、风格，或回答用户关于图片的问题。当用户说「分析这张图」「这张图是什么」「描述图片」「图片里有什么」并给出图片ID时调用。",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "picture_id", Map.of("type", "integer", "description", "图片ID，必填"),
                                        "question", Map.of("type", "string", "description", "用户对图片的提问，可选；不传则默认描述图片内容、主体和风格")
                                ),
                                "required", List.of("picture_id")
                        )
                )
        ));
        Map<String, Object> editPictureProps = new HashMap<>();
        editPictureProps.put("picture_id", Map.of("type", "integer", "description", "图片ID，必填"));
        editPictureProps.put("name", Map.of("type", "string", "description", "新的图片名称，可选"));
        editPictureProps.put("introduction", Map.of("type", "string", "description", "新的图片简介，可选；最多 800 字"));
        editPictureProps.put("tags", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "description", "新的标签列表，可选；如 [\"爱豆名\",\"风格\",\"场景\",\"情绪\"]，会整体替换原标签"
        ));
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "edit_picture",
                        "description", "编辑用户自己的图片信息（名称、简介、标签）。仅图片所有者或管理员可编辑。当用户说「修改图片名称」「改简介」「改标签」「把图片 123 的名字/简介/标签改成 xxx」等并给出图片ID时调用。",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", editPictureProps,
                                "required", List.of("picture_id")
                        )
                )
        ));
        return tools;
    }

    private ChatCompletionResponse.Message callDashScope(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        try {
            return dashScopeClient.chatWithTools(messages, tools);
        } catch (Exception e) {
            log.error("DashScope 调用失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "智能客服暂时不可用，请稍后再试");
        }
    }

    private Map<String, Object> buildAssistantMessageWithToolCalls(ChatCompletionResponse.Message msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("role", "assistant");
        m.put("content", msg.getContent() != null ? msg.getContent() : "");
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        for (ChatCompletionResponse.ToolCall tc : msg.getToolCalls()) {
            Map<String, Object> fc = new HashMap<>();
            fc.put("id", tc.getId());
            fc.put("type", "function");
            fc.put("function", Map.of("name", tc.getFunction().getName(), "arguments", tc.getFunction().getArguments()));
            toolCalls.add(fc);
        }
        m.put("tool_calls", toolCalls);
        return m;
    }

    private String executeTool(String name, String argumentsJson, HttpServletRequest request) {
        Map<String, Object> args = parseArguments(argumentsJson);
        try {
            switch (name) {
                case "search_pictures":
                    return doSearchPictures(args);
                case "get_my_recent_uploads":
                    return doGetMyRecentUploads(args, request);
                case "get_upload_rank":
                    return doGetUploadRank(args);
                case "analyze_image":
                    return doAnalyzeImage(args);
                case "edit_picture":
                    return doEditPicture(args, request);
                default:
                    return "未知工具: " + name;
            }
        } catch (Exception e) {
            log.warn("工具执行异常 name={} args={}", name, argumentsJson, e);
            return "执行失败: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argumentsJson) {
        if (StrUtil.isBlank(argumentsJson)) {
            return new HashMap<>();
        }
        try {
            return JSONUtil.toBean(argumentsJson, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String doSearchPictures(Map<String, Object> args) {
        String keyword = (String) args.getOrDefault("keyword", "");
        String category = (String) args.get("category");
        PictureQueryRequest req = new PictureQueryRequest();
        req.setCurrent(1);
        req.setPageSize(10);
        req.setSearchText(StrUtil.isNotBlank(keyword) ? keyword : null);
        req.setCategory(StrUtil.isNotBlank(category) ? category : null);
        req.setReviewStatus(PictureReviewStatusEnum.APPROVED.getValue());
        QueryWrapper<Picture> qw = pictureService.getQueryWrapper(req);
        if (qw == null) {
            return "暂无符合条件的图片。";
        }
        qw.eq("reviewStatus", PictureReviewStatusEnum.APPROVED.getValue());
        Page<Picture> page = pictureService.page(new Page<>(1, 10), qw);
        List<PictureVo> list = pictureService.getPictureVoPage(page, null).getRecords();
        if (list == null || list.isEmpty()) {
            return "未找到相关图片。";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            PictureVo p = list.get(i);
            String intro = StrUtil.nullToEmpty(p.getIntroduction());
            intro = intro.length() > 50 ? intro.substring(0, 50) : intro;
            sb.append(i + 1).append(". id=").append(p.getId())
                    .append(" 名称=").append(StrUtil.nullToEmpty(p.getName()))
                    .append(" 简介=").append(intro)
                    .append("\n");
        }
        return sb.toString();
    }

    private String doGetMyRecentUploads(Map<String, Object> args, HttpServletRequest request) {
        UserLoginVo user = null;
        try {
            user = userService.getCurrentUser(request);
        } catch (Exception ignored) {
        }
        if (user == null || user.getId() == null) {
            return "当前未登录，无法查询「我的上传」。请先登录。";
        }
        int limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 10;
        limit = Math.min(Math.max(limit, 1), 20);
        // 直接按 userId 查库，不经过 getQueryWrapper，避免布隆过滤器导致返回 null 查不到
        // 包含待审核与已通过的图片，方便用户看到自己上传的所有图
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        qw.eq("userId", user.getId());
        qw.in("reviewStatus", PictureReviewStatusEnum.PENDING.getValue(), PictureReviewStatusEnum.APPROVED.getValue());
        qw.orderByDesc("createTime");
        Page<Picture> page = pictureService.page(new Page<>(1, limit), qw);
        List<PictureVo> list = pictureService.getPictureVoPage(page, null).getRecords();
        if (list == null || list.isEmpty()) {
            return "您最近没有上传的图片。";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            PictureVo p = list.get(i);
            sb.append(i + 1).append(". id=").append(p.getId()).append(" ").append(StrUtil.nullToEmpty(p.getName())).append("\n");
        }
        return sb.toString();
    }

    private String doAnalyzeImage(Map<String, Object> args) {
        Object picIdObj = args.get("picture_id");
        if (picIdObj == null) {
            return "请提供图片ID（picture_id）。";
        }
        long pictureId = ((Number) picIdObj).longValue();
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            return "图片不存在，请检查图片ID。";
        }
        String url = picture.getUrl();
        if (StrUtil.isBlank(url)) {
            return "该图片暂无有效地址，无法分析。";
        }
        String question = (String) args.get("question");
        try {
            return dashScopeClient.analyzeImage(url, question);
        } catch (Exception e) {
            log.warn("图片分析失败 pictureId={}", pictureId, e);
            return "图片分析暂时失败，请稍后再试。原因：" + e.getMessage();
        }
    }

    private String doEditPicture(Map<String, Object> args, HttpServletRequest request) {
        UserLoginVo loginUser;
        try {
            loginUser = userService.getCurrentUser(request);
        } catch (Exception e) {
            return "请先登录后再编辑图片。";
        }
        if (loginUser == null || loginUser.getId() == null) {
            return "请先登录后再编辑图片。";
        }
        Object picIdObj = args.get("picture_id");
        if (picIdObj == null) {
            return "请提供图片ID（picture_id）。";
        }
        long pictureId = ((Number) picIdObj).longValue();
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            return "图片不存在，请检查图片ID。";
        }
        boolean isAdmin = UserConstant.ROLE_ADMIN.equals(loginUser.getUserRole());
        boolean isOwner = picture.getUserId() != null && picture.getUserId().equals(loginUser.getId());
        if (!isAdmin && !isOwner) {
            return "无权限修改该图片，仅图片所有者或管理员可编辑。";
        }
        String name = (String) args.get("name");
        String introduction = (String) args.get("introduction");
        Object tagsObj = args.get("tags");
        if (StrUtil.isNotBlank(name)) {
            picture.setName(name);
        }
        if (introduction != null) {
            picture.setIntroduction(introduction);
        }
        if (tagsObj != null) {
            List<String> tagList = new ArrayList<>();
            if (tagsObj instanceof List) {
                for (Object o : (List<?>) tagsObj) {
                    if (o != null) tagList.add(o.toString());
                }
            }
            picture.setTags(JSONUtil.toJsonStr(tagList));
        }
        picture.setEditTime(new Date());
        try {
            pictureService.validPicture(picture);
        } catch (BusinessException e) {
            return "校验失败：" + e.getMessage();
        }
        try {
            boolean ok = pictureService.updatePictureShardingSafe(picture);
            if (!ok) {
                return "更新失败，请稍后再试。";
            }
        } catch (Exception e) {
            log.error("编辑图片失败 pictureId={} userId={}", pictureId, picture.getUserId(), e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.contains("sharding") || msg.contains("20031") || msg.contains("分库分表") || msg.contains("分片")) {
                return "更新失败：分库分表限制。详情：" + msg;
            }
            return "更新失败：" + msg;
        }
        rabbitTemplate.convertAndSend("picture.cache.invalidate.queue", pictureId);
        StringBuilder sb = new StringBuilder("已更新图片信息。");
        if (StrUtil.isNotBlank(name)) sb.append(" 名称：").append(name);
        if (introduction != null) sb.append(" 简介：").append(introduction.length() > 50 ? introduction.substring(0, 50) + "…" : introduction);
        if (tagsObj != null && tagsObj instanceof List && !((List<?>) tagsObj).isEmpty()) {
            sb.append(" 标签：").append(JSONUtil.toJsonStr(tagsObj));
        }
        if (StrUtil.isBlank(name) && introduction == null && tagsObj == null) {
            sb.append("（未传入要修改的 name、introduction 或 tags）");
        }
        return sb.toString();
    }

    private String doGetUploadRank(Map<String, Object> args) {
        int top = args.get("top") != null ? ((Number) args.get("top")).intValue() : 10;
        top = Math.min(Math.max(top, 1), 20);
        Set<ZSetOperations.TypedTuple<String>> set = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisKeyConstant.PICTURE_UPLOAD_RANK, 0, top - 1);
        if (set == null || set.isEmpty()) {
            return "暂无排行榜数据。";
        }
        List<UserUploadRankVo> rankList = new ArrayList<>();
        int r = 1;
        for (ZSetOperations.TypedTuple<String> t : set) {
            if (t.getValue() == null || t.getScore() == null) continue;
            long uid = Long.parseLong(t.getValue());
            long count = t.getScore().longValue();
            if (count <= 0) continue;
            UserUploadRankVo vo = new UserUploadRankVo();
            vo.setUserId(uid);
            vo.setUploadCount(count);
            vo.setRank(r++);
            User u = userService.getById(uid);
            if (u != null) {
                vo.setUserName(u.getUserName());
                vo.setUserAvatar(u.getUserAvatar());
            }
            rankList.add(vo);
        }
        if (rankList.isEmpty()) {
            return "暂无排行榜数据。";
        }
        StringBuilder sb = new StringBuilder();
        for (UserUploadRankVo vo : rankList) {
            sb.append("第").append(vo.getRank()).append("名: 用户").append(vo.getUserName() != null ? vo.getUserName() : vo.getUserId())
                    .append(" 上传").append(vo.getUploadCount()).append("张\n");
        }
        return sb.toString();
    }


    private void collectPictureIdsFromToolResult(String toolName, String toolResult, List<Long> pictureIds) {
        if (!"search_pictures".equals(toolName) && !"get_my_recent_uploads".equals(toolName)) return;
        // 从 "1. id=123 ..." 形式解析 id（搜图与「我的上传」均为此格式）
        String[] lines = toolResult.split("\n");
        for (String line : lines) {
            int idx = line.indexOf("id=");
            if (idx < 0) continue;
            int start = idx + 3;
            int end = start;
            while (end < line.length() && Character.isDigit(line.charAt(end))) end++;
            if (end > start) {
                try {
                    pictureIds.add(Long.parseLong(line.substring(start, end)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /** 根据图片 ID 列表批量查询，返回 id + url + name 列表（去重、保持原顺序），方便前端展示 */
    private List<ChatPictureItem> buildPictureListFromIds(List<Long> pictureIds) {
        if (pictureIds == null || pictureIds.isEmpty()) return null;
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(pictureIds));
        if (ids.isEmpty()) return null;
        List<Picture> list = pictureService.listByIds(ids);
        Map<Long, Picture> idToPicture = new HashMap<>();
        for (Picture p : list) {
            if (p != null && p.getId() != null) idToPicture.put(p.getId(), p);
        }
        List<ChatPictureItem> result = new ArrayList<>();
        for (Long id : ids) {
            Picture p = idToPicture.get(id);
            if (p == null) continue;
            ChatPictureItem item = new ChatPictureItem();
            item.setId(p.getId());
            item.setUrl(p.getUrl());
            item.setName(p.getName());
            result.add(item);
        }
        return result.isEmpty() ? null : result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadSession(String sessionId) {
        String key = RedisKeyConstant.CHAT_SESSION_PREFIX + sessionId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) return Collections.emptyList();
        try {
            List<?> raw = JSONUtil.toList(json, Map.class);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object o : raw) result.add((Map<String, Object>) o);
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void saveSession(String sessionId, String userMessage, List<Map<String, Object>> fullMessages) {
        // 只保留 user / assistant / tool 轮次，且去掉 system；用于下一轮 context
        List<Map<String, Object>> toSave = fullMessages.stream()
                .filter(m -> !"system".equals(m.get("role")))
                .collect(Collectors.toList());
        String key = RedisKeyConstant.CHAT_SESSION_PREFIX + sessionId;
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(toSave), SESSION_TTL_MINUTES, TimeUnit.MINUTES);
    }
}
