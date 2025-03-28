import { z } from "zod"

export const applicationModuleSchema = z.object({
  id: z.string(),
  name: z.string(),
  description: z.string(),
})

export type ApplicationModule = z.infer<typeof applicationModuleSchema>