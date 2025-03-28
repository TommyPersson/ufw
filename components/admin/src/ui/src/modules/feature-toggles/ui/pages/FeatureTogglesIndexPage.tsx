import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline"
import HighlightOffIcon from "@mui/icons-material/HighlightOff"
import {
  Card,
  CardContent,
  Chip,
  ChipProps,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography
} from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { uniqBy } from "es-toolkit"
import { useMemo } from "react"
import Markdown from "react-markdown"
import {
  ApplicationModuleHeader,
  CommandButton,
  DateTimeText,
  Page,
  PageBreadcrumb,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import { ApplicationModule } from "../../../../common/models"
import { DisableFeatureToggleCommand, EnableFeatureToggleCommand } from "../../commands"
import { FeatureToggleItem } from "../../models"
import { FeatureToggleListQuery } from "../../queries"

import classes from "./FeatureTogglesIndexPage.module.css"

export const FeatureTogglesIndexPage = () => {

  const featureTogglesQuery = useQuery(FeatureToggleListQuery(1))
  const featureToggles = featureTogglesQuery.data?.items ?? []

  const isEmpty = !featureTogglesQuery.isLoading && featureToggles.length === 0

  const applicationModules = uniqBy(featureToggles.map(it => it.applicationModule), it => it.id)

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Feature Toggles" },
    { text: "All", current: true },
  ], [])

  return (
    <Page
      heading={"Feature Toggles"}
      breadcrumbs={breadcrumbs}
      isLoading={featureTogglesQuery.isFetching}
      onRefresh={featureTogglesQuery.refetch}
      autoRefresh={true}
      error={featureTogglesQuery.error}
    >
      {isEmpty && emptyCard}
      {applicationModules.map(module => {
        const featureTogglesInModule = featureToggles.filter(cache => cache.applicationModule.id === module.id)
        return <FeatureToggleTableCard
          key={module.id}
          module={module}
          featureToggles={featureTogglesInModule}
          isFetching={featureTogglesQuery.isFetching}
        />
      })}
    </Page>
  )
}

const FeatureToggleTableCard = (props: {
  featureToggles: FeatureToggleItem[]
  module: ApplicationModule
  isFetching: boolean
}) => {
  const { featureToggles, module, isFetching } = props

  return (
    <TableContainer component={Card}>
      <CardContent>
        <ApplicationModuleHeader applicationModule={module} />
      </CardContent>
      <Divider />
      <Table className={classes.FeatureTogglesTable}>
        <TableHead>
          <TableRow>
            <TableCell>State</TableCell>
            <TableCell>Feature Toggle</TableCell>
            <TableCell>Events</TableCell>
            <TableCell></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          <>
            {featureToggles.map(it =>
              <FeatureToggleRow key={it.id} featureToggle={it} isFetching={isFetching} />
            )}
          </>
        </TableBody>
      </Table>
    </TableContainer>
  )
}

const FeatureToggleRow = (props: {
  featureToggle: FeatureToggleItem,
  isFetching: boolean,
}) => {
  const { featureToggle, isFetching } = props

  const commandArgs = { featureToggleId: featureToggle.id }

  return (
    <TableRow hover>
      <TableCell>
        <FeatureToggleStateChip featureToggle={featureToggle} />
      </TableCell>
      <TableCell>
        <Typography variant={"subtitle2"}>{featureToggle.title}</Typography>
        <Typography variant={"body2"} component={"div"}>
          <Markdown>{featureToggle.description}</Markdown>
        </Typography>
        <Typography variant={"caption"}>ID: <code>{featureToggle.id}</code></Typography>
      </TableCell>
      <TableCell>
        <PropertyGroup>
          <PropertyText
            title={"Created At"}
            subtitle={<DateTimeText dateTime={featureToggle.createdAt} />}
          />
          <PropertyText
            title={"State Changed At"}
            subtitle={<DateTimeText dateTime={featureToggle.stateChangedAt} />}
          />
        </PropertyGroup>
      </TableCell>
      <TableCell>
        {!featureToggle.isEnabled &&
            <CommandButton
                command={EnableFeatureToggleCommand}
                args={commandArgs}
                variant={"contained"}
                size={"small"}
                disabled={isFetching}
            />
        }
        {featureToggle.isEnabled &&
            <CommandButton
                command={DisableFeatureToggleCommand}
                args={commandArgs}
                variant={"contained"}
                size={"small"}
                disabled={isFetching}
            />
        }
      </TableCell>
    </TableRow>
  )
}

const emptyCard = (
  <Card>
    <CardContent>
      <Typography variant={"body2"}>
        <center><em>No feature toggles found</em></center>
      </Typography>
    </CardContent>
  </Card>
)


const FeatureToggleStateChip = (props: { featureToggle: FeatureToggleItem }) => {
  const { featureToggle } = props

  const color: ChipProps["color"] = featureToggle.isEnabled ? "success" : "error"
  const label = featureToggle.isEnabled ? "Enabled" : "Disabled"
  const icon = featureToggle.isEnabled ? <CheckCircleOutlineIcon /> : <HighlightOffIcon />

  return (
    <Chip
      variant={"outlined"}
      icon={icon}
      color={color}
      label={label}
    />
  )
}