import { WorkQueueDetails } from "../../models"
import { DatabaseQueueAdapterSettings } from "../../DatabaseQueueAdapterSettings"
import { WorkItemTypesSection, WorkQueueActionsSection, WorkQueueStatisticsSection } from "./components"
import { WorkQueueStatusSection } from "./components/WorkQueueStatusSection"

export const WorkQueueDetailsPageContent = (props: {
  queueId: string
  queueDetails: WorkQueueDetails | null
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { queueId, queueDetails, adapterSettings } = props

  return (
    <>
      <WorkQueueStatusSection
        details={queueDetails}
        adapterSettings={adapterSettings}
      />
      <WorkQueueStatisticsSection
        details={queueDetails}
        adapterSettings={adapterSettings}
      />
      <WorkQueueActionsSection
        queueId={queueId!}
        adapterSettings={adapterSettings}
      />
      <WorkItemTypesSection
        workItemTypes={queueDetails?.workItemTypes ?? []}
        adapterSettings={adapterSettings}
      />
    </>
  )
}