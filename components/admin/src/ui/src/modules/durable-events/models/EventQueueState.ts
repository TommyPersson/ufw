import { z } from "zod"

export const eventQueueStateSchema = z.enum([
  "ACTIVE",
  "PAUSED",
])

export type EventQueueState = z.infer<typeof eventQueueStateSchema>

