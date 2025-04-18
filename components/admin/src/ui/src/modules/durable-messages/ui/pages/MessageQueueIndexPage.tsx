import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkQueueIndexPageContent } from "../../../database-queues-common/ui/WorkQueueIndexPageContent"
import { MessageQueueListQuery } from "../../queries"
import { DurableMessagesAdapterSettings } from "../../utils"

export const MessageQueueIndexPage = () => {

  const adapterSettings = DurableMessagesAdapterSettings

  const queuesQuery = useQuery(MessageQueueListQuery)
  const queues = queuesQuery.data ?? []

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Messages" },
    { text: "Message Queues", current: true },
  ], [])

  return (
    <Page
      heading={"Message Queues"}
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
