import { z } from "zod"
import { zx } from "../../../common/utils/zod"

export const durableCacheItemSchema = z.object({
  id: z.string(),
  title: z.string(),
  description: z.string(),
  containsSensitiveData: z.boolean(),
  expirationDuration: zx.duration.nullable(),
  inMemoryExpirationDuration: zx.duration.nullable(),
  numEntries: z.number(),
})

export type DurableCacheItem = z.infer<typeof durableCacheItemSchema>