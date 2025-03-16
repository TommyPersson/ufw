import { z } from "zod"

export const jobStateSchema = z.enum(["SCHEDULED", "PENDING", "IN_PROGRESS", "SUCCESSFUL", "FAILED"])

export type JobState = z.infer<typeof jobStateSchema>