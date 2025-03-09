import { UseMutationOptions } from "@tanstack/react-query"


export const RescheduleAllFailedJobsCommand: UseMutationOptions<any, Error, { queueId: string }> = ({
  mutationFn: async ({ queueId }) => {
    const response = await fetch(`/admin/api/job-queue/queues/${queueId}/actions/reschedule-all-failed-jobs`, {
      method: "POST"
    })
      // TODO real error handling
    if (!response.ok) {
      throw new Error(response.statusText)
    }
  }
})