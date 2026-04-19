# 工具管理

工具管理模块是智能体能力的"执行层扩展中枢"，通过将内部系统能力、外部 API 服务、自定义业务逻辑封装为标准化工具，赋予智能体感知环境、执行操作、处理复杂任务的能力。


## 功能概述

- 支持内置工具和自定义工具两种类型
- 内置工具随系统启动自动注册，自定义工具通过在线编写代码创建
- 支持按分类组织工具
- 工具可标记"需要确认"，在开启记忆时自动触发确认钩子


## 核心概念

### 工具类型

| 类型 | 说明 | 可执行操作 |
|------|------|-----------|
| **内置（BUILTIN）** | 系统预置工具，通过 Java 类路径注册 | 查看详情、启用/禁用 |
| **自定义（CUSTOM）** | 用户在线编写的工具代码 | 完整 CRUD |

### 工具属性

| 属性 | 说明 |
|------|------|
| 名称 | 工具显示名称 |
| 编号 | 工具唯一标识符（小写字母+下划线，如 `get_weather`） |
| 描述 | 工具功能描述，影响模型对工具的理解和选择 |
| 标签/分类 | 工具所属分类 |
| 是否需要确认 | 开启后，智能体调用该工具前需用户确认 |
| 版本号 | 工具版本标识 |
| 输入参数 Schema | 工具入参的 JSON Schema 定义 |
| 代码 | 自定义工具的实现代码 |

### 工具选择策略

智能体可配置工具选择策略，控制模型如何选择调用工具：

- **自动（AUTO）**：由模型自主决定是否调用工具
- **必选（REQUIRED）**：模型必须调用工具
- **指定（SPECIFIC）**：指定调用某个工具
- **无需（NONE）**：不使用工具


## 操作指南

### 创建自定义工具

1. 在工具管理页面点击 **添加新工具** 卡片
2. 填写表单：
   - **标签**：选择或新建分类
   - **名称**：工具名称
   - **编号**：工具标识符（自定义工具必填）
   - **描述**：工具功能说明
   - **是否需要确认**：开关
   - **版本号**：版本标识
   - **输入参数**：定义参数 Schema
   - **代码**：编写工具实现代码
3. 点击确认提交

:::tip
在线工具目前支持同步工具。异步或流式工具请自行在 `com.hxh.apboa.core.tool.builtins` 包下编写。
:::

:::warning 重要
在线编写工具时，**输入参数的顺序必须与 `execute` 方法中接收参数的顺序保持一致**。参数将按照定义的顺序依次传入 `args` 数组。
:::

### 其他操作

- **查看详情**：卡片菜单 → 查看
- **编辑工具**：卡片菜单 → 编辑（仅自定义工具）
- **启用/禁用**：卡片菜单 → 启用/禁用
- **删除工具**：卡片菜单 → 删除（仅自定义工具）

### 搜索与筛选

- **类型筛选**：页面顶部切换全部/内置/自定义
- **分类筛选**：下拉选择分类
- **名称搜索**：输入关键词后回车搜索


## 在智能体中使用

在智能体配置的「工具与能力」步骤中：

1. 在 **工具集** 区域，按分类折叠面板浏览工具
2. 勾选需要的工具（Checkbox 多选）
3. 配置 **工具选择策略**

![工具配置占位](src/doc/images/tool-agent-config.png)


## 工具描述编写技巧

工具描述直接影响模型选择和使用工具的准确性：

- **明确功能**：清楚说明工具能做什么
- **说明入参**：描述每个参数的含义和取值范围
- **给出示例**：在描述中包含调用场景说明
- **标注限制**：说明工具的适用范围和限制条件

### 好的描述示例

```
查询指定城市的当前天气信息，包括温度、湿度、风速和天气状况。
适用于需要了解天气情况的场景，如出行建议、活动规划等。
仅支持中国主要城市查询。
```

### 差的描述示例

```
获取天气
```


## 自定义工具开发

### 参数顺序说明

输入参数的顺序必须与 `execute` 方法中接收参数的顺序保持一致。

**示例：**

假设定义了以下参数 Schema（按顺序）：
1. `num1` - 第一个数字
2. `num2` - 第二个数字
3. `operator` - 运算符

