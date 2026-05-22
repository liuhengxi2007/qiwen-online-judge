export type EmailAddress = string & { readonly __brand: 'EmailAddress' }
export type PlaintextPassword = string & { readonly __brand: 'PlaintextPassword' }
export type PasswordHash = string & { readonly __brand: 'PasswordHash' }
