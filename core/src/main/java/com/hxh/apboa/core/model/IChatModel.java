package com.hxh.apboa.core.model;

import com.hxh.apboa.common.enums.ModelProviderType;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import io.agentscope.core.model.Model;

/**
 * 描述：聊天模型
 *
 * @author huxuehao
 **/
public interface IChatModel {
    Model getModel(ModelConfigWrapper modelConfig);
    ModelProviderType getProvider();
    int order();
}
