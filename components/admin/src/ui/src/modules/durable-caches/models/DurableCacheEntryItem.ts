import { z } from "zod"
import { zx } from "../../../common/utils/zod"

export const durableCacheEntryItemSchema = z.object({
  key: z.string(),
  cachedAt: zx.dateTime,
  expiresAt: zx.dateTime,
})

export type DurableCacheEntryItem = z.infer<typeof durableCacheEntryItemSchema>