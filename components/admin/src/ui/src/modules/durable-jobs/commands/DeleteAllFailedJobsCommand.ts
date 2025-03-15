import { UseMutationOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { queryClient } from "../../../common/utils/tsq"


export const DeleteAllFailedJobsCommand: UseMutationOptions<any, Error, { queueId: string }> = ({
  mutationFn: async ({ queueId }) => {
    await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/actions/delete-all-failed-jobs`, {
      method: "POST"
    })
  },
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ["durable-jobs", "queues"] })
  }
})
