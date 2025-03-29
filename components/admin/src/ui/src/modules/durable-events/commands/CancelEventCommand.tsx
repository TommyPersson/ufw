import PlaylistRemoveIcon from "@mui/icons-material/PlaylistRemoveOutlined"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const CancelEventCommand: Command<{ queueId: string, eventId: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-events", "CancelEvent"],
    mutationFn: async ({ queueId, eventId }) => {
      await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/events/${eventId}/actions/cancel`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-events", "queues"] })
    }
  }),
  label: "Cancel event",
  color: "error",
  icon: <PlaylistRemoveIcon />,
  errorTitle: "Unable to cancel event",
  confirmText: <>
    Are you sure you want to <strong>cancel</strong> this event?<br />
    You will not be able to reschedule it.
  </>,
  confirmColor: "error",
}
