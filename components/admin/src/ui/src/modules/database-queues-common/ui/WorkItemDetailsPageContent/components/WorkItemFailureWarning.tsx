import { Alert } from "@mui/material"
import { WorkItemDetails } from "../../../common/models"
import { DatabaseQueueAdapterSettings } from "../../../DatabaseQueueAdapterSettings"


export const WorkItemFailureWarning = (props: {
  details: WorkItemDetails | null | undefined,
  adapterSettings: DatabaseQueueAdapterSettings,
}) => {

  const { details, adapterSettings } = props

  if (!details || details.numFailures === 0) {
    return null
  }

  const queueType = adapterSettings.texts.queueTypeSingular.toLowerCase()

  return (
    <Alert severity={"warning"}>
      This {queueType} has failed <strong>{details.numFailures}</strong> times. See the failure list below for details.
    </Alert>
  )
}
