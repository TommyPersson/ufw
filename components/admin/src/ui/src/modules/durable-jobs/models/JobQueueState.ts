import { z } from "zod"

export const jobQueueStateSchema = z.enum([
  "ACTIVE",
  "PAUSED",
])

export type JobQueueState = z.infer<typeof jobQueueStateSchema>

