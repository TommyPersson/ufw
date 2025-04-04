import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { adminRequestTypeSchema } from "./AdminRequestType"
import { adminRequestParameterSchema } from "./AdminRequestParameter"

export const adminRequestSchema = z.object({
  name: z.string(),
  description: z.string(),
  className: z.string(),
  fullClassName: z.string(),
  type: adminRequestTypeSchema,
  parameters: adminRequestParameterSchema.array(),
  applicationModule: applicationModuleSchema,
})

export type AdminRequest = z.infer<typeof adminRequestSchema>