在 `execute` 方法中：
```java
public Object execute(Object... args) {
    String num1Str = args[0].toString();   // 对应第一个参数
    String num2Str = args[1].toString();   // 对应第二个参数
    String operator = args[2].toString();  // 对应第三个参数
    // ...
}
```

### 完整示例：数学计算器

```java
import com.hxh.apboa.core.tool.dynamices.IDynamicAgentTool;
import java.util.*;

public class MathCalculator implements IDynamicAgentTool {

    @Override
    public Object execute(Object... args) {
        // 参数验证
        if (args == null || args.length < 3) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "参数不足，需要3个参数：数字1、数字2、运算符");
            errorMap.put("required_format", "第一个参数: 数字1(字符串), 第二个参数: 数字2(字符串), 第三个参数: 运算符(字符串)");
            return errorMap;
        }

        try {
            // 获取参数（都是字符串类型）
            String num1Str = args[0].toString();
            String num2Str = args[1].toString();
            String operator = args[2].toString();

            // 转换为数字
            double num1 = Double.parseDouble(num1Str);
            double num2 = Double.parseDouble(num2Str);

            // 计算结果
            double result = 0;
            String calculation = "";

            switch (operator) {
                case "+":
                    result = num1 + num2;
                    calculation = num1 + " + " + num2 + " = " + result;
                    break;
                case "-":
                    result = num1 - num2;
                    calculation = num1 + " - " + num2 + " = " + result;
                    break;
                case "*":
                    result = num1 * num2;
                    calculation = num1 + " × " + num2 + " = " + result;
                    break;
                case "/":
                    if (num2 == 0) {
                        throw new ArithmeticException("除数不能为0");
                    }
                    result = num1 / num2;
                    calculation = num1 + " ÷ " + num2 + " = " + result;
                    break;
                case "%":
                    result = num1 % num2;
                    calculation = num1 + " % " + num2 + " = " + result;
                    break;
                case "pow":
                    result = Math.pow(num1, num2);
                    calculation = num1 + " ^ " + num2 + " = " + result;
                    break;
                default:
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", "不支持的运算符: " + operator);
                    errorMap.put("supported_operators", "+, -, *, /, %, pow");
                    return errorMap;
            }

            // 构建返回结果
            Map<String, Object> resMap = new HashMap<String, Object>() {{
                put("num1", num1Str);
                put("num2", num2Str);
                put("operator", operator);
                put("result", result);
                put("calculation", calculation);
                put("timestamp", new Date().toString());
            }};

            return resMap;

        } catch (NumberFormatException e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "数字格式错误，请确保传入有效的数字字符串");
            errorMap.put("message", e.getMessage());
            return errorMap;
        } catch (ArithmeticException e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "数学运算错误");
            errorMap.put("message", e.getMessage());
            return errorMap;
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "未知错误");
            errorMap.put("message", e.getMessage());
            return errorMap;
        }
    }
}
```


## 内置工具开发指南

内置工具通过实现 `IAgentTool` 接口并配合注解完成定义，支持 Spring 容器管理，可享受依赖注入等能力。

### 核心接口：IAgentTool

