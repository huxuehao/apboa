package com.hxh.apboa.core.model.checkpiont;

import com.hxh.apboa.common.entity.ModelConfig;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import com.hxh.apboa.common.wrapper.ModelWrapper;
import com.hxh.apboa.core.model.ChatModelFactory;
import com.hxh.apboa.model.service.ModelConfigService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 描述：
 *
 * @author huxuehao
 **/
@RestController
@RequestMapping("/model/config")
@RequiredArgsConstructor
public class CheckModelService {
    private final ModelConfigService modelConfigService;
    private final ChatModelFactory chatModelFactory;

    @RequestMapping("/check/{modelId}")
    public R<CheckModelResult> checkModel(@PathVariable("modelId") Long modelId) {
        // 先更新状态为 CHECKING（独立事务，立即提交）
        updateConnectivityResult(modelId, "CHECKING", null);

        try {

            ModelWrapper config = modelConfigService.getModelWrapperById(modelId);

            ModelConfigWrapper configWrapper = new ModelConfigWrapper();
            config.getConfig().fillModelConfigWrapper(configWrapper);
            config.getProvider().fillModelConfigWrapper(configWrapper);

            Model simpleModel = chatModelFactory.getSimpleModel(configWrapper);

            ReActAgent agent = ReActAgent.builder()
                    .name("CHECK_MODEL_AGENT")
                    .model(simpleModel)
                    .sysPrompt("For user inquiries, you must always respond with \"Connection test successful.\"")
                    .build();

            Msg msg = Msg.builder().textContent("hello").build();
            Msg response = agent.call(msg).block();
            if (response == null || response.getTextContent() == null) {
                // 检测失败：无响应
                updateConnectivityResult(modelId, "FAILED", "模型无响应");
                return R.data(new CheckModelResult(false, "模型无响应"));
            }

            // 检测成功
            updateConnectivityResult(modelId, "CONNECTED", null);
            return R.data(new CheckModelResult(true, "连接成功"));
        } catch (Exception e) {
            // 检测失败：异常
            updateConnectivityResult(modelId, "FAILED", e.getMessage());
            return R.data(new CheckModelResult(false, e.getMessage()));
        }
    }

    /**
     * 更新连接性检测结果到数据库
     */
    private void updateConnectivityResult(Long modelId, String status, String message) {
        ModelConfig entity = new ModelConfig();
        entity.setId(modelId);
        entity.setConnectivityStatus(status);
        entity.setConnectivityMessage(message);
        entity.setLastConnectivityCheck(LocalDateTime.now());
        modelConfigService.updateById(entity);
    }
}
