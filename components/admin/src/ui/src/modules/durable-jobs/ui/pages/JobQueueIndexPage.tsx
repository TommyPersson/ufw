import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkQueueIndexPageContent } from "../../../database-queues-common/ui/WorkQueueIndexPageContent"
import { JobQueueListQuery } from "../../queries"
import { DurableJobsAdapterSettings } from "../../utils"

export const JobQueueIndexPage = () => {

  const adapterSettings = DurableJobsAdapterSettings

  const queuesQuery = useQuery(JobQueueListQuery)
  const queues = queuesQuery.data ?? []

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Jobs" },
    { text: "Job Queues", current: true },
  ], [])

  return (
    <Page
      heading={"Job Queues"}
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
