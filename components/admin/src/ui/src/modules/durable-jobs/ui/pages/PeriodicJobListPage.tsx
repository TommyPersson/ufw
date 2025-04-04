import { Card, CardContent, Chip, Divider, Table, TableRow, Typography } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { uniqBy } from "es-toolkit"
import { useMemo } from "react"
import Markdown from "react-markdown"
import {
  ApplicationModuleHeader,
  DateTimeText,
  LinkTableCell,
  Page, PageBreadcrumb,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import { WorkQueueState } from "../../../database-queues-common/models"
import { getQueueStateColor } from "../../../database-queues-common/ui/utils/colors"
import { PeriodicJobListItem } from "../../models/PeriodicJobListItem"
import { PeriodicJobListQuery } from "../../queries/PeriodicJobListQuery"


export const PeriodicJobListPage = () => {
  const periodicJobListQuery = useQuery(PeriodicJobListQuery)
  const periodicJobs = periodicJobListQuery.data ?? []

  const applicationModules = uniqBy(
    periodicJobs.map(it => it.applicationModule),
    (it) => it.id
  )

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Periodic Jobs" },
    { text: "All", current: true },
  ], [])

  return (
    <Page
      heading={"Periodic Jobs"}
      breadcrumbs={breadcrumbs}
      error={periodicJobListQuery.error}
      isLoading={periodicJobListQuery.isFetching}
      onRefresh={periodicJobListQuery.refetch}
      autoRefresh={true}
    >
      {applicationModules.map(module => {
        const periodicJobsInModule = periodicJobs.filter(it => it.applicationModule.id == module.id)
        return (
          <Card>
            <CardContent>
              <ApplicationModuleHeader applicationModule={module} />
            </CardContent>
            <Divider />
            <Table>
              {periodicJobsInModule.map(job => (
                <PeriodicJobRow key={job.type} job={job} />
              ))}
            </Table>
          </Card>
        )
      })}
    </Page>
  )
}

function PeriodicJobRow(props: { job: PeriodicJobListItem }) {
  const { job } = props

  const link = `/durable-jobs/queues/${job.queueId}/details`

  return (
    <TableRow hover>
      <LinkTableCell to={link} style={{ textAlign: "center" }}>
        <QueueStateChip state={job.queueState} />
      </LinkTableCell>
      <LinkTableCell to={link} style={{ maxWidth: 400 }}>
        <PropertyGroup>
          <PropertyText
            title={"Job Type"}
            subtitle={<code>{job.type}</code>}
          />
          <PropertyText
            title={"Description"}
            noSubtitleStyling
            subtitle={job.description ? <Markdown>{job.description}</Markdown> : <em>N/A</em>}
          />
        </PropertyGroup>
      </LinkTableCell>
      <LinkTableCell to={link} style={{ verticalAlign: "top" }}>
        <PropertyGroup>
          <PropertyText
            title={"Cron Expression"}
            subtitle={<code>{job.cronExpression}</code>}
          />
          <PropertyText
            title={"Cron Description"}
            subtitle={job.cronDescription}
          />
        </PropertyGroup>
      </LinkTableCell>
      <LinkTableCell to={link} style={{ verticalAlign: "top" }}>
        <PropertyGroup>
          <PropertyText
            title={"Last Scheduling Attempt"}
            subtitle={<DateTimeText dateTime={job.lastSchedulingAttempt} fallback={<em>N/A</em>} />}
          />
          <PropertyText
            title={"Next Scheduling Attempt"}
            subtitle={<DateTimeText dateTime={job.nextSchedulingAttempt} fallback={<em>N/A</em>} />}
          />
        </PropertyGroup>
      </LinkTableCell>
      <LinkTableCell to={link} style={{ verticalAlign: "top" }}>
        <PropertyGroup>
          <PropertyText
            title={"# Scheduled"}
            subtitle={1}
          />
          <PropertyText
            title={"# Pending"}
            subtitle={2}
          />
        </PropertyGroup>
      </LinkTableCell>
      <LinkTableCell to={link} style={{ verticalAlign: "top" }}>
        <PropertyGroup>
          <PropertyText
            title={"# In Progress"}
            subtitle={3}
          />
          <PropertyText
            title={"# Failed"}
            subtitle={4}
          />
        </PropertyGroup>
      </LinkTableCell>
    </TableRow>
  )
}

const QueueStateChip = (props: { state: WorkQueueState }) => {
  const { state } = props

  return (
    <Chip
      size={"small"}
      color={getQueueStateColor(state)}
      variant={"outlined"}
      label={state}
    />
  )
}