import { uploadChunk } from "@/api/attach";
import { useAccountStore } from "@/stores";
import type { AxiosResponse } from "axios";
import SparkMd5 from "spark-md5";
const useUser = useAccountStore()

// 定义每个分片的大小（10MB）
const CHUNK_SIZE = 1024 * 1024 * 10;

/**
 * 分片对象类型
 */
interface FileChunk {
  start: number; // 分片起始位置
  end: number; // 分片结束位置
  index: number; // 分片索引
  hash: string; // 分片的MD5哈希值
  blob: Blob; // 分片的Blob数据
}

/**
 * 创建文件分片
 * @param file - 文件对象
 * @param chunkIndex - 分片索引
 * @param chunkSize - 分片大小
 * @returns 返回分片对象
 */
export function createFileChunk(file: File, chunkIndex: number, chunkSize: number): Promise<FileChunk> {
  return new Promise((resolve, reject) => {
    // 计算文件切割的起始位置，并切割文件
    const start = chunkIndex * chunkSize;
    const end = start + chunkSize;
    const blob = file.slice(start, end);

    // 用于对分片进行 MD5
    const spark = new SparkMd5.ArrayBuffer();

    // 使用 FileReader 读取文件
    const fileReader = new FileReader();
    // 处理文件读取完的事件
    fileReader.onload = (event: ProgressEvent<FileReader>) => {
      if (event.target?.result) {
        spark.append(event.target.result as ArrayBuffer);
        resolve({
          start,
          end,
          index: chunkIndex,
          hash: spark.end(), // 计算并返回 MD5 值
          blob,
        });
      } else {
        reject(new Error("文件读取失败：结果为 null"));
      }
      // 显式释放 FileReader 对象
      fileReader.onload = null;
      fileReader.onerror = null;
    };

    // 处理文件读取失败的事件
    fileReader.onerror = () => {
      reject(new Error("文件读取失败"));
      // 显式释放 FileReader 对象
      fileReader.onload = null;
      fileReader.onerror = null;
    };

    // 启动文件读取
    fileReader.readAsArrayBuffer(blob);
  });
}

/**
 * 将文件切分为多个分片
 * @param file - 需要分片的文件
 * @returns 返回分片结果的数组
 */
export async function splitFileIntoChunks(file: File): Promise<FileChunk[]> {
  // 计算总的分片数
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE);

  // 创建一个长度为totalChunks的新数组
  const chunkPromises = Array.from({ length: totalChunks }, (_, index) =>
    //这个函数异步地创建文件的分片
    createFileChunk(file, index, CHUNK_SIZE)
  );

  // 等待所有分片生成完成
  return Promise.all(chunkPromises);
}

/**
 * 上传单个分片
 * @param chunk - 分片对象
 * @param fileName - 文件名
 * @param totalChunks - 分片总数
 * @param totalSize - 文件总大小
 */
async function uploadFileChunk(
  chunk: FileChunk,
  fileName: string,
  totalChunks: number,
  totalSize: number): Promise<AxiosResponse<any, any>>
{
  // 文件唯一key
  const uniqueKey = SparkMd5.hash( useUser.accessToken + "_" + fileName + "_" + totalSize)

  const formData = new FormData();
  formData.append("file", chunk.blob);
  formData.append("hash", chunk.hash);
  formData.append("totalSize", totalSize.toString());
  formData.append("index", chunk.index.toString());
  formData.append("uniqueKey", uniqueKey);
  formData.append("fileName", fileName);
  formData.append("totalChunks", totalChunks.toString());

  try {
    // 发送axios请求
    return await uploadChunk(formData);
  } catch (error) {
    console.error(`分片 ${chunk.index} 上传失败:`, error);
    throw error;
  }
}

/**
 * 分片串行上传文件
 * @param file - 需要上传的文件
 */
export async function uploadFileInChunks(file: File): Promise<AxiosResponse<any, any>> {
  try {
    // 进行文件分片
    const chunks = await splitFileIntoChunks(file);
    // 依次上传分片
    let res:any
    for (const chunk of chunks) {
      res = await uploadFileChunk(chunk, file.name, chunks.length, file.size);
    }

    console.log("文件上传完成");
    return res
  } catch (error) {
    console.error("文件上传失败:", error);
    throw error;
  }
}
