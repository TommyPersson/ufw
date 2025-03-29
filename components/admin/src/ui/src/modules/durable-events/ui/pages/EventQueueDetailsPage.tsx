import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { useParams } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkQueueDetailsPageContent } from "../../../database-queues-common/ui/WorkQueueDetailsPageContent"
import { EventQueueDetailsQuery } from "../../queries"
import { DurableEventsAdapterSettings } from "../../utils"

export const EventQueueDetailsPage = () => {
  const params = useParams<{ queueId: string }>()
  const queueId = params.queueId!

  const adapterSettings = DurableEventsAdapterSettings

  const queuesQuery = useQuery(EventQueueDetailsQuery(queueId!))
  const queueDetails = queuesQuery.data ?? null

  const workQueueDetails = queueDetails ? adapterSettings.transforms.queueDetailsTransform(queueDetails) : null

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Events" },
    { text: "Event Queues", link: "../" },
    { text: <code>{queueId}</code> },
    { text: "Details", current: true }
  ], [queueId])

  return (
    <Page
      heading={<>Event Queue: <code>{queueId}</code></>}
      isLoading={queuesQuery.isFetching}
      onRefresh={queuesQuery.refetch}
      autoRefresh={true}
      breadcrumbs={breadcrumbs}
    >
      <WorkQueueDetailsPageContent
        queueId={queueId}
        queueDetails={workQueueDetails}
        adapterSettings={adapterSettings}
      />
    </Page>
  )
}
