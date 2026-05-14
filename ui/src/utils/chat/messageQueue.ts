/**
 * 聊天消息队列管理器
 * 按 sessionId 隔离队列，保证同一会话内消息按发送顺序串行入库，
 * 防止先发消息因网络响应延迟而导致入库时间晚于后发消息。
 *
 * 设计要点：
 * - 每个 sessionId 独立 FIFO 队列，不同会话互不阻塞
 * - 入队操作立即返回 Promise，结果在对应任务执行完成后 resolve
 * - 队列处理失败时自动继续下一个任务，不阻塞后续消息
 * - 单任务超时保护，防止网络挂起导致队列永久阻塞
 * - 队列空时自动清理 Map 条目，防止内存泄漏
 */
type QueueTask = {
  execute: () => Promise<unknown>
  resolve: (value: unknown) => void
  reject: (error: unknown) => void
}

/** 单任务最大执行时间（毫秒） */
const TASK_TIMEOUT_MS = 10_000

class ChatMessageQueue {
  private queues = new Map<string, QueueTask[]>()
  private processing = new Map<string, boolean>()

  /**
   * 将消息保存任务加入指定会话的队列
   *
   * @param sessionId 会话 ID
   * @param task      返回 Promise 的异步任务函数
   * @param onSuccess 任务成功时的回调，在 Promise resolve 之前同步触发
   * @returns 任务执行结果的 Promise，按入队顺序依次 resolve
   */
  enqueue<T>(sessionId: string, task: () => Promise<T>, onSuccess?: (result: T) => void): Promise<T> {
    return new Promise((resolve, reject) => {
      const wrappedResolve = (value: unknown) => {
        try {
          onSuccess?.(value as T)
        } catch {
          // 回调异常不影响队列流程
        }
        ;(resolve as (v: unknown) => void)(value)
      }
      const queue = this.queues.get(sessionId) ?? []
      queue.push({ execute: task as () => Promise<unknown>, resolve: wrappedResolve, reject })
      this.queues.set(sessionId, queue)
      this.processQueue(sessionId)
    })
  }

  /**
   * 串行处理指定会话的队列
   */
  private async processQueue(sessionId: string): Promise<void> {
    if (this.processing.get(sessionId)) return
    this.processing.set(sessionId, true)

    try {
      while (true) {
        const queue = this.queues.get(sessionId)
        if (!queue || queue.length === 0) break

        const task = queue.shift()!
        try {
          // 带超时保护执行，防止网络挂起导致队列永久阻塞
          const result = await Promise.race([
            task.execute(),
            new Promise<never>((_, reject) =>
              setTimeout(() => reject(new Error('Task timeout')), TASK_TIMEOUT_MS)
            )
          ])
          task.resolve(result)
        } catch (error) {
          task.reject(error)
        }
      }
    } finally {
      this.processing.set(sessionId, false)
      // 处理期间可能有新任务入队，检查是否需要继续
      const queue = this.queues.get(sessionId)
      if (queue && queue.length > 0) {
        this.processQueue(sessionId)
      } else {
        // 队列已空，清理 Map 条目
        this.queues.delete(sessionId)
        this.processing.delete(sessionId)
      }
    }
  }

  /**
   * 清空指定会话的队列（如中止运行、切换会话等场景）
   */
  clear(sessionId: string): void {
    const queue = this.queues.get(sessionId)
    if (queue) {
      queue.forEach((task) => task.reject(new Error('Queue cleared')))
    }
    this.queues.delete(sessionId)
    this.processing.delete(sessionId)
  }
}

/** 全局单例 */
export const chatMessageQueue = new ChatMessageQueue()
