import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import { useParams } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkItemListPageContent } from "../../../database-queues-common/ui/WorkItemListPageContent"
import { JobState } from "../../models"
import { JobListQuery, JobQueueDetailsQuery } from "../../queries"
import { DurableJobsAdapterSettings } from "../../utils"

export const JobListPage = () => {
  const params = useParams<{ queueId: string, jobState: JobState }>()
  const queueId = params.queueId!
  const state = params.jobState!

  const adapterSettings = DurableJobsAdapterSettings

  const [page, setPage] = useState(1)

  const queueDetailsQuery = useQuery(JobQueueDetailsQuery(queueId))
  const jobListQuery = useQuery(JobListQuery(queueId, state, page))

  const workItems = (jobListQuery.data?.items ?? []).map(adapterSettings.transforms.itemListItemTransform)
  const workQueueDetails = queueDetailsQuery.data ? adapterSettings.transforms.queueDetailsTransform(queueDetailsQuery.data) : null

  const handleRefresh = () => {
    jobListQuery.refetch().then()
    queueDetailsQuery.refetch().then()
  }

  const isLoading = queueDetailsQuery.isLoading || jobListQuery.isLoading
  const isFetching = queueDetailsQuery.isFetching || jobListQuery.isFetching

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Jobs" },
    { text: "Job Queues", link: "../" },
    { text: <code>{queueId}</code>, link: `../queues/${queueId}/details` },
    { text: "Jobs" },
    { text: <code>{state}</code>, current: true },
  ], [queueId, state])

  return (
    <Page
      heading={<>Jobs</>}
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
