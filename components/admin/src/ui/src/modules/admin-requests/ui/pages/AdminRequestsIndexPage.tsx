import { Card, CardContent, Divider, Table, TableBody, TableContainer, TableRow, Typography } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { groupBy, uniqBy } from "es-toolkit"
import Markdown from "react-markdown"
import { ApplicationModuleHeader, LinkTableCell, Page } from "../../../../common/components"
import { ApplicationModule } from "../../../../common/models"
import { AdminRequest, AdminRequestType } from "../../models"
import { AdminRequestsQuery } from "../../queries"
import { ClassNameText } from "../components"

import classes from "./AdminRequestsIndexPage.module.css"

export const AdminRequestsIndexPage = (props: {
  requestType: AdminRequestType
}) => {

  const { requestType } = props

  const requestsQuery = useQuery(AdminRequestsQuery(requestType))
  const requests = requestsQuery.data ?? []

  const applicationModules = uniqBy(requests.map(it => it.applicationModule), it => it.id)

  const requestsByModuleId = groupBy(requests, it => it.applicationModule.id)

  console.log(requests, applicationModules, requestsByModuleId, requestsQuery.error)

  return (
    <Page
      heading={formatPageHeading(requestType)}
    >
      {applicationModules.map(module => (
        <AdminRequestTableCard
          key={module.id}
          requests={requestsByModuleId[module.id]}
          requestType={requestType}
          module={module}
        />
      ))}
    </Page>
  )
}


const AdminRequestTableCard = (props: {
  requests: AdminRequest[]
  requestType: AdminRequestType
  module: ApplicationModule
}) => {
  const { requests, module } = props

  return (
    <TableContainer component={Card}>
      <CardContent>
        <ApplicationModuleHeader applicationModule={module} />
      </CardContent>
      <Divider />
      <Table className={classes.Table}>
        <TableBody>
          <>
            {requests.map(it =>
              <AdminRequestItemRow key={it.className} request={it} />
            )}
          </>
        </TableBody>
      </Table>
    </TableContainer>
  )
}

const AdminRequestItemRow = (props: { request: AdminRequest }) => {
  const { request } = props

  const link = formatRequestDetailsLink(request)

  return (
    <TableRow hover>
      <LinkTableCell to={link}>
        <Typography variant={"subtitle2"}>{request.name}</Typography>
        <Markdown>{request.description}</Markdown>
        <Typography variant={"caption"}>Class: <ClassNameText fqcn={request.fullClassName} variant={'with-short-package'} /></Typography>
      </LinkTableCell>
    </TableRow>
  )
}

function formatPageHeading(requestType: AdminRequestType): string {
  switch (requestType) {
    case "COMMAND":
      return "Admin Commands"
    case "QUERY":
      return "Admin Queries"

  }
}


function formatRequestDetailsLink(request: AdminRequest): string {
  switch (request.type) {
    case "COMMAND":
      return `/admin-commands/${request.fullClassName}/details`
    case "QUERY":
      return `/admin-queries/${request.fullClassName}/details`
  }
}
