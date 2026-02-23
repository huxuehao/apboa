package com.hxh.apboa.agent.service;

import com.hxh.apboa.common.dto.ChatMessageAppendDTO;
import com.hxh.apboa.common.dto.ChatSessionCreateDTO;
import com.hxh.apboa.common.dto.ChatSessionQueryDTO;
import com.hxh.apboa.common.entity.ChatSession;
import com.hxh.apboa.common.vo.ChatMessageVO;
import com.hxh.apboa.common.vo.ChatSessionVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 聊天会话 Service
 *
 * @author huxuehao
 */
public interface ChatSessionService extends IService<ChatSession> {

    /**
     * 创建新会话：插入根消息并设置 current_message_id
     *
     * @param dto 创建参数（agentId、title）
     * @return 会话 VO
     */
    ChatSessionVO createSession(ChatSessionCreateDTO dto);

    /**
     * 正常对话追加：在 current_message_id 后插入新消息并更新 current_message_id
     *
     * @param sessionId 会话 ID
     * @param dto       消息内容（role、content）
     * @return 新消息 VO
     */
    ChatMessageVO appendMessage(Long sessionId, ChatMessageAppendDTO dto);

    /**
     * 重新生成：以当前消息为父节点插入新消息（新分支），并更新 current_message_id
     *
     * @param sessionId 会话 ID
     * @param dto       消息内容（role、content）
     * @return 新消息 VO
     */
    ChatMessageVO regenerateMessage(Long sessionId, ChatMessageAppendDTO dto);

    /**
     * 切换历史分支：仅更新 current_message_id
     *
     * @param sessionId 会话 ID
     * @param messageId 目标消息 ID
     */
    void switchCurrentMessage(Long sessionId, Integer messageId);

    /**
     * 回显当前完整对话：根据 current_message_id 取 path，按 path 的 id 列表查询并按 depth 排序
     *
     * @param sessionId 会话 ID
     * @return 当前路径上的消息列表（按深度升序）
     */
    List<ChatMessageVO> getCurrentMessages(Long sessionId);

    /**
     * 分页或列表查询会话（未删除，可按 userId、agentId 筛选）
     *
     * @param query 查询条件
     * @return 会话 VO 列表
     */
    List<ChatSessionVO> listSessions(ChatSessionQueryDTO query);

    /**
     * 会话详情
     *
     * @param id 会话 ID
     * @return 会话 VO，不存在返回 null
     */
    ChatSessionVO getSessionDetail(Long id);

    /**
     * 置顶会话
     *
     * @param id 会话 ID
     */
    void pinSession(Long id);

    /**
     * 取消置顶会话
     *
     * @param id 会话 ID
     */
    void unpinSession(Long id);

    /**
     * 更新会话标题
     *
     * @param id    会话 ID
     * @param title 新标题
     */
    void updateTitle(Long id, String title);

    /**
     * 删除会话（物理删除会话及其消息）
     *
     * @param id 会话 ID
     */
    void deleteSession(Long id);
}
