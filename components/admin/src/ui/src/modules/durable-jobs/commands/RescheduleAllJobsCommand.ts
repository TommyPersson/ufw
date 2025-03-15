import { UseMutationOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { queryClient } from "../../../common/utils/tsq"


export const RescheduleAllFailedJobsCommand: UseMutationOptions<any, Error, { queueId: string }> = ({
  mutationFn: async ({ queueId }) => {
    await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/actions/reschedule-all-failed-jobs`, {
      method: "POST"
    })
  },
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ["durable-jobs", "queues"] })
  }
})
