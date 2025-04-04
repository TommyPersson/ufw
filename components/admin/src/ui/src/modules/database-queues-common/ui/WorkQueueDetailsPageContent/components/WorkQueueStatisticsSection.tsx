import { Box, Card, CardActionArea, CardContent, Skeleton, Typography } from "@mui/material"
import * as React from "react"
import { Link, LinkProps } from "react-router"
import { PageSectionHeader } from "../../../../../common/components"
import { WorkQueueDetails } from "../../../models"
import { DatabaseQueueAdapterSettings } from "../../../DatabaseQueueAdapterSettings"

import classes from "./WorkQueueStatisticsSection.module.css"

export const WorkQueueStatisticsSection = (props: {
  details: WorkQueueDetails | null
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { details, adapterSettings } = props

  return (
    <>
      <PageSectionHeader>Queue Statistics</PageSectionHeader>
      <Box className={classes.StatCardBox}>
        <StatCard
          title={"# Scheduled"}
          value={details?.numScheduled ?? <Skeleton variant="text" />}
          linkProps={{ to: adapterSettings.linkFormatters.workItemList(details?.queueId!, "SCHEDULED") }}
        />
        <StatCard
          title={"# Pending"}
          value={details?.numPending ?? <Skeleton variant="text" />}
          linkProps={{ to: adapterSettings.linkFormatters.workItemList(details?.queueId!, "PENDING") }}
        />
        <StatCard
          title={"# In Progress"}
          value={details?.numInProgress ?? <Skeleton variant="text" />}
          linkProps={{ to: adapterSettings.linkFormatters.workItemList(details?.queueId!, "IN_PROGRESS") }}
        />
        <StatCard
          title={"# Failed"}
          value={details?.numFailed ?? <Skeleton variant="text" />}
          linkProps={{ to: adapterSettings.linkFormatters.workItemList(details?.queueId!, "FAILED") }}
        />
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
            <Typography variant={"overline"} component={"div"}>
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