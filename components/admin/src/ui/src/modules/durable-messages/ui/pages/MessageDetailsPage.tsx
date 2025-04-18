import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { useParams } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkItemDetailsPageContent } from "../../../database-queues-common/ui/WorkItemDetailsPageContent"
import { MessageDetailsQuery, MessageFailuresQuery } from "../../queries"
import { DurableMessagesAdapterSettings } from "../../utils"


export const MessageDetailsPage = () => {
  const params = useParams<{ queueId: string, messageId: string }>()
  const queueId = params.queueId!!
  const messageId = params.messageId!!

  const adapterSettings = DurableMessagesAdapterSettings

  const messageDetailsQuery = useQuery(MessageDetailsQuery(queueId, messageId))
  const messageFailuresQuery = useQuery(MessageFailuresQuery(queueId, messageId))

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Messages" },
    { text: "Message Queues", link: "../" },
    { text: <code>{queueId}</code>, link: `../queues/${queueId}/details` },
    { text: "Message" },
    { text: <code>{messageId}</code>, current: true },
  ], [queueId, messageId])

  const messageDetails = messageDetailsQuery.data
  const messageFailures = messageFailuresQuery.data?.items ?? []

  const workItemDetails = messageDetails ? adapterSettings.transforms.itemDetailsTransform(messageDetails) : null
  const workItemFailures = messageFailures.map(adapterSettings.transforms.failureTransform)

  const isLoading = messageDetailsQuery.isLoading || messageFailuresQuery.isLoading
  const isFetching = messageDetailsQuery.isFetching || messageFailuresQuery.isFetching

  const onRefresh = () => {
    messageDetailsQuery.refetch().then()
    messageFailuresQuery.refetch().then()
  }

  return (
    <Page
      heading={"Message Details"}
      breadcrumbs={breadcrumbs}
      isLoading={isFetching}
      onRefresh={onRefresh}
      autoRefresh={true}
    >
      <WorkItemDetailsPageContent
        details={workItemDetails}
        failures={workItemFailures}
        isLoading={isLoading}
        error={messageDetailsQuery.error}
        adapterSettings={adapterSettings}
      />
    </Page>

  )
}
