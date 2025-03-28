import ExpandLessIcon from "@mui/icons-material/ExpandLess"
import ExpandMoreIcon from "@mui/icons-material/ExpandMore"
import { Alert, AlertTitle, Box, ButtonProps, Card, CardContent, Divider, IconButton, Skeleton } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import Markdown from "react-markdown"
import { useParams } from "react-router"
import {
  CodeBlock,
  CommandButton,
  DateTimeText,
  ErrorAlert,
  JsonBlock,
  Page,
  PageBreadcrumb,
  PageSectionCard,
  PageSectionHeader,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import { CancelJobCommand, DeleteJobCommand, RescheduleJobNowCommand } from "../../commands"
import { JobDetails, JobFailure } from "../../models"
import { JobDetailsQuery, JobFailuresQuery } from "../../queries"


export const JobDetailsPage = () => {
  const params = useParams<{ queueId: string, jobId: string }>()
  const queueId = params.queueId!!
  const jobId = params.jobId!!

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

  const isLoading = jobDetailsQuery.isLoading || jobFailuresQuery.isLoading
  const isFetching = jobDetailsQuery.isFetching || jobFailuresQuery.isFetching

  const onRefresh = () => {
    jobDetailsQuery.refetch().then()
    jobFailuresQuery.refetch().then()
  }

  const content = jobDetailsQuery.error ? (
    <ErrorAlert error={jobDetailsQuery.error} title={"Unable to load job details"} />
  ) : (
    <>
      <JobStateSection isLoading={isLoading} jobDetails={jobDetails} lastJobFailure={jobFailures[0]} />
      <JobActionsSection jobDetails={jobDetails} />
      <JobFailureWarning jobDetails={jobDetails} />
      <JobDetailsSection isLoading={isLoading} jobDetails={jobDetails} />
      <JobDataSections isLoading={isLoading} jobDetails={jobDetails} />
      <JobFailuresSection isLoading={isLoading} jobFailures={jobFailures ?? []} />
      <JobTimelineSection />
    </>
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

const JobStateSection = (props: {
  isLoading: boolean,
  jobDetails: JobDetails | null | undefined
  lastJobFailure: JobFailure | null | undefined
}) => {
  const { isLoading, jobDetails, lastJobFailure } = props

  if (isLoading || !jobDetails) {
    return null
  }

  switch (jobDetails.state) {
    case "SCHEDULED":
      return (
        <Alert severity={"info"}>
          <AlertTitle>{jobDetails.state}</AlertTitle>
          This job is scheduled to execute at <strong><DateTimeText dateTime={jobDetails.nextScheduledFor} /></strong>.
        </Alert>
      )
    case "PENDING":
      return (
        <Alert severity={"info"}>
          <AlertTitle>{jobDetails.state}</AlertTitle>
          This job is waiting to execute since <strong><DateTimeText dateTime={jobDetails.nextScheduledFor} /></strong>.
        </Alert>
      )
    case "IN_PROGRESS":
      return (
        <Alert severity={"info"}>
          <AlertTitle>{jobDetails.state}</AlertTitle>
          This job started at <strong><DateTimeText dateTime={jobDetails.stateChangedAt} /></strong>.
        </Alert>
      )
    case "CANCELLED":
      return (
        <Alert severity={"warning"}>
          <AlertTitle>{jobDetails.state}</AlertTitle>
          This job was cancelled at <strong><DateTimeText dateTime={jobDetails.stateChangedAt} /></strong>.
        </Alert>
      )
    case "SUCCESSFUL":
      return (
        <Alert severity={"success"}>
          <AlertTitle>{jobDetails.state}</AlertTitle>
          This job finished successfully at <strong><DateTimeText dateTime={jobDetails.stateChangedAt} /></strong>.
        </Alert>
      )
    case "FAILED":
      return (
        <Alert severity={"error"}>
          <AlertTitle>{jobDetails.state}</AlertTitle>
          <Box sx={{ mb: 1 }}>
            This job failed at <strong><DateTimeText dateTime={jobDetails.stateChangedAt} /></strong>.
          </Box>
          <Box>
            The most recent error was <code>{lastJobFailure?.errorType}</code>, see the failure list below for details.
          </Box>
        </Alert>
      )
    default:
      return null
  }
}

const JobFailureWarning = (props: {
  jobDetails: JobDetails | null | undefined
}) => {

  const { jobDetails } = props

  if (!jobDetails || jobDetails.numFailures === 0) {
    return null
  }

  return (
    <Alert severity={"warning"}>
      This job has failed <strong>{jobDetails.numFailures}</strong> times. See the failure list below for details.
    </Alert>
  )
}

const JobDetailsSection = (props: {
  isLoading: boolean,
  jobDetails: JobDetails | null | undefined
}) => {
  const { isLoading, jobDetails } = props

  return (
    <PageSectionCard heading={"Details"}>
      <CardContent>
        <PropertyGroup>
          <PropertyText
            title={"Job ID"}
            isLoading={isLoading}
            subtitle={<code>{jobDetails?.jobId}</code>}
          />
          <PropertyGroup horizontal>
            <PropertyGroup boxProps={{ flex: 1 }}>
              <PropertyText
                title={"Job State"}
                isLoading={isLoading}
                subtitle={<code>{jobDetails?.state}</code>}
              />
              <PropertyText
                title={"# Failures"}
                isLoading={isLoading}
                subtitle={jobDetails?.numFailures}
              />
            </PropertyGroup>
            <PropertyGroup boxProps={{ flex: 1 }}>
              <PropertyText
                title={"Created At"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={jobDetails?.createdAt ?? null} />}
              />
              <PropertyText
                title={"First Scheduled For"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={jobDetails?.firstScheduledFor ?? null} />}
              />
              <PropertyText
                title={"Next Scheduled For"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={jobDetails?.nextScheduledFor ?? null} fallback={<em>N/A</em>} />}
              />
              <PropertyText
                title={"State Changed At"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={jobDetails?.stateChangedAt ?? null} />}
              />
              <PropertyText
                title={"Expires At"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={jobDetails?.expiresAt ?? null} fallback={<em>N/A</em>} />}
              />
            </PropertyGroup>
            <PropertyGroup boxProps={{ flex: 1 }}>
              <PropertyText
                title={"Concurrency Key"}
                isLoading={isLoading}
                subtitle={jobDetails?.concurrencyKey ? <code>{jobDetails.concurrencyKey}</code> : <em>N/A</em>}
              />
              <PropertyText
                title={"Watchdog Owner"}
                isLoading={isLoading}
                subtitle={jobDetails?.watchdogOwner ? <code>{jobDetails.watchdogOwner}</code> : <em>N/A</em>}
              />
              <PropertyText
                title={"Watchdog Timestamp"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={jobDetails?.watchdogTimestamp ?? null} fallback={<em>N/A</em>} />}
              />
            </PropertyGroup>
          </PropertyGroup>
        </PropertyGroup>
      </CardContent>
      <Divider />
      <CardContent>
        <PropertyGroup>
          <PropertyGroup horizontal>
            <PropertyText
              title={"Job Type"}
              isLoading={isLoading}
              subtitle={<code>{jobDetails?.jobType}</code>}
            />
            <PropertyText
              title={"Job Class Name"}
              isLoading={isLoading}
              subtitle={<code>{jobDetails?.jobTypeClass}</code>}
            />
          </PropertyGroup>
          <PropertyText
            title={"Job Type Description"}
            isLoading={isLoading}
            noSubtitleStyling={!!jobDetails?.jobTypeDescription}
            subtitle={jobDetails?.jobTypeDescription ? <Markdown>{jobDetails?.jobTypeDescription}</Markdown> : <em>N/A</em>}
          />
        </PropertyGroup>
      </CardContent>
    </PageSectionCard>
  )
}

const JobActionsSection = (props: {
  jobDetails: JobDetails | null | undefined
}) => {
  const { jobDetails } = props

  if (!jobDetails) {
    return null
  }

  const baseActionButtonProps: ButtonProps = {
    variant: "contained"
  }

  const { queueId, jobId, state } = jobDetails

  const canRescheduleJob = state === "FAILED"
  const canDeleteJob = state === "FAILED"
  const canCancelJob = state === "SCHEDULED" || state === "PENDING" || state === "IN_PROGRESS"

  if (!canRescheduleJob && !canDeleteJob && !canCancelJob) {
    return null
  }

  return (
    <Box display={"flex"} flexDirection={"row"} gap={1}>
      {canRescheduleJob && (
        <CommandButton
          {...baseActionButtonProps}
          command={RescheduleJobNowCommand}
          args={{ queueId, jobId }}
        />
      )}
      {canDeleteJob && (
        <CommandButton
          {...baseActionButtonProps}
          command={DeleteJobCommand}
          args={{ queueId, jobId }}
        />
      )}
      {canCancelJob && (
        <CommandButton
          {...baseActionButtonProps}
          command={CancelJobCommand}
          args={{ queueId, jobId }}
        />
      )}
    </Box>
  )
}

const JobDataSections = (props: {
  isLoading: boolean,
  jobDetails: JobDetails | null | undefined
}) => {
  const { isLoading, jobDetails } = props

  return (
    <>
      <PageSectionHeader>Job Data</PageSectionHeader>
      <PageSectionCard heading={"Data JSON"}>
        <CardContent>
          <JsonBlock isLoading={isLoading} json={jobDetails?.dataJson ?? null} />
        </CardContent>
      </PageSectionCard>
      <PageSectionCard heading={"Metadata JSON"}>
        <CardContent>
          <JsonBlock isLoading={isLoading} json={jobDetails?.metadataJson ?? null} />
        </CardContent>
      </PageSectionCard>
    </>
  )
}

const JobFailuresSection = (props: {
  isLoading: boolean,
  jobFailures: JobFailure[]
}) => {
  const { isLoading, jobFailures } = props

  return (
    <>
      <PageSectionHeader>Last 5 Failures</PageSectionHeader>
      {isLoading && <Skeleton />}
      {!isLoading && jobFailures.length === 0 && <NoFailuresMessage />}
      {jobFailures.map((it, i) => (
        <JobFailureCard key={it.failureId} failure={it} isFirst={i === 0} />
      ))}
    </>
  )
}

const JobFailureCard = (props: { failure: JobFailure, isFirst: boolean }) => {
  const { failure, isFirst } = props

  const [isExpanded, setIsExpanded] = useState(isFirst)

  const toggleExpansionButton = (
    <IconButton
      onClick={() => setIsExpanded(s => !s)}
      children={isExpanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
    />
  )

  return (
    <Card key={failure.failureId}>
      <Alert severity={"error"} action={toggleExpansionButton}>
        <PropertyGroup>
          <PropertyGroup horizontal>
            <PropertyText
              title={"Error Type"}
              subtitle={<code>{failure.errorType}</code>}
            />
            <PropertyText
              title={"Timestamp"}
              subtitle={<DateTimeText dateTime={failure.timestamp} />}
            />
          </PropertyGroup>
          {isExpanded && (
            <PropertyGroup>
              <PropertyText
                title={"Error Message"}
                subtitle={<code>{failure.errorMessage}</code>}
              />
              <PropertyText
                title={"Error Stacktrace"}
                subtitle={<CodeBlock code={failure.errorStackTrace} style={{ height: 160 }} />}
              />
            </PropertyGroup>
          )}
        </PropertyGroup>
      </Alert>
    </Card>
  )
}

const NoFailuresMessage = () => {
  return (<Alert severity={"success"}>No failures has been recorded</Alert>)
}

const JobTimelineSection = () => {
  return (
    <>
      <PageSectionHeader>Timeline</PageSectionHeader>
      TODO TIMELINE
    </>
  )
}