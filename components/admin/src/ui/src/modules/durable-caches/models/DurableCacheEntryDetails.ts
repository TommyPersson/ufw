import { z } from "zod"
import { zx } from "../../../common/utils/zod"

export const durableCacheEntryDetailsSchema = z.object({
  key: z.string(),
  content: z.string(),
  contentType: z.string().nullable(),
  cachedAt: zx.dateTime,
  expiresAt: zx.dateTime.nullable()
})

export type DurableCacheEntryDetails = z.infer<typeof durableCacheEntryDetailsSchema>