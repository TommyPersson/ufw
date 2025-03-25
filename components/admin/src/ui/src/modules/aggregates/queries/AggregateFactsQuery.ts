import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { AggregateFact, aggregateFactSchema } from "../models"

export const AggregateFactsQuery: (aggregateId: String, page: number) => UseQueryOptions<PaginatedList<AggregateFact>> = (aggregateId, page) => ({
  queryKey: ["aggregates", "aggregate", aggregateId, "facts", page],
  queryFn: async () => {
    const limit = 100
    const offset = (page - 1) * 100
    return responseSchema.parse(await makeApiRequest(
      `/admin/api/aggregates/aggregates/${aggregateId}/facts?limit=${limit}&offset=${offset}`
    ))
  },
  enabled: aggregateId.trim().length > 0,
  retry: false,
})

const responseSchema = zx.paginatedList(aggregateFactSchema)