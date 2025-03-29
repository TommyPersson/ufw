import PlaylistPlayIcon from "@mui/icons-material/PlaylistPlay"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const RescheduleAllFailedEventsCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-events", "RescheduleAllFailedEvents"],
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/actions/reschedule-all-failed-events`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-events", "queues"] })
    }
  }),
  label: "Reschedule all failed events",
  icon: <PlaylistPlayIcon />,
  errorTitle: "Unable to reschedule events",
  confirmText: ({ queueId }) =>
    <>Are you sure you want to <strong>reschedule</strong> all events in the <code>{queueId}</code> queue?</>,
}
