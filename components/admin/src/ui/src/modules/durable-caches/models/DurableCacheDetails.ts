import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { zx } from "../../../common/utils/zod"

export const durableCacheDetailsSchema = z.object({
  id: z.string(),
  title: z.string(),
  description: z.string(),
  containsSensitiveData: z.boolean(),
  expirationDuration: zx.duration.nullable(),
  inMemoryExpirationDuration: zx.duration.nullable(),
  numEntries: z.number(),
  applicationModule: applicationModuleSchema,
})

export type DurableCacheDetails = z.infer<typeof durableCacheDetailsSchema>