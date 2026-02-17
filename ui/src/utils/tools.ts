import cache from './cache'
import setting from '@/config/setting'

/**
 * 获取第一个叶子节点
 * @param nodes 树形结构数据
 * @returns 第一个叶子节点
 */
const findFirstLeafNode: any = (nodes: any) => {
  if (!nodes || nodes.length === 0) {
    return null;
  }
  for (let i = 0; i < nodes.length; i++) {
    const node = nodes[i];
    if (!node.children || node.children.length === 0) {
      return node;
    }
    const leaf = findFirstLeafNode(node.children);
    if (leaf) {
      return leaf;
    }
  }
  return null;
};

/**
 * 根据路由path获取顶级节点
 * @param nodes 树形结构数据
 * @param path  路由path
 * @returns 第一个叶子节点
 */
const findTopNodeWithRoutePath: any = (nodes: any, path: string) => {
  if (!nodes || nodes.length === 0) {
    return null;
  }
  for(const item of nodes) {
    if(item.path === path) {
      return {...item, hasChildren:item.children?.length > 0 || false, children: null};
    }
    if(item.children && item.children.length > 0) {
      const top = findTopNodeWithRoutePath(item.children, path)
      if(top) {
        return {...item, hasChildren:item.children?.length > 0 || false, children: null};
      }
    }
  }
  return null;
};

/**
 * 根据路由path获取节点
 * @param nodes 树形结构数据
 * @param path  路由path
 * @returns 目标节点
 */
const findNodeWithRoutePath: any = (nodes: any, path: string) => {
  if (!nodes || nodes.length === 0) {
    return null;
  }
  for(const item of nodes) {
    if(item.path === path) {
      return {...item, hasChildren:item.children?.length > 0 || false, children: null};
    }
    if(item.children && item.children.length > 0) {
      const node = findNodeWithRoutePath(item.children, path)
      if(node) {
        return node
      }
    }
  }
  return null;
};

/**
 * 转换请求参数，将对象转成字符串链，主要是为了除了可能存在的问题
 * @param params 请求参数
 */
function tansRequestParams(params: any) {
  let result = ''
  for (const propName of Object.keys(params)) {
    const value = params[propName]
    const part = encodeURIComponent(propName) + '='
    if (value !== null && value !== '' && typeof value !== 'undefined') {
      if (typeof value === 'object') {
        for (const key of Object.keys(value)) {
          if (value[key] !== null && value[key] !== '' && typeof value[key] !== 'undefined') {
            const subPart = encodeURIComponent(propName) + '='
            result += subPart + encodeURIComponent(value[key]) + '&'
          }
        }
      } else {
        result += part + encodeURIComponent(value) + '&'
      }
    }
  }
  return result
}

/**
 * 检测请求是否重复
 * @param url 请求路径
 * @param body 请求体
 */
function checkRequestRepeat(url: string, body: object) {
  const currentReqSession = {
    url: url,
    data: typeof body === "object" ? JSON.stringify(body) : body,
    time: new Date().getTime(),
  };

  const lastReqSession = cache.session.getJSON("lastReqSession");
  if (
    lastReqSession === null ||
    lastReqSession === undefined ||
    lastReqSession === ""
  ) {
    cache.session.setJSON("lastReqSession", currentReqSession);
    return false;
  } else {
    // 上次的请求地址
    const l_url = lastReqSession.url;
    // 上次的请求数据
    const l_data = lastReqSession.data;
    // 上次的请求时间
    const l_time = lastReqSession.time;
    // 间隔时间(ms)，小于此时间视为重复提交
    const interval = setting.repeatReqInterval;
    if (
      l_url === currentReqSession.url &&
      l_data === currentReqSession.data &&
      currentReqSession.time - l_time < interval
    ) {
      cache.session.setJSON("lastReqSession", currentReqSession);
      return true;
    } else {
      cache.session.setJSON("lastReqSession", currentReqSession);
      return false;
    }
  }
}

/**
 * 函数防抖
 * @param func 函数
 * @param delay 防抖间隔
 * @param immediate 是否立即执行
 */
const debounce = (func: (...args: any) => void, delay: number, immediate: boolean) => {
  let timer: any = null;
  return function (this: any, ...args: any) {
    if (timer) clearTimeout(timer);
    if (immediate) {
      const firstRun = !timer;
      timer = setTimeout(() => {
        timer = null;
      }, delay);
      if (firstRun) {
        func.apply(this, args);
      }
    } else {
      timer = setTimeout(() => {
        func.apply(this, args);
      }, delay);
    }
  };
};

/**
 * 格式化日期时间
 * @param time 日期，支持多种格式（number | string | Date）
 * @param pattern 格式化字符串（如：yyyy-MM-dd HH:mm:ss）
 * @returns 格式化后字符串
 */
function formatDate(
  time: number | string | Date,
  pattern: string = "yyyy-MM-dd HH:mm:ss"
) {
  let date: Date;

  // 将 time 转成 Date
  if (typeof time === "string") {
    date = new Date(time);
  } else if (typeof time === "number") {
    date = new Date(time);
  } else {
    date = time;
  }

  // 补充 0
  const pad = (n: number | string) => {
    if (typeof n === "string") {
      return Number(n) < 10 ? "0" + n : n.toString();
    } else {
      return n < 10 ? "0" + n : n.toString();
    }
  };

  // 根据 pattern 格式化日期
  return pattern.replace(/(yyyy|MM|dd|HH|mm|ss|SSS)/g, (match) => {
    switch (match) {
      case "yyyy":
        return date.getFullYear().toString();
      case "MM":
        return pad(date.getMonth() + 1); // 月份从 0 开始，所以需要加 1
      case "dd":
        return pad(date.getDate());
      case "HH":
        return pad(date.getHours());
      case "mm":
        return pad(date.getMinutes());
      case "ss":
        return pad(date.getSeconds());
      case "SSS":
        return pad(date.getMilliseconds());
      default:
        return match;
    }
  });
}

