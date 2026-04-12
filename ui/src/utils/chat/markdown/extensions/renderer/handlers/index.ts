/**
 * 描述：渲染处理器统一导出
 *
 * @author huxuehao
 **/

// 处理器函数
export { codeHandler } from './code-handler'
export { linkHandler } from './link-handler'
export { imageHandler } from './image-handler'
export { tableHandler } from './table-handler'
export { headingHandler } from './heading-handler'
export { listHandler, listitemHandler } from './list-handler'

// 处理器注册表
export {
  RendererHandlerRegistry,
  globalHandlerRegistry,
  type HandlerFunctions,
  type HandlerName,
  type HandlerConfig,
  type HandlerMetadata,
} from './registry'