```java
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.wrapper.ToolInfoWrapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 代理工具接口
 *
 * @author huxuehao
 **/
public interface IAgentTool extends SmartInitializingSingleton {

    default ToolInfoWrapper parseToolInfo() {
        try {
            // 遍历类中的所有方法
            for (Method method : this.getClass().getDeclaredMethods()) {
                // 检查方法是否有 @Tool 注解
                if (method.isAnnotationPresent(Tool.class)) {
                    ToolInfoWrapper toolInfo = new ToolInfoWrapper();
                    toolInfo.setClassPath(this.getClass().getName());

                    Tool toolAnnotation = method.getAnnotation(Tool.class);
                    if (toolAnnotation != null) {
                        toolInfo.setName(toolAnnotation.name());
                        toolInfo.setDescription(toolAnnotation.description());
                    }

                    // 解析方法参数
                    List<ToolInfoWrapper.ParamInfo> params = getParamInfos(method);

                    toolInfo.setParams(params);
                    return toolInfo; // 返回第一个找到的 @Tool 方法
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static List<ToolInfoWrapper.ParamInfo> getParamInfos(Method method) {
        List<ToolInfoWrapper.ParamInfo> params = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        for (Parameter param : parameters) {
            // 检查参数是否有 @ToolParam 注解
            if (param.isAnnotationPresent(ToolParam.class)) {
                ToolParam paramAnnotation = param.getAnnotation(ToolParam.class);
                ToolInfoWrapper.ParamInfo paramInfo = new ToolInfoWrapper.ParamInfo();
                if (paramAnnotation != null) {
                    paramInfo.setName(paramAnnotation.name());
                }
                if (paramAnnotation != null) {
                    paramInfo.setDescription(paramAnnotation.description());
                }
                if (paramAnnotation != null) {
                    paramInfo.setRequired(paramAnnotation.required());
                } else {
                    paramInfo.setRequired(false);
                }
                paramInfo.setType(extractParameterType(param));
                params.add(paramInfo);
            }
        }
        return params;
    }

    private static String extractParameterType(Parameter parameter) {
        Class<?> type = parameter.getType();
        Type genericType = parameter.getParameterizedType();

        // 处理基本类型和包装类
        if (type == int.class || type == Integer.class) {
            return "integer";
        } else if (type == long.class || type == Long.class
                || type == float.class || type == Float.class
                || type == double.class || type == Double.class
                || type == byte.class || type == Byte.class
                || type == short.class || type == Short.class) {
            return "number";
        } else if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }

        // 处理数组类型
        if (type.isArray()) {
            return "array";
        }

        // 处理集合类型（List、Set等）
        if (Collection.class.isAssignableFrom(type)) {
            return "array";
        }

        // 处理Map类型
        if (Map.class.isAssignableFrom(type)) {
            return "object";
        }

        // 处理泛型数组（如List<String>[]）
        if (genericType instanceof GenericArrayType) {
            return "array";
        }

        // 处理参数化类型（泛型）
        if (genericType instanceof ParameterizedType pType) {
            Class<?> rawType = (Class<?>) pType.getRawType();

            if (Collection.class.isAssignableFrom(rawType)) {
                return "array";
            } else if (Map.class.isAssignableFrom(rawType)) {
                return "object";
            }
        }

        // 其他所有类型都返回 string
        return "string";
    }

    /**
     * 将对象转换为json字符串
     */
    default String toJsonString(Object obj) {
        return JsonUtils.toJsonStr(obj);
    }

    default void afterSingletonsInstantiated() {
        ToolsRegister.register(this.getClass().getName(), this);
    }
}
```

### 注解说明

| 注解 | 作用 | 关键属性 |
|------|------|---------|
| `@Tool` | 标记工具方法 | `name`：工具名称<br>`description`：工具描述 |
| `@ToolParam` | 描述方法参数 | `name`：参数名称<br>`description`：参数描述<br>`required`：是否必填（默认 false） |

### 编写步骤

1. **创建工具类**，实现 `IAgentTool` 接口
2. **添加 `@Component` 注解**，交由 Spring 管理
3. **定义工具方法**，使用 `@Tool` 注解标注
4. **定义方法参数**，使用 `@ToolParam` 注解描述参数
5. **返回结果**，建议使用 `R.data()` 包装

### 完整示例：获取当前时间工具

```java
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.core.tool.IAgentTool;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

/**
 * 获取当前时间工具
 *
 * @author huxuehao
 **/
@Component
public class GetCurrentTimeTool implements IAgentTool {

    @Tool(name = "get_current_datetime", description = "获取当前的日期时间")
    public Object getCurrentDateTime(
            @ToolParam(
                    name = "format",
                    description = "日期时间格式，默认值 yyyy-MM-dd HH:mm:ss",
                    required = false)
            String format) {

        if (format == null || format.isEmpty()) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        return R.data(new SimpleDateFormat(format).format(System.currentTimeMillis()));
    }
}
```

### 更多资源

更多内置工具开发相关内容，请参考：[https://java.agentscope.io/zh/task/tool.html](https://java.agentscope.io/zh/task/tool.html)
