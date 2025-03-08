import { Box, Card, CardActionArea, CardContent, Typography } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import * as React from "react"
import { Link, LinkProps, useParams } from "react-router"
import { Page } from "../../../../common/components"
import { JobQueueListQuery } from "../../queries/JobQueueListQuery"

import classes from "./JobQueueDetailsPage.module.css"

export const JobQueueDetailsPage = () => {
  const { queueId } = useParams<{ queueId: string }>()

  const queuesQuery = useQuery(JobQueueListQuery)
  const queueItem = (queuesQuery.data ?? []).find(it => it.queueId === queueId)

  return (
    <Page heading={`Job Queue: ${queueId}`}>
      <PageSectionHeader>Queue Statistics</PageSectionHeader>
      <Box className={classes.StatCardBox}>
        <StatCard
          title={"# Scheduled"}
          value={queueItem?.numScheduled}
          linkProps={{ to: "../jobs/SCHEDULED", relative: "path" }}
        />
        <StatCard
          title={"# Pending"}
          value={queueItem?.numPending}
          linkProps={{ to: "../jobs/PENDING", relative: "path" }} />
        <StatCard
          title={"# In Progress"}
          value={queueItem?.numInProgress}
          linkProps={{ to: "../jobs/IN_PROGRESS", relative: "path" }} />
        <StatCard
          title={"# Failed"}
          value={queueItem?.numFailed}
          linkProps={{ to: "../jobs/FAILED", relative: "path" }} />
      </Box>
      <PageSectionHeader>Actions</PageSectionHeader>
      TODO
    </Page>
  )
}

const PageSectionHeader = (props: { children: any }) => {
  return (
    <Typography variant={"h5"} component={"h3"}>{props.children}</Typography>
  )
}

const StatCard = (props: {
  title: string
  value: any | undefined
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
              {value ?? "-"}
            </Typography>
          </CardContent>
        </CardActionArea>
      </MaybeLink>
    </Card>
  )
}