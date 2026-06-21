/**
 * 全局类型声明
 *
 * @deprecated 请使用 @/types 下的具名导出
 */
declare global {
  type TagView = import("@/types/ui").TagView;
  type AppSettings = import("@/types/ui").AppSettings;
  type PageResult<T> = import("@/api/common").PageResult<T>;
  type OptionItem = import("@/api/common").OptionItem;
  type ApiResponse<T = unknown> = import("@/api/common").ApiResponse<T>;
}

export {};
