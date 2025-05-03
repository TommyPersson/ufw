import { z } from "zod"

export const adminRequestParameterTypeSchema = z.enum([
  "INTEGER",
  "DECIMAL",
  "STRING",
  "BOOLEAN",
  "LOCAL_DATE",
  "LOCAL_TIME",
  "LOCAL_DATE_TIME",
])

export type AdminRequestParameterType = z.infer<typeof adminRequestParameterTypeSchema>