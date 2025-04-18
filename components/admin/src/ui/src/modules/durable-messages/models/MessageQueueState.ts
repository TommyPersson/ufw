import { z } from "zod"

export const messageQueueStateSchema = z.enum([
  "ACTIVE",
  "PAUSED",
])

export type MessageQueueState = z.infer<typeof messageQueueStateSchema>

