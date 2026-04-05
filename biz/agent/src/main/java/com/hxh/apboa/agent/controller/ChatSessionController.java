package com.hxh.apboa.agent.controller;

import com.hxh.apboa.agent.service.ChatSessionService;
import com.hxh.apboa.common.config.auth.SkAccess;
import com.hxh.apboa.common.dto.ChatMessageAppendDTO;
import com.hxh.apboa.common.dto.ChatSessionCreateDTO;
import com.hxh.apboa.common.dto.ChatSessionQueryDTO;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.vo.ChatMessageVO;
import com.hxh.apboa.common.vo.ChatSessionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天会话 Controller：创建会话、追加/重新生成消息、切换分支、回显当前对话、会话列表与详情
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/agent/chat/session")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 创建新会话（插入根消息并设置 current_message_id）
     */
    @SkAccess
    @PostMapping
    public R<ChatSessionVO> create(@RequestBody ChatSessionCreateDTO dto) {
        return R.data(chatSessionService.createSession(dto));
    }

    /**
     * 正常对话追加消息
     */
    @SkAccess
    @PostMapping("/{sessionId}/message")
    public R<ChatMessageVO> appendMessage(@PathVariable("sessionId") Long sessionId, @RequestBody ChatMessageAppendDTO dto) {
        return R.data(chatSessionService.appendMessage(sessionId, dto));
    }

    /**
     * 重新生成（新分支，更新 current_message_id）
     */
    @SkAccess
    @PostMapping("/{sessionId}/regenerate")
    public R<ChatMessageVO> regenerate(@PathVariable("sessionId") Long sessionId, @RequestBody ChatMessageAppendDTO dto) {
        return R.data(chatSessionService.regenerateMessage(sessionId, dto));
    }

    /**
     * 切换历史分支（仅更新 current_message_id）
     */
    @SkAccess
    @PutMapping("/{sessionId}/current")
    public R<Void> switchCurrentMessage(@PathVariable("sessionId") Long sessionId, @RequestParam("messageId") Integer messageId) {
        chatSessionService.switchCurrentMessage(sessionId, messageId);
        return R.success("操作成功");
    }

    /**
     * 回显当前完整对话（按 path 查消息链，按 depth 排序）
     */
    @SkAccess
    @GetMapping("/{sessionId}/messages/current")
    public R<List<ChatMessageVO>> getCurrentMessages(@PathVariable("sessionId") Long sessionId) {
        return R.data(chatSessionService.getCurrentMessages(sessionId));
    }

    /**
     * 会话列表（未删除，默认当前用户，可按 agentId 筛选）
     */
    @SkAccess
    @GetMapping("/list")
    public R<List<ChatSessionVO>> list(ChatSessionQueryDTO query) {
        return R.data(chatSessionService.listSessions(query));
    }

    /**
     * 分页查询会话（支持 isPinned 筛选）
     */
    @SkAccess
    @GetMapping("/page")
    public R<IPage<ChatSessionVO>> page(ChatSessionQueryDTO query) {
        return R.data(chatSessionService.pageSessions(query));
    }

    /**
     * 会话详情
     */
    @SkAccess
    @GetMapping("/{id}")
    public R<ChatSessionVO> detail(@PathVariable("id") Long id) {
        return R.data(chatSessionService.getSessionDetail(id));
    }

    /**
     * 置顶会话
     */
    @SkAccess
    @PutMapping("/{id}/pin")
    public R<Void> pin(@PathVariable("id") Long id) {
        chatSessionService.pinSession(id);
        return R.success("操作成功");
    }

    /**
     * 取消置顶会话
     */
    @SkAccess
    @PutMapping("/{id}/unpin")
    public R<Void> unpin(@PathVariable("id") Long id) {
        chatSessionService.unpinSession(id);
        return R.success("操作成功");
    }

    /**
     * 更新会话标题
     */
    @SkAccess
    @PutMapping("/{id}/title")
    public R<Void> updateTitle(@PathVariable("id") Long id, @RequestParam("title") String title) {
        chatSessionService.updateTitle(id, title);
        return R.success("操作成功");
    }

    /**
     * 删除会话
     */
    @SkAccess
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable("id") Long id) {
        chatSessionService.deleteSession(id);
        return R.success("操作成功");
    }
}
