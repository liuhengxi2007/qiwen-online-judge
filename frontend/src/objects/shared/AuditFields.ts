import { readRecord, readString } from '@/objects/shared/PageResponse'

export type AuditFields = {
  createdAt: string
  updatedAt: string
}

export function fromAuditFieldsContract(value: unknown, label: string): AuditFields {
  const fields = readRecord(value, label)
  return {
    createdAt: readString(fields.createdAt, `${label} created at`),
    updatedAt: readString(fields.updatedAt, `${label} updated at`),
  }
}
