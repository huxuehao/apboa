import { markRaw } from 'vue';

// 事件监听器类型
type EventListener<T = any> = (data: T) => void;

// 事件总线接口
interface IEventBus {
  on<T>(type: string, listener: EventListener<T>): () => void;
  off<T>(type: string, listener: EventListener<T>): void;
  emit<T>(type: string, data: T): void;
  once<T>(type: string, listener: EventListener<T>): void;
  clear(type?: string): void;
}

// 单例事件总线
class EventBus implements IEventBus {
  private listeners: Map<string, Set<EventListener>> = new Map();
  private onceListeners: Map<string, Set<EventListener>> = new Map();

  constructor() {
    // 使用markRaw避免Vue对事件总线的响应式代理
    return markRaw(this);
  }

  /**
   * 订阅事件
   * @returns 取消订阅的函数
   */
  on<T>(type: string, listener: EventListener<T>): () => void {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set());
    }
    this.listeners.get(type)!.add(listener);

    // 返回取消订阅函数
    return () => this.off(type, listener);
  }

  /**
   * 取消订阅
   */
  off<T>(type: string, listener: EventListener<T>): void {
    this.listeners.get(type)?.delete(listener);
    this.onceListeners.get(type)?.delete(listener);
  }

  /**
   * 触发事件
   */
  emit<T>(type: string, data: T): void {
    // 触发普通监听器
    this.listeners.get(type)?.forEach(listener => {
      try {
        listener(data);
      } catch (error) {
        console.error(`事件监听器执行失败 [${type}]:`, error);
      }
    });

    // 触发一次性监听器并清理
    const onceSet = this.onceListeners.get(type);
    if (onceSet) {
      onceSet.forEach(listener => {
        try {
          listener(data);
        } catch (error) {
          console.error(`一次性事件监听器执行失败 [${type}]:`, error);
        }
      });
      this.onceListeners.delete(type);
    }
  }

  /**
   * 一次性订阅
   */
  once<T>(type: string, listener: EventListener<T>): void {
    if (!this.onceListeners.has(type)) {
      this.onceListeners.set(type, new Set());
    }
    this.onceListeners.get(type)!.add(listener);
  }

  /**
   * 清理事件监听器
   * @param type 如果不传，清理所有事件
   */
  clear(type?: string): void {
    if (type) {
      this.listeners.delete(type);
      this.onceListeners.delete(type);
    } else {
      this.listeners.clear();
      this.onceListeners.clear();
    }
  }

  /**
   * 获取指定事件的监听器数量
   */
  listenerCount(type: string): number {
    return (this.listeners.get(type)?.size || 0) + (this.onceListeners.get(type)?.size || 0);
  }
}

// 导出单例
export const eventBus = new EventBus();
