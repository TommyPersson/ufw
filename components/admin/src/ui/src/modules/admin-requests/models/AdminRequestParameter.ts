import { z } from "zod"
import { adminRequestParameterTypeSchema } from "./AdminRequestParameterType"

export const adminRequestParameterSchema = z.object({
  field: z.string(),
  displayName: z.string(),
  type: adminRequestParameterTypeSchema,
  helperText: z.string().nullable(),
  required: z.boolean(),
  defaultValue: z.string().nullable(),
})

export type AdminRequestParameter = z.infer<typeof adminRequestParameterSchema>