import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { useParams } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkQueueDetailsPageContent } from "../../../database-queues-common/ui/WorkQueueDetailsPageContent"
import { JobQueueDetailsQuery } from "../../queries"
import { DurableJobsAdapterSettings } from "../../utils"

export const JobQueueDetailsPage = () => {
  const params = useParams<{ queueId: string }>()
  const queueId = params.queueId!

  const adapterSettings = DurableJobsAdapterSettings

  const queuesQuery = useQuery(JobQueueDetailsQuery(queueId!))
  const queueDetails = queuesQuery.data ?? null

  const workQueueDetails = queueDetails ? adapterSettings.transforms.queueDetailsTransform(queueDetails) : null

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Jobs" },
    { text: "Job Queues", link: "../" },
    { text: <code>{queueId}</code> },
    { text: "Details", current: true }
  ], [queueId])

  return (
    <Page
      heading={<>Job Queue: <code>{queueId}</code></>}
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
