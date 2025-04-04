import { z } from "zod"
import { adminRequestParameterTypeSchema } from "./AdminRequestParameterType"

export const adminRequestParameterSchema = z.object({
  name: z.string(),
  type: adminRequestParameterTypeSchema,
  description: z.string(),
  required: z.boolean(),
})

export type AdminRequestParameter = z.infer<typeof adminRequestParameterSchema>