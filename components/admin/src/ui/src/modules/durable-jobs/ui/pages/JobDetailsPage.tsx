import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { useParams } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkItemDetailsPageContent } from "../../../database-queues-common/ui/WorkItemDetailsPageContent"
import { JobDetailsQuery, JobFailuresQuery } from "../../queries"
import { DurableJobsAdapterSettings } from "../../utils"


export const JobDetailsPage = () => {
  const params = useParams<{ queueId: string, jobId: string }>()
  const queueId = params.queueId!!
  const jobId = params.jobId!!

  const adapterSettings = DurableJobsAdapterSettings

  const jobDetailsQuery = useQuery(JobDetailsQuery(queueId, jobId))
  const jobFailuresQuery = useQuery(JobFailuresQuery(queueId, jobId))

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Jobs" },
    { text: "Job Queues", link: "../" },
    { text: <code>{queueId}</code>, link: `../queues/${queueId}/details` },
    { text: "Job" },
    { text: <code>{jobId}</code>, current: true },
  ], [queueId, jobId])

  const jobDetails = jobDetailsQuery.data
  const jobFailures = jobFailuresQuery.data?.items ?? []

  const workItemDetails = jobDetails ? adapterSettings.transforms.itemDetailsTransform(jobDetails) : null
  const workItemFailures = jobFailures.map(adapterSettings.transforms.failureTransform)

  const isLoading = jobDetailsQuery.isLoading || jobFailuresQuery.isLoading
  const isFetching = jobDetailsQuery.isFetching || jobFailuresQuery.isFetching

  const onRefresh = () => {
    jobDetailsQuery.refetch().then()
    jobFailuresQuery.refetch().then()
  }

  const content = (
    <WorkItemDetailsPageContent
      details={workItemDetails}
      failures={workItemFailures}
      isLoading={isLoading}
      error={jobDetailsQuery.error}
      adapterSettings={adapterSettings}
    />
  )

  return (
    <Page
      heading={"Job Details"}
      breadcrumbs={breadcrumbs}
      isLoading={isFetching}
      onRefresh={onRefresh}
      autoRefresh={true}
      children={content}
    />
  )
}
