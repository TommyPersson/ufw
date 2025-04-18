import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import { useParams } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkItemListPageContent } from "../../../database-queues-common/ui/WorkItemListPageContent"
import { MessageState } from "../../models"
import { MessageListQuery, MessageQueueDetailsQuery } from "../../queries"
import { DurableMessagesAdapterSettings } from "../../utils"

export const MessageListPage = () => {
  const params = useParams<{ queueId: string, messageState: MessageState }>()
  const queueId = params.queueId!
  const state = params.messageState!

  const adapterSettings = DurableMessagesAdapterSettings

  const [page, setPage] = useState(1)

  const queueDetailsQuery = useQuery(MessageQueueDetailsQuery(queueId))
  const messageListQuery = useQuery(MessageListQuery(queueId, state, page))

  const workItems = (messageListQuery.data?.items ?? []).map(adapterSettings.transforms.itemListItemTransform)
  const workQueueDetails = queueDetailsQuery.data ? adapterSettings.transforms.queueDetailsTransform(queueDetailsQuery.data) : null

  const handleRefresh = () => {
    messageListQuery.refetch().then()
    queueDetailsQuery.refetch().then()
  }

  const isLoading = queueDetailsQuery.isLoading || messageListQuery.isLoading
  const isFetching = queueDetailsQuery.isFetching || messageListQuery.isFetching

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Messages" },
    { text: "Message Queues", link: "../" },
    { text: <code>{queueId}</code>, link: `../queues/${queueId}/details` },
    { text: "Messages" },
    { text: <code>{state}</code>, current: true },
  ], [queueId, state])

  return (
    <Page
      heading={<>Messages</>}
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
