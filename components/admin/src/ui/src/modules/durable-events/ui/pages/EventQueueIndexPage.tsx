import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkQueueIndexPageContent } from "../../../database-queues-common/ui/WorkQueueIndexPageContent"
import { EventQueueListQuery } from "../../queries"
import { DurableEventsAdapterSettings } from "../../utils"

export const EventQueueIndexPage = () => {

  const adapterSettings = DurableEventsAdapterSettings

  const queuesQuery = useQuery(EventQueueListQuery)
  const queues = queuesQuery.data ?? []

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Events" },
    { text: "Event Queues", current: true },
  ], [])

  return (
    <Page
      heading={"Event Queues"}
      isLoading={queuesQuery.isFetching}
      onRefresh={queuesQuery.refetch}
      breadcrumbs={breadcrumbs}
    >
      <WorkQueueIndexPageContent
        queues={queues}
        error={queuesQuery.error}
        adapterSettings={adapterSettings}
      />
    </Page>
  )
}
