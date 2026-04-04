package com.hxh.apboa.agent.service.impl;

import com.hxh.apboa.agent.mapper.AgentScopeSessionMapper;
import com.hxh.apboa.agent.mapper.ChatSessionMapper;
import com.hxh.apboa.agent.service.ChatMessageService;
import com.hxh.apboa.agent.service.ChatSessionService;
import com.hxh.apboa.common.dto.ChatMessageAppendDTO;
import com.hxh.apboa.common.dto.ChatSessionCreateDTO;
import com.hxh.apboa.common.dto.ChatSessionQueryDTO;
import com.hxh.apboa.common.entity.ChatMessage;
import com.hxh.apboa.common.entity.ChatSession;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.util.UserUtils;
import com.hxh.apboa.common.vo.ChatMessageVO;
import com.hxh.apboa.common.vo.ChatSessionVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.agentscope.spring.boot.agui.common.ThreadSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天会话 Service 实现：新会话插根消息并设 current_message_id；追加/重新生成插新消息并更新 current_message_id；切换分支仅更新 current_message_id；回显按 path 查消息链。
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private final ChatMessageService chatMessageService;
    private final ThreadSessionManager sessionManager;
    private final AgentScopeSessionMapper agentScopeSessionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionVO createSession(ChatSessionCreateDTO dto) {
        Long userId = UserUtils.getId();
        if (userId == null || userId == 0L) {
            throw new RuntimeException("用户未登录");
        }
        if (dto.getAgentId() == null) {
            throw new RuntimeException("agentId 不能为空");
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setAgentId(dto.getAgentId());
        session.setTitle(dto.getTitle() != null ? dto.getTitle() : "新对话");

        save(session);

        ChatMessage root = new ChatMessage();
        root.setSessionId(session.getId());
        root.setRole("system");
        root.setContent("");
        root.setParentId(null);
        chatMessageService.save(root);
        root.setPath(String.valueOf(root.getId()));
        root.setDepth(0);
        chatMessageService.updateById(root);

        session.setCurrentMessageId(root.getId());
        updateById(session);

        return toSessionVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO appendMessage(Long sessionId, ChatMessageAppendDTO dto) {
        ChatSession session = getAndCheckSession(sessionId);
        ChatMessage parent = getMessageBy(session.getCurrentMessageId(), sessionId);
        return saveNewMessageAndMoveCursor(session, parent, dto.getRole(), dto.getContent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO regenerateMessage(Long sessionId, ChatMessageAppendDTO dto) {
        ChatSession session = getAndCheckSession(sessionId);
        ChatMessage parent = getMessageBy(session.getCurrentMessageId(), sessionId);
        return saveNewMessageAndMoveCursor(session, parent, dto.getRole(), dto.getContent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchCurrentMessage(Long sessionId, Integer messageId) {
        ChatSession session = getAndCheckSession(sessionId);
        ChatMessage message = chatMessageService.getById(messageId);
        if (message == null || !message.getSessionId().equals(sessionId)) {
            throw new RuntimeException("消息不存在或不属于该会话");
        }
        session.setCurrentMessageId(messageId);
        updateById(session);
    }

    @Override
    public List<ChatMessageVO> getCurrentMessages(Long sessionId) {
        ChatSession session = getById(sessionId);
        if (session == null) {
            return new ArrayList<>();
        }
        Integer curId = session.getCurrentMessageId();
        if (curId == null) {
            return new ArrayList<>();
        }
        ChatMessage cur = chatMessageService.getById(curId);
        if (cur == null) {
            return new ArrayList<>();
        }
        String path = cur.getPath();
        if (path == null || path.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> ids = Arrays.stream(path.split("/"))
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            ids.add(curId);
        }
        List<ChatMessage> list = chatMessageService.listByIdsOrderByDepth(ids);
        return BeanUtils.copyList(list, ChatMessageVO.class);
    }

    @Override
    public List<ChatSessionVO> listSessions(ChatSessionQueryDTO query) {
        Long userId = query.getUserId() != null ? query.getUserId() : UserUtils.getId();
        return lambdaQuery()
                .eq(userId != null, ChatSession::getUserId, userId)
                .eq(query.getAgentId() != null, ChatSession::getAgentId, query.getAgentId())
                .orderByDesc(ChatSession::getIsPinned)
                .orderByDesc(ChatSession::getUpdatedAt)
                .list()
                .stream()
                .map(this::toSessionVO)
                .collect(Collectors.toList());
    }

    @Override
    public ChatSessionVO getSessionDetail(Long id) {
        ChatSession session = getById(id);
        if (session == null) {
            return null;
        }
        return toSessionVO(session);
    }

    private ChatSession getAndCheckSession(Long sessionId) {
        ChatSession session = getById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在或已删除");
        }
        if (!session.getUserId().equals(UserUtils.getId())) {
            throw new RuntimeException("无权限操作该会话");
        }
        return session;
    }

    private ChatMessage getMessageBy(Integer messageId, Long sessionId) {
        ChatMessage msg = chatMessageService.getById(messageId);
        if (msg == null || !msg.getSessionId().equals(sessionId)) {
            throw new RuntimeException("当前消息不存在或不属于该会话");
        }
        return msg;
    }

    /**
     * 在父消息后插入新消息并更新会话的 current_message_id（用于正常追加与重新生成）
     */
    private ChatMessageVO saveNewMessageAndMoveCursor(ChatSession session, ChatMessage parent, String role, String content) {
        if (role == null || content == null) {
            throw new RuntimeException("role 与 content 不能为空");
        }

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(session.getId());
        msg.setRole(role);
        msg.setContent(content);
        msg.setParentId(parent.getId());
        chatMessageService.save(msg);

        String parentPath = parent.getPath();
        String newPath = (parentPath == null || parentPath.isEmpty())
                ? String.valueOf(msg.getId())
                : parentPath + "/" + msg.getId();
        msg.setPath(newPath);
        msg.setDepth((parent.getDepth() == null ? 0 : parent.getDepth()) + 1);
        chatMessageService.updateById(msg);

        session.setCurrentMessageId(msg.getId());
        updateById(session);

        return BeanUtils.copy(msg, ChatMessageVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pinSession(Long id) {
        ChatSession session = getAndCheckSession(id);
        session.setIsPinned(true);
        session.setPinTime(java.time.LocalDateTime.now());
        updateById(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpinSession(Long id) {
        ChatSession session = getAndCheckSession(id);
        session.setIsPinned(false);
        session.setPinTime(null);
        updateById(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTitle(Long id, String title) {
        ChatSession session = getAndCheckSession(id);
        session.setTitle(title != null ? title : "新对话");
        updateById(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long id) {
        ChatSession session = getAndCheckSession(id);
        chatMessageService.lambdaUpdate().eq(ChatMessage::getSessionId, session.getId()).remove();
        removeById(id);

        agentScopeSessionMapper.deleteById(String.valueOf(session.getId()));

        // 删除 agentscope session
        if (sessionManager != null) {
            sessionManager.removeSession(String.valueOf(session.getId()));
        }
    }

    private ChatSessionVO toSessionVO(ChatSession session) {
        return BeanUtils.copy(session, ChatSessionVO.class);
    }
}
