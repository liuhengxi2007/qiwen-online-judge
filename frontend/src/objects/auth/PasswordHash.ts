/** 后端密码哈希品牌类型；前端只透传，不应从明文密码本地生成。 */
export type PasswordHash = string & { readonly __brand: 'PasswordHash' }
