import { z } from "zod"

export const eventStateSchema = z.enum([
  "SCHEDULED",
  "PENDING",
  "IN_PROGRESS",
  "CANCELLED",
  "SUCCESSFUL",
  "FAILED"
])

export type EventState = z.infer<typeof eventStateSchema>