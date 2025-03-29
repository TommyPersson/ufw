import PlaylistPlayIcon from "@mui/icons-material/PlaylistPlay"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const RescheduleEventNowCommand: Command<{ queueId: string, eventId: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-events", "RescheduleEventNow"],
    mutationFn: async ({ queueId, eventId }) => {
      await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/events/${eventId}/actions/reschedule-now`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      queryClient.invalidateQueries({ queryKey: ["durable-events", "queues"] }).then()
    }
  }),
  label: "Reschedule event",
  color: "primary",
  icon: <PlaylistPlayIcon />,
  errorTitle: "Unable to reschedule event",
}
