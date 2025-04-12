import PlaylistPlayIcon from "@mui/icons-material/PlaylistPlay"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"
import { navigate } from "../../../router"

export const SchedulePeriodicJobNowCommand: Command<{ queueId: string, jobType: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-jobs", "SchedulePeriodicJobNow"],
    mutationFn: async ({ queueId, jobType }) => {
      return await makeApiRequest<{ jobId: string }>(`/admin/api/durable-jobs/periodic-jobs/${queueId}/${jobType}/actions/schedule-now`, {
        method: "POST"
      })
    },
    onSuccess: async ({ jobId }, { queueId }) => {
      await navigate(`/durable-jobs/queues/${queueId}/jobs/by-id/${jobId}/details`)
      queryClient.invalidateQueries({ queryKey: ["durable-jobs"] }).then()
    }
  }),
  label: "Schedule Now",
  color: "primary",
  icon: <PlaylistPlayIcon />,
  errorTitle: "Unable to schedule periodic job",
  confirmColor: "primary",
  confirmText: ({ jobType }) =>
    <>Are you sure you want to <strong>schedule</strong> a <code>{jobType}</code> now?</>
}
