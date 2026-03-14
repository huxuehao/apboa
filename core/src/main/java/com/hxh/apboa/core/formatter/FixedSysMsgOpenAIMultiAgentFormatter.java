package com.hxh.apboa.core.formatter;

import io.agentscope.core.formatter.openai.OpenAIMultiAgentFormatter;
import io.agentscope.core.formatter.openai.dto.OpenAIMessage;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ThinkingBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * 确保 system 消息始终在消息列表的最前面，以兼容 SGLang 等严格部署环境
 *
 * @author huxuehao
 */
public class FixedSysMsgOpenAIMultiAgentFormatter extends OpenAIMultiAgentFormatter {

    public FixedSysMsgOpenAIMultiAgentFormatter() {
        super();
    }

    @Override
    protected List<OpenAIMessage> doFormat(List<Msg> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return new ArrayList<>();
        }

        // 分离 system消息和其他消息
        List<List<ContentBlock>> allSystemContents = new ArrayList<>();
        List<Msg> otherMsgs = new ArrayList<>();

        for (Msg msg : msgs) {
            if (MsgRole.SYSTEM.equals(msg.getRole())) {
                if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                    allSystemContents.add(new ArrayList<>(msg.getContent()));
                }
            } else {
                otherMsgs.add(msg);
            }
        }

        // 合并所有system消息的内容块
        List<Msg> sortedMsgs = new ArrayList<>();
        if (!allSystemContents.isEmpty()) {
            // 合并所有内容块到一个列表中
            List<ContentBlock> mergedContentBlocks = new ArrayList<>();
            for (List<ContentBlock> contentBlocks : allSystemContents) {
                mergedContentBlocks.addAll(contentBlocks);
            }

            // 创建合并后的 system消息
            Msg mergedSystemMsg = Msg.builder()
                .role(MsgRole.SYSTEM)
                .content(mergedContentBlocks)
                .build();
            sortedMsgs.add(mergedSystemMsg);
        }

        // 添加其他消息
        sortedMsgs.addAll(otherMsgs);

        // 调用父类进行实际的格式化处理
        List<OpenAIMessage> result = new ArrayList<>();
        for (Msg msg : sortedMsgs) {
            boolean hasMedia = hasMediaContent(msg);
            OpenAIMessage openAIMsg = messageConverter.convertToMessage(msg, hasMedia);
            if (openAIMsg != null) {
                // 当消息内容中有 ThinkingBlock 时，清除 reasoningContent 和 ReasoningDetails 的内容，以实现对本地模型的最大兼容性，
                // 例如： SGLang 部署的本地 Qwen 模型不支持该字段
                if (msg.getRole() == MsgRole.ASSISTANT
                        && msg.getContent() != null
                        && msg.getContent().stream().anyMatch(b -> b instanceof ThinkingBlock)) {
                    openAIMsg.setReasoningContent(null);
                    openAIMsg.setReasoningDetails(null);
                }
                result.add(openAIMsg);
            }
        }

        return result;
    }
}
