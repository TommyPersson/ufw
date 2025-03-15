import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline"
import PlaylistPlayIcon from "@mui/icons-material/PlaylistPlay"
import { Box, Button, Card, CardActionArea, CardContent, Skeleton, Typography } from "@mui/material"
import { useMutation, useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import * as React from "react"
import Markdown from "react-markdown"
import { Link, LinkProps, useParams } from "react-router"
import { CommandButton, Page, PageBreadcrumb, PropertyText } from "../../../../common/components"
import { RescheduleAllFailedJobsCommand } from "../../commands/RescheduleAllJobsCommand"
import { JobQueueDetails } from "../../models/JobQueueDetails"
import { JobType } from "../../models/JobType"
import { JobQueueDetailsQuery } from "../../queries/JobQueueDetailsQuery"

import classes from "./JobQueueDetailsPage.module.css"

export const JobQueueDetailsPage = () => {
  const { queueId } = useParams<{ queueId: string }>()

  const queuesQuery = useQuery(JobQueueDetailsQuery(queueId!))
  const queueDetails = queuesQuery.data ?? null

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Job Queue" },
    { text: "Queues", link: "../" },
    { text: <code>{queueId}</code> },
    { text: "Details", current: true }
  ], [queueId])

  return (
    <Page
      heading={<>Job Queue: <code>{queueId}</code></>}
      isLoading={queuesQuery.isFetching}
      onRefresh={queuesQuery.refetch}
      breadcrumbs={breadcrumbs}
    >
      <QueueStatisticsSection details={queueDetails} />
      <QueueActionsSection queueId={queueId!} />
      <JobTypesSection jobTypes={queueDetails?.jobTypes ?? []} />
    </Page>
  )
}

const PageSectionHeader = (props: { children: any }) => {
  return (
    <Typography variant={"h5"} component={"h3"}>{props.children}</Typography>
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

  const rescheduleAllFailedJobsCommand = useMutation(RescheduleAllFailedJobsCommand)

  return (
    <>
      <PageSectionHeader>Actions</PageSectionHeader>
      <Box sx={{ display: "flex", gap: 2, flexDirection: "column" }}>
        <CommandButton
          variant={"contained"}
          sx={{ alignSelf: "flex-start" }}
          startIcon={<PlaylistPlayIcon />}
          command={rescheduleAllFailedJobsCommand}
          args={{ queueId }}
          errorTitle={"Unable to rescheduled jobs"}
          confirmText={"Are you sure you want to reschedule all jobs?"}
          children={"Reschedule all failed jobs"}
        />
        <Button
          color={"error"}
          variant={"contained"}
          sx={{ alignSelf: "flex-start" }}
          startIcon={<DeleteOutlineIcon />}
          children={"Delete all failed jobs"}
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
    <Card className={classes.JobTypeDetailsCard}>
      <CardContent>
        <Box sx={{ display: "flex", gap: 2, flexDirection: "column" }}>
          <Box display={"flex"} flexDirection={"row"} sx={{ gap: 2 }}>
            <PropertyText
              title={"Job Type"}
              subtitle={<code>{props.jobType.type}</code>}
            />
            <PropertyText
              title={"Job Class Name"}
              subtitle={<code>{props.jobType.jobClassName}</code>}
            />
          </Box>
          <PropertyText
            title={"Description"}
            subtitle={<Markdown>{props.jobType.description ?? "*No description*"}</Markdown>}
            noSubtitleStyling
          />
        </Box>
      </CardContent>
    </Card>
  )
}