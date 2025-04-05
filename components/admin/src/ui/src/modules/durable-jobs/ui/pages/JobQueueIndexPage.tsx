import { Box, FormControlLabel, Stack, Switch } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { WorkQueueIndexPageContent } from "../../../database-queues-common/ui/WorkQueueIndexPageContent"
import { JobQueueListQuery } from "../../queries"
import { DurableJobsAdapterSettings } from "../../utils"

export const JobQueueIndexPage = () => {

  const adapterSettings = DurableJobsAdapterSettings

  const [showPeriodicJobs, setShowPeriodicJobs] = useState(false)

  const queuesQuery = useQuery(JobQueueListQuery)
  const queues = (queuesQuery.data ?? []).filter(it => showPeriodicJobs || !it.hasOnlyPeriodicJobTypes)

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Jobs" },
    { text: "Job Queues", current: true },
  ], [])

  return (
    <Page
      heading={
        <Stack direction={"row"}>
          <>Job Queues</>
          <Box flex={1} />
          <FormControlLabel
            control={<Switch checked={showPeriodicJobs} onChange={(_, s) => setShowPeriodicJobs(s)} />}
            labelPlacement={"start"}
            label={"Show queues with periodic jobs"}
          />
        </Stack>
      }
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
