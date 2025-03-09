import { Box, Button, Card, CardActionArea, CardContent, Skeleton, Typography } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import * as React from "react"
import Markdown from "react-markdown"
import { Link, LinkProps, useParams } from "react-router"
import { Page, PropertyText } from "../../../../common/components"
import { JobQueueDetails, JobType } from "../../models/JobQueueDetails"
import { JobQueueDetailsQuery } from "../../queries/JobQueueDetailsQuery"
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline"
import PlaylistPlayIcon from "@mui/icons-material/PlaylistPlay"

import classes from "./JobQueueDetailsPage.module.css"

export const JobQueueDetailsPage = () => {
  const { queueId } = useParams<{ queueId: string }>()

  const queuesQuery = useQuery(JobQueueDetailsQuery(queueId!))
  const queueDetails = queuesQuery.data ?? null

  return (
    <Page
      heading={<>Job Queue: <code>{queueId}</code></>}
      isLoading={queuesQuery.isFetching}
      onRefresh={queuesQuery.refetch}
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
  const {  } = props
  return (
    <>
      <PageSectionHeader>Actions</PageSectionHeader>
      <Box sx={{ display: "flex", gap: 2, flexDirection: "column" }}>
        <Button
          variant={"contained"}
          children={"Rescheduled all failed jobs"}
          startIcon={<PlaylistPlayIcon />}
          sx={{ alignSelf: "flex-start" }}
        />
        <Button
          color={"error"}
          variant={"contained"}
          children={"Delete all failed jobs"}
          startIcon={<DeleteOutlineIcon />}
          sx={{ alignSelf: "flex-start" }}
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