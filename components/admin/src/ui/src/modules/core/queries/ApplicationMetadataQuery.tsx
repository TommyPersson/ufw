import { z } from "zod"
import { ApplicationMetadata } from "../models/ApplicationMetadata.ts"
import { UseQueryOptions } from "@tanstack/react-query"

export const ApplicationMetadataQuery: UseQueryOptions<ApplicationMetadata> = {
  queryKey: ["core", "application-metadata"],
  queryFn: async () => {
    return responseSchema.parse(await (await fetch("/admin/api/core/application-metadata")).json())
  },
}

const responseSchema = z.object({
  name: z.string(),
  version: z.string(),
  availableModuleIds: z.string().array()
})