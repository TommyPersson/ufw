import { z } from "zod"

export const messageStateSchema = z.enum([
  "SCHEDULED",
  "PENDING",
  "IN_PROGRESS",
  "CANCELLED",
  "SUCCESSFUL",
  "FAILED"
])

export type MessageState = z.infer<typeof messageStateSchema>