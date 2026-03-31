import type {
  ResourceStatus as ContractResourceStatus,
  ResourceVisibility as ContractResourceVisibility,
} from '@contracts/shared'

export type ResourceVisibility = ContractResourceVisibility

export type ResourceStatus = ContractResourceStatus

export type AuditFields = {
  createdAt: string
  updatedAt: string
}
