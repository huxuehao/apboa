/**
 * 描述：渲染处理器统一导出
 *
 * @author huxuehao
 **/

// 处理器函数
export { codeHandler } from './code-handler'
export { codespanHandler } from './codespan-handler'
export { linkHandler } from './link-handler'
export { imageHandler } from './image-handler'
export { tableHandler } from './table-handler'
export { headingHandler } from './heading-handler'
export { listHandler, listitemHandler } from './list-handler'
export { strongHandler } from './strong-handler'
export { emHandler } from './em-handler'
export { blockquoteHandler } from './blockquote-handler'
export { hrHandler } from './hr-handler'
export { paragraphHandler } from './paragraph-handler'
export { brHandler } from './br-handler'

// 处理器注册表
export {
  RendererHandlerRegistry,
  globalHandlerRegistry,
  type HandlerFunctions,
  type HandlerName,
  type HandlerConfig,
  type HandlerMetadata,
} from './registry'
