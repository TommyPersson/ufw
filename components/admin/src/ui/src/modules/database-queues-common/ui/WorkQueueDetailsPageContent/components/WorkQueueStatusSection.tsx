import { Alert, AlertTitle, ButtonProps } from "@mui/material"
import { CommandButton, DateTimeText } from "../../../../../common/components"
import { WorkQueueDetails } from "../../../models"
import { DatabaseQueueAdapterSettings } from "../../../DatabaseQueueAdapterSettings"
import { getQueueStateColor } from "../../utils/colors"


export const WorkQueueStatusSection = (props: {
  details: WorkQueueDetails | null
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { details, adapterSettings } = props
  if (!details) {
    return null
  }

  const state = details.status.state

  const color = getQueueStateColor(state)

  const actionButtonProps: Partial<ButtonProps> = {
    color: "inherit"
  }

  const action = state === "ACTIVE"
    ? (<CommandButton
        command={adapterSettings.commands.pauseWorkQueueCommand}
        args={{ queueId: details.queueId }}
        {...actionButtonProps}
      />
    ) : (<CommandButton
        command={adapterSettings.commands.unpauseWorkQueueCommand}
        args={{ queueId: details.queueId }}
        {...actionButtonProps}
      />
    )

  const workItemsText = adapterSettings.texts.queueTypePlural.toLowerCase()

  return (
    <Alert severity={color} action={action}>
      <AlertTitle>{state}</AlertTitle>
      {state === "ACTIVE" &&
          <>The queue is active and will process {workItemsText} until paused.</>
      }
      {state === "PAUSED" &&
          <>
              The queue has been paused since{" "}
              <strong><DateTimeText dateTime={details.status.stateChangedAt} /></strong>{" "}
              and will not process {workItemsText} until unpaused.
          </>
      }
    </Alert>
  )
}
