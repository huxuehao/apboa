/**
 * 描述：渲染器扩展模块统一导出
 *
 * @author huxuehao
 **/

export {
  createRendererExtension,
  rendererExtension,
  RendererHandlerRegistry,
  globalHandlerRegistry,
  type RendererExtensionConfig,
  type HandlerConfig,
  type HandlerFunctions,
  type HandlerName,
} from './renderer-extension'
export * from './handlers'
