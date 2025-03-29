import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import { useParams } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkItemListPageContent } from "../../../database-queues-common/ui/WorkItemListPageContent"
import { EventState } from "../../models"
import { EventListQuery, EventQueueDetailsQuery } from "../../queries"
import { DurableEventsAdapterSettings } from "../../utils"

export const EventListPage = () => {
  const params = useParams<{ queueId: string, eventState: EventState }>()
  const queueId = params.queueId!
  const state = params.eventState!

  const adapterSettings = DurableEventsAdapterSettings

  const [page, setPage] = useState(1)

  const queueDetailsQuery = useQuery(EventQueueDetailsQuery(queueId))
  const eventListQuery = useQuery(EventListQuery(queueId, state, page))

  const workItems = (eventListQuery.data?.items ?? []).map(adapterSettings.transforms.itemListItemTransform)
  const workQueueDetails = queueDetailsQuery.data ? adapterSettings.transforms.queueDetailsTransform(queueDetailsQuery.data) : null

  const handleRefresh = () => {
    eventListQuery.refetch().then()
    queueDetailsQuery.refetch().then()
  }

  const isLoading = queueDetailsQuery.isLoading || eventListQuery.isLoading
  const isFetching = queueDetailsQuery.isFetching || eventListQuery.isFetching

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Events" },
    { text: "Event Queues", link: "../" },
    { text: <code>{queueId}</code>, link: `../queues/${queueId}/details` },
    { text: "Events" },
    { text: <code>{state}</code>, current: true },
  ], [queueId, state])

  return (
    <Page
      heading={<>Events</>}
      isLoading={isFetching}
      onRefresh={handleRefresh}
      breadcrumbs={breadcrumbs}
    >
      <WorkItemListPageContent
        queueId={queueId}
        state={state}
        workQueueDetails={workQueueDetails}
        workItems={workItems}
        page={page}
        setPage={setPage}
        isLoading={isLoading}
        adapterSettings={adapterSettings}
      />
    </Page>
  )
}
