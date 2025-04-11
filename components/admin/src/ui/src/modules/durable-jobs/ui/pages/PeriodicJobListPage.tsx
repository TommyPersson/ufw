import AccessAlarmOutlinedIcon from "@mui/icons-material/AccessAlarmOutlined"
import CalendarMonthOutlinedIcon from "@mui/icons-material/CalendarMonthOutlined"
import CheckCircleOutlinedIcon from "@mui/icons-material/CheckCircleOutlined"
import HelpOutlineOutlinedIcon from "@mui/icons-material/HelpOutlineOutlined"
import PendingOutlinedIcon from "@mui/icons-material/PendingOutlined"
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined"
import { Card, CardContent, Chip, Divider, Table, TableBody, TableCell, TableRow } from "@mui/material"
import { SvgIconOwnProps } from "@mui/material/SvgIcon/SvgIcon"
import { useQuery } from "@tanstack/react-query"
import { uniqBy } from "es-toolkit"
import * as React from "react"
import { useMemo } from "react"
import Markdown from "react-markdown"
import {
  ApplicationModuleHeader,
  CommandMenuItem,
  DateTimeText,
  LinkTableCell,
  MoreOptionsMenuButton,
  Page,
  PageBreadcrumb,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import { WorkQueueState } from "../../../database-queues-common/models"
import { getQueueStateColor } from "../../../database-queues-common/ui/utils/colors"
import { getQueueStateIcon } from "../../../database-queues-common/ui/utils/icons"
import { PauseJobQueueCommand, UnpauseJobQueueCommand } from "../../commands"
import { SchedulePeriodicJobNowCommand } from "../../commands/SchedulePeriodicJobNowCommand"
import { JobState } from "../../models"
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
          <Card key={module.id}>
            <CardContent>
              <ApplicationModuleHeader applicationModule={module} />
            </CardContent>
            <Divider />
            <Table>
              <TableBody>
                {periodicJobsInModule.map(job => (
                  <PeriodicJobRow key={job.type} job={job} />
                ))}
              </TableBody>
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
            title={"# Failed"}
            subtitle={job.queueNumFailures}
          />
          <PropertyText
            title={"Last Execution"}
            subtitle={
              <Chip
                label={<DateTimeText dateTime={job.lastExecutionStateChangeTimestamp} fallback={<em>N/A</em>} />}
                icon={<JobStateIcon
                  state={job.lastExecutionState}
                  color={"inherit" /* must be set to override chip default*/}
                />}
                size={"small"}
              />
            }
          />
        </PropertyGroup>
      </LinkTableCell>
      <TableCell>
        <MoreOptionsMenuButton>
          <CommandMenuItem
            command={SchedulePeriodicJobNowCommand}
            args={{ queueId: job.queueId, jobType: job.type }}
          />
          {job.queueState === "ACTIVE" && (
            <CommandMenuItem
              command={PauseJobQueueCommand}
              args={{ queueId: job.queueId }}
            />
          )}
          {job.queueState === "PAUSED" && (
            <CommandMenuItem
              command={UnpauseJobQueueCommand}
              args={{ queueId: job.queueId }}
            />
          )}
        </MoreOptionsMenuButton>
      </TableCell>
    </TableRow>
  )
}

const QueueStateChip = (props: { state: WorkQueueState }) => {
  const { state } = props

  return (
    <Chip
      size={"small"}
      color={getQueueStateColor(state)}
      icon={getQueueStateIcon(state)}
      variant={"outlined"}
      label={state}
    />
  )
}

const JobStateIcon = React.forwardRef((props: {
  state: JobState | null | undefined,
} & Partial<SvgIconOwnProps>, ref: any) => {
  const { state, ...iconProps } = props

  switch (state) {
    case "SUCCESSFUL":
      return <CheckCircleOutlinedIcon ref={ref} {...iconProps} color={"success"} />
    case "FAILED":
      return <WarningAmberOutlinedIcon ref={ref} {...iconProps} color={"error"} />
    case "PENDING":
      return <AccessAlarmOutlinedIcon ref={ref} {...iconProps} color={"primary"} />
    case "SCHEDULED":
      return <CalendarMonthOutlinedIcon ref={ref}  {...iconProps} color={"primary"} />
    case "IN_PROGRESS":
      return <PendingOutlinedIcon ref={ref} {...iconProps} color={"primary"} />
    case "CANCELLED":
      return <WarningAmberOutlinedIcon ref={ref}  {...iconProps} color={"error"} /> // TODO
    default:
      return <HelpOutlineOutlinedIcon ref={ref} {...iconProps} /> // TODO
  }
})
