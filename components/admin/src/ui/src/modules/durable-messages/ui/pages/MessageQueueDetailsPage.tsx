import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { useParams } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkQueueDetailsPageContent } from "../../../database-queues-common/ui/WorkQueueDetailsPageContent"
import { MessageQueueDetailsQuery } from "../../queries"
import { DurableMessagesAdapterSettings } from "../../utils"

export const MessageQueueDetailsPage = () => {
  const params = useParams<{ queueId: string }>()
  const queueId = params.queueId!

  const adapterSettings = DurableMessagesAdapterSettings

  const queuesQuery = useQuery(MessageQueueDetailsQuery(queueId!))
  const queueDetails = queuesQuery.data ?? null

  const workQueueDetails = queueDetails ? adapterSettings.transforms.queueDetailsTransform(queueDetails) : null

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Messages" },
    { text: "Message Queues", link: "../" },
    { text: <code>{queueId}</code> },
    { text: "Details", current: true }
  ], [queueId])

  return (
    <Page
      heading={<>Message Queue: <code>{queueId}</code></>}
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
