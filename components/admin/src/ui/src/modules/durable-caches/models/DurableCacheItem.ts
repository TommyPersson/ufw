import { z } from "zod"

export const durableCacheItemSchema = z.object({
  id: z.string(),
  title: z.string(),
  description: z.string(),
  numEntries: z.number(),
})

export type DurableCacheItem = z.infer<typeof durableCacheItemSchema>