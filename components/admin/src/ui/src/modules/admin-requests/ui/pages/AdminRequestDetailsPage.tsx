import { Button, CardContent, Divider, TextField, Typography } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useForm, UseFormReturn, useWatch } from "react-hook-form"
import Markdown from "react-markdown"
import { useParams } from "react-router"
import {
  ApplicationModuleHeader, JsonBlock,
  Page,
  PageBreadcrumb,
  PageSectionCard,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import { AdminRequest, AdminRequestParameter, AdminRequestType } from "../../models"
import { AdminRequestsQuery } from "../../queries"
import { ClassNameText } from "../components"

export const AdminRequestDetailsPage = (props: { requestType: AdminRequestType }) => {
  const { requestType } = props

  const params = useParams<{ requestClassName: string }>()
  const requestClassName = params.requestClassName!

  const adminRequestsQuery = useQuery(AdminRequestsQuery(requestType))
  const adminRequests = adminRequestsQuery.data ?? []
  const adminRequest = adminRequests.find(it => it.fullClassName === requestClassName)

  const breadcrumbs: PageBreadcrumb[] = [
    { text: "Admin Queries", link: "/admin-queries" }, // switch based on type
    { text: <ClassNameText fqcn={requestClassName} variant={"with-short-package"} /> },
    { text: "Details", current: true }
  ]

  const pageContent = adminRequest ? (
    <PageContent request={adminRequest} />
  ) : "not found"

  return (
    <Page
      heading={formatPageHeading(requestType)}
      breadcrumbs={breadcrumbs}
      isLoading={adminRequestsQuery.isLoading}
      error={adminRequestsQuery.error}
    >
      {pageContent}
    </Page>
  )
}

const PageContent = (props: { request: AdminRequest }) => {
  const { request } = props

  const form = useForm()
  const formValues = useWatch(form)

  return (
    <>
      <RequestDetailsSection request={request} />
      <RequestParametersSection request={request} form={form} />
      <RequestPreviewSection formValues={formValues} />

      <Button variant={"contained"} style={{ alignSelf: "flex-start" }}>Execute</Button>

      <PageSectionCard heading={"Result"}>
        <CardContent>asd</CardContent>
      </PageSectionCard>

      <Button variant={"contained"} style={{ alignSelf: "flex-start" }}>Clear</Button>
    </>
  )
}

const RequestDetailsSection = (props: { request: AdminRequest }) => {
  return (
    <PageSectionCard heading={props.request.name}>
      <CardContent>
        <Typography variant={"body2"} component={"div"}>
          <Markdown>{props.request.description}</Markdown>
        </Typography>
      </CardContent>
      <Divider />
      <CardContent>
        <PropertyGroup>
          <PropertyText
            title={"Class Name"}
            subtitle={<ClassNameText fqcn={props.request.fullClassName} variant={"only-class"} />}
          />
          <PropertyText
            title={"Full Class Name"}
            subtitle={<ClassNameText fqcn={props.request.fullClassName} variant={"full"} />}
          />
        </PropertyGroup>
      </CardContent>
      <Divider />
      <CardContent>
        <ApplicationModuleHeader applicationModule={props.request.applicationModule} />
      </CardContent>
    </PageSectionCard>
  )
}

const RequestParametersSection = (props: {
  request: AdminRequest
  form: UseFormReturn
}) => {
  const { request, form } = props

  return (
    <PageSectionCard heading={"Parameters"}>
      <CardContent>
        {request.parameters.map(it => (
          <RequestParameterFormControl key={it.name} parameter={it} form={form} />
        ))}
      </CardContent>
    </PageSectionCard>
  )
}

const RequestParameterFormControl = (props: {
  parameter: AdminRequestParameter
  form: UseFormReturn
}) => {
  const { parameter, form } = props

  switch (parameter.type) {
    case "STRING":
      return (
        <TextField
          id={`field-${parameter.name}`}
          label={parameter.name}
          helperText={parameter.description}
          required={parameter.required}
          size={"small"}
          slotProps={{
            input: form.register(parameter.name, {
              required: parameter.required
            })
          }}
        />
      )
  }
}

const RequestPreviewSection = (props: { formValues: any }) => {
  return (
    <PageSectionCard heading={"Preview"}>
      <CardContent><JsonBlock json={JSON.stringify(props.formValues)} /></CardContent>
    </PageSectionCard>
  )
}

function formatPageHeading(requestType: AdminRequestType): string {
  switch (requestType) {
    case "COMMAND":
      return "Admin Command"
    case "QUERY":
      return "Admin Query"
  }
}
