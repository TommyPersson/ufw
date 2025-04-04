import { z } from "zod"

export const adminRequestParameterTypeSchema = z.enum(["INTEGER", "STRING", "BOOLEAN"])

export type AdminRequestParameterType = z.infer<typeof adminRequestParameterTypeSchema>