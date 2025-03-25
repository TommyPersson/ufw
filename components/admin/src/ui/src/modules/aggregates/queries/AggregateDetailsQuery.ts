import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { AggregateDetails, aggregateDetailsSchema } from "../models"

export const AggregateDetailsQuery: (aggregateId: String) => UseQueryOptions<AggregateDetails> = (aggregateId) => ({
  queryKey: ["aggregates", "aggregate", aggregateId, "details"],
  queryFn: async () => {
    return aggregateDetailsSchema.parse(await makeApiRequest(
      `/admin/api/aggregates/aggregates/${aggregateId}/details`
    ))
  },
  enabled: aggregateId.trim().length > 0,
  retry: false,
})
