import { z } from "zod"

export const aggregateFactTypeSchema = z.object({
  type: z.string()
})

export const aggregateDetailsSchema = z.object({
  id: z.string(),
  type: z.string(),
  json: z.string(),
  factTypes: aggregateFactTypeSchema.array()
})

export type AggregateDetails = z.infer<typeof aggregateDetailsSchema>

