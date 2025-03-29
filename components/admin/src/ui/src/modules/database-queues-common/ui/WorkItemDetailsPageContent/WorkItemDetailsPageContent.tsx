import { WorkItemDetails, WorkItemFailure } from "../../common/models"
import { DatabaseQueueAdapterSettings } from "../../DatabaseQueueAdapterSettings"
import {
  LoadingErrorAlert,
  WorkItemActionsSection, WorkItemDataSection,
  WorkItemDetailsSection, WorkItemFailuresSection,
  WorkItemFailureWarning,
  WorkItemStateSection, WorkItemTimelineSection
} from "./components"

export const WorkItemDetailsPageContent = (props: {
  details: WorkItemDetails | null
  failures: WorkItemFailure[]
  isLoading: boolean
  error: any | null
  adapterSettings: DatabaseQueueAdapterSettings
}) => {

  const {details, failures, isLoading, error, adapterSettings}= props

  return error ? (
    <LoadingErrorAlert error={error} adapterSettings={adapterSettings} />
  ) : (
    <>
      <WorkItemStateSection
        isLoading={isLoading}
        details={details}
        lastFailure={failures[0]}
        adapterSettings={adapterSettings}
      />
      <WorkItemActionsSection details={details} adapterSettings={adapterSettings} />
      <WorkItemFailureWarning details={details} adapterSettings={adapterSettings} />
      <WorkItemDetailsSection isLoading={isLoading} details={details} />
      <WorkItemDataSection isLoading={isLoading} details={details} />
      <WorkItemFailuresSection isLoading={isLoading} failures={failures} />
      <WorkItemTimelineSection />
    </>
  )
}