import { ErrorAlert } from "../../../../../common/components"
import { DatabaseQueueAdapterSettings } from "../../../DatabaseQueueAdapterSettings"

export const LoadingErrorAlert = (props: {error: any, adapterSettings: DatabaseQueueAdapterSettings }) => {
  const { error, adapterSettings } = props

  const queueType = adapterSettings.texts.queueTypeSingular.toLowerCase()

  return (
    <ErrorAlert error={error} title={`Unable to load ${queueType} details`} />
  )
}