import { z } from "zod"

export const durableCacheDetailsSchema = z.object({
  id: z.string(),
  title: z.string(),
  description: z.string(),
  numEntries: z.number(),
})

export type DurableCacheDetails = z.infer<typeof durableCacheDetailsSchema>