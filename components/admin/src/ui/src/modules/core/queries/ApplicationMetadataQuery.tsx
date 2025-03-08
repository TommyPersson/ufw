import { ApplicationMetadata } from "../models/ApplicationMetadata.ts"
import { UseQueryOptions } from "@tanstack/react-query"

export const ApplicationMetadataQuery: UseQueryOptions<ApplicationMetadata> = {
  queryKey: ["core", "application-metadata"],
  queryFn: async () => {
    return await (await fetch("/admin/api/core/application-metadata")).json()
  },
}