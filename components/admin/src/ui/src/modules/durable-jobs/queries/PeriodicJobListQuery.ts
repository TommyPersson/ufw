import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { PeriodicJobListItem, periodicJobListItemSchema } from "../models/PeriodicJobListItem"

export const PeriodicJobListQuery: UseQueryOptions<PeriodicJobListItem[]> = {
  queryKey: ["durable-jobs", "periodic-jobs"],
  queryFn: async () => {
    return periodicJobListItemSchema.array().parse(await makeApiRequest(`/admin/api/durable-jobs/periodic-jobs`))
  },
}



