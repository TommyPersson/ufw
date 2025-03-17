import PlaylistPlayIcon from "@mui/icons-material/PlaylistPlay"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const RescheduleJobNowCommand: Command<{ queueId: string, jobId: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-jobs", "RescheduleJobNow"],
    mutationFn: async ({ queueId, jobId }) => {
      await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/jobs/${jobId}/actions/reschedule-now`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      queryClient.invalidateQueries({ queryKey: ["durable-jobs", "queues"] }).then()
    }
  }),
  label: "Reschedule job",
  color: "primary",
  icon: <PlaylistPlayIcon />,
  errorTitle: "Unable to reschedule job",
}
