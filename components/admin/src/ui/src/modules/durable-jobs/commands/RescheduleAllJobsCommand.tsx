import PlaylistPlayIcon from "@mui/icons-material/PlaylistPlay"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const RescheduleAllFailedJobsCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/actions/reschedule-all-failed-jobs`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-jobs", "queues"] })
    }
  }),
  label: "Reschedule all failed jobs",
  icon: <PlaylistPlayIcon />,
  errorTitle: "Unable to reschedule jobs",
  confirmText: <>Are you sure you want to <strong>reschedule</strong> all jobs?</>,
}