/**
 * 将数据转成树
 * @param data 数据
 * @param id id字段名
 * @param parentId parentId字段名
 * @param children children字段名
 * @returns
 */
function handleTree(
  data: any[],
  id: string,
  parentId: string,
  children: string
) {
  const config = {
    id: id || "id",
    parentId: parentId || "parentId",
    childrenList: children || "children",
  };

  const childrenListMap: any = {};
  const nodeIds: any = {};
  const tree = [];

  for (const d of data) {
    const parentId = d[config.parentId];
    if (childrenListMap[parentId] == null) {
      childrenListMap[parentId] = [];
    }
    nodeIds[d[config.id]] = d;
    childrenListMap[parentId].push(d);
  }

  for (const d of data) {
    const parentId = d[config.parentId];
    if (nodeIds[parentId] == null) {
      tree.push(d);
    }
  }

  for (const t of tree) {
    adaptToChildrenList(t);
  }

  function adaptToChildrenList(o: any) {
    if (childrenListMap[o[config.id]] !== null) {
      o[config.childrenList] = childrenListMap[o[config.id]];
    }

    if (o[config.childrenList]) {
      for (const c of o[config.childrenList]) {
        adaptToChildrenList(c);
      }
    }
  }
  return tree;
}

/**
 * 是否为url
 * @param path 路径
 * @returns 是否
 */
function isUrl(path: string) {
  return /^(https?:|mailto:|tel:)/.test(path);
}

/**
 * 是否为邮箱
 * @param email 邮箱
 * @returns 是否
 */
function isEmail(email: string) {
  const reg =
    /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
  return reg.test(email);
}

/**
 * 是否为空
 * @param obj 对象
 * @returns 是否
 */
function isEmpty(obj: any) {
  if (obj === null || obj === "" || obj === undefined) {
    return true;
  }
  if (obj instanceof Array && obj.length === 0) {
    return true;
  }
  return obj instanceof Object && Object.keys(obj).length == 0;

}

// 格式化文件大小
function formatFileSize(sizeInBytes: number) {
  if (sizeInBytes < 1024) {
    return `${sizeInBytes} B`;
  } else if (sizeInBytes < 1024 * 1024) {
    return `${(sizeInBytes / 1024).toFixed(2)} KB`;
  } else if (sizeInBytes < 1024 * 1024 * 1024) {
    return `${(sizeInBytes / (1024 * 1024)).toFixed(2)} MB`;
  } else {
    return `${(sizeInBytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  }
}

// 格式化java代码
function formatJavaCode(code: string): string {
  const lines = code.split("\n");
  let indentLevel = 0;
  const formattedLines: string[] = [];

  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed === "") {
      formattedLines.push("");
      continue;
    }

    // 处理以 } 开头的情况，减少当前行的缩进
    let currentIndent = indentLevel;
    if (trimmed.startsWith("}")) {
      currentIndent = Math.max(indentLevel - 1, 0);
    }

    // 生成当前行的缩进
    const indent = " ".repeat(currentIndent * 4);
    formattedLines.push(indent + trimmed);

    // 计算下一行的缩进级别
    const openBraces = (trimmed.match(/{/g) || []).length;
    const closeBraces = (trimmed.match(/}/g) || []).length;
    indentLevel += openBraces - closeBraces;
    indentLevel = Math.max(indentLevel, 0); // 缩进级别不能为负数
  }

  return formattedLines.join("\n");
}

/**
 *
 * @param jsonStr JSON 字符串
 * @param indent  缩进
 */
function formatJSON(jsonStr: string, indent: number = 2): string {
  try {
    const obj = JSON.parse(jsonStr);
    // 处理缩进类型：数字转换为空格数，字符串直接使用（如 '\t'）
    const indentChar = " ".repeat(indent);
    return JSON.stringify(obj, null, indentChar);
  } catch (e: any) {
    // 返回带错误信息的字符串
    return `Invalid JSON: ${e.message}`;
  }
}

/**
 * 统计数组 arrA 中包含数组 arrB 元素的数量（包含重复项）
 * @param arrA 主数组
 * @param arrB 参考数组
 * @returns 匹配的元素总数
 */
function countCommonElements<T>(arrA: T[], arrB: T[]): number {
  // 将数组b转为Set，提升查询效率
  const setB = new Set<T>(arrB);
  let count = 0;

  // 遍历数组a，统计在setB中存在的元素数量
  for (const item of arrA) {
    if (setB.has(item)) {
      count++;
    }
  }

  return count;
}

export {
  findFirstLeafNode,
  findTopNodeWithRoutePath,
  findNodeWithRoutePath,
  tansRequestParams,
  checkRequestRepeat,
  debounce,
  formatDate,
  handleTree,
  isUrl,
  isEmail,
  isEmpty,
  formatFileSize,
  formatJavaCode,
  formatJSON,
  countCommonElements
};
