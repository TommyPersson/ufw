import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { useParams } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkItemDetailsPageContent } from "../../../database-queues-common/ui/WorkItemDetailsPageContent"
import { EventDetailsQuery, EventFailuresQuery } from "../../queries"
import { DurableEventsAdapterSettings } from "../../utils"


export const EventDetailsPage = () => {
  const params = useParams<{ queueId: string, eventId: string }>()
  const queueId = params.queueId!!
  const eventId = params.eventId!!

  const adapterSettings = DurableEventsAdapterSettings

  const eventDetailsQuery = useQuery(EventDetailsQuery(queueId, eventId))
  const eventFailuresQuery = useQuery(EventFailuresQuery(queueId, eventId))

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Events" },
    { text: "Event Queues", link: "../" },
    { text: <code>{queueId}</code>, link: `../queues/${queueId}/details` },
    { text: "Event" },
    { text: <code>{eventId}</code>, current: true },
  ], [queueId, eventId])

  const eventDetails = eventDetailsQuery.data
  const eventFailures = eventFailuresQuery.data?.items ?? []

  const workItemDetails = eventDetails ? adapterSettings.transforms.itemDetailsTransform(eventDetails) : null
  const workItemFailures = eventFailures.map(adapterSettings.transforms.failureTransform)

  const isLoading = eventDetailsQuery.isLoading || eventFailuresQuery.isLoading
  const isFetching = eventDetailsQuery.isFetching || eventFailuresQuery.isFetching

  const onRefresh = () => {
    eventDetailsQuery.refetch().then()
    eventFailuresQuery.refetch().then()
  }

  return (
    <Page
      heading={"Event Details"}
      breadcrumbs={breadcrumbs}
      isLoading={isFetching}
      onRefresh={onRefresh}
      autoRefresh={true}
    >
      <WorkItemDetailsPageContent
        details={workItemDetails}
        failures={workItemFailures}
        isLoading={isLoading}
        error={eventDetailsQuery.error}
        adapterSettings={adapterSettings}
      />
    </Page>

  )
}
