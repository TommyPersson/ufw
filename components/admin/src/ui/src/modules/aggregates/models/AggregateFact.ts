import { z } from "zod"
import { zx } from "../../../common/utils/zod"

export const aggregateFactSchema = z.object({
  id: z.string(),
  aggregateId: z.string(),
  type: z.string(),
  json: z.string(),
  timestamp: zx.dateTime,
  version: z.number()
})

export type AggregateFact = z.infer<typeof aggregateFactSchema>
