import { z } from "zod"


export const aggregateDetailsSchema = z.object({
  id: z.string(),
  type: z.string(),
})

export type AggregateDetails = z.infer<typeof aggregateDetailsSchema>
