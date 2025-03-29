import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"
import { routerHolder } from "../../../router"

export const DeleteEventCommand: Command<{ queueId: string, eventId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-events', 'DeleteEvent'],
    mutationFn: async ({ queueId, eventId }) => {
      await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/events/${eventId}/actions/delete`, {
        method: "POST"
      })
    },
    onSuccess: async (_, { queueId }) => {
      await routerHolder.router?.navigate(`/durable-events/queues/${queueId}/details`)
      queryClient.invalidateQueries({ queryKey: ["durable-events", "queues"] }).then()
    }
  }),
  label: "Delete event",
  color: "error",
  icon: <DeleteOutlineIcon />,
  errorTitle: "Unable to delete event",
  confirmText: <>Are you sure you want to <strong>delete</strong> this event?</>,
  confirmColor: "error",
}
