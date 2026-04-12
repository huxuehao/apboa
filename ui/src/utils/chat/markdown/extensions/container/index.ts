/**
 * 描述：自定义容器扩展模块统一导出
 *
 * @author huxuehao
 **/

export { ContainerExtension, createContainerExtension, containerExtension } from './container-extension'
export type {
  ContainerType,
  ContainerTypeConfig,
  ContainerToken,
  ContainerExtensionConfig,
} from './types'
export { defaultContainerConfigs } from './types'
