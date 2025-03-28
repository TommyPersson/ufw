import {
  Alert,
  AlertTitle,
  Box,
  ButtonProps,
  Card,
  CardActionArea,
  CardContent,
  Skeleton,
  Typography
} from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import * as React from "react"
import { useMemo } from "react"
import Markdown from "react-markdown"
import { Link, LinkProps, useParams } from "react-router"
import {
  CommandButton,
  DateTimeText,
  Page,
  PageBreadcrumb,
  PageSectionCard,
  PageSectionHeader,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import {
  DeleteAllFailedJobsCommand,
  PauseJobQueueCommand,
  RescheduleAllFailedJobsCommand,
  UnpauseJobQueueCommand
} from "../../commands"
import { JobQueueDetails, JobType } from "../../models"
import { JobQueueDetailsQuery } from "../../queries"
import { getQueueStateColor } from "../utils/colors"

import classes from "./JobQueueDetailsPage.module.css"

export const JobQueueDetailsPage = () => {
  const { queueId } = useParams<{ queueId: string }>()

  const queuesQuery = useQuery(JobQueueDetailsQuery(queueId!))
  const queueDetails = queuesQuery.data ?? null

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
      <JobQueueStatusSection details={queueDetails} />
      <QueueStatisticsSection details={queueDetails} />
      <QueueActionsSection queueId={queueId!} />
      <JobTypesSection jobTypes={queueDetails?.jobTypes ?? []} />
    </Page>
  )
}

const JobQueueStatusSection = (props: { details: JobQueueDetails | null }) => {
  const { details } = props
  if (!details) {
    return null
  }

  const state = details.status.state

  const color = getQueueStateColor(state)

  const actionButtonProps: Partial<ButtonProps> = {
    color: "inherit"
  }

  const action = state === "ACTIVE"
    ? <CommandButton command={PauseJobQueueCommand} args={{ queueId: details.queueId }} {...actionButtonProps} />
    : <CommandButton command={UnpauseJobQueueCommand} args={{ queueId: details.queueId }} {...actionButtonProps} />

  return (
    <Alert severity={color} action={action}>
      <AlertTitle>{state}</AlertTitle>
      {state === "ACTIVE" &&
          <>The queue is active and will process jobs until paused.</>
      }
      {state === "PAUSED" &&
          <>
              The queue has been paused since{" "}
              <strong><DateTimeText dateTime={details.status.stateChangedAt} /></strong>{" "}
              and will not process jobs until unpaused.
          </>
      }
    </Alert>
  )
}

const QueueStatisticsSection = (props: { details: JobQueueDetails | null }) => {
  const { details } = props

  return (
    <>
      <PageSectionHeader>Queue Statistics</PageSectionHeader>
      <Box className={classes.StatCardBox}>
        <StatCard
          title={"# Scheduled"}
          value={details?.numScheduled ?? <Skeleton variant="text" />}
          linkProps={{ to: "../jobs/SCHEDULED", relative: "path" }}
        />
        <StatCard
          title={"# Pending"}
          value={details?.numPending ?? <Skeleton variant="text" />}
          linkProps={{ to: "../jobs/PENDING", relative: "path" }} />
        <StatCard
          title={"# In Progress"}
          value={details?.numInProgress ?? <Skeleton variant="text" />}
          linkProps={{ to: "../jobs/IN_PROGRESS", relative: "path" }} />
        <StatCard
          title={"# Failed"}
          value={details?.numFailed ?? <Skeleton variant="text" />}
          linkProps={{ to: "../jobs/FAILED", relative: "path" }} />
      </Box>
    </>
  )
}

const StatCard = (props: {
  title: string
  value: any
  linkProps?: LinkProps
}) => {

  const { title, value, linkProps } = props

  const MaybeLink = linkProps ? Link : React.Fragment

  return (
    <Card className={classes.StatCard}>
      <MaybeLink {...linkProps as any}>
        <CardActionArea>
          <CardContent>
            <Typography variant={"subtitle1"} component={"div"}>
              {title}
            </Typography>
            <Typography variant={"h4"} component={"div"}>
              {value}
            </Typography>
          </CardContent>
        </CardActionArea>
      </MaybeLink>
    </Card>
  )
}

const QueueActionsSection = (props: { queueId: string }) => {
  const { queueId } = props
  return (
    <>
      <PageSectionHeader>Actions</PageSectionHeader>
      <Box sx={{ display: "flex", gap: 2, flexDirection: "column" }}>
        <CommandButton
          variant={"contained"}
          sx={{ alignSelf: "flex-start" }}
          command={RescheduleAllFailedJobsCommand}
          args={{ queueId }}
        />
        <CommandButton
          variant={"contained"}
          sx={{ alignSelf: "flex-start" }}
          command={DeleteAllFailedJobsCommand}
          args={{ queueId }}
        />
      </Box>
    </>
  )
}


const JobTypesSection = (props: { jobTypes: JobType[] }) => {
  return (
    <>
      <PageSectionHeader>Job Types</PageSectionHeader>
      {props.jobTypes.map(it => <JobTypeDetailsCard key={it.type} jobType={it} />)}
    </>
  )
}

const JobTypeDetailsCard = (props: { jobType: JobType }) => {
  return (
    <PageSectionCard>
      <CardContent>
        <PropertyGroup>
          <PropertyGroup horizontal>
            <PropertyText
              title={"Job Type"}
              subtitle={<code>{props.jobType.type}</code>}
            />
            <PropertyText
              title={"Job Class Name"}
              subtitle={<code>{props.jobType.jobClassName}</code>}
            />
          </PropertyGroup>
          <PropertyText
            title={"Description"}
            subtitle={<Markdown>{props.jobType.description ?? "*No description*"}</Markdown>}
            noSubtitleStyling
          />
        </PropertyGroup>
      </CardContent>
    </PageSectionCard>
  )
}