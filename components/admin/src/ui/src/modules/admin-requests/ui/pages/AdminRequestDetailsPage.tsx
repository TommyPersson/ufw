import ClearOutlinedIcon from "@mui/icons-material/ClearOutlined"
import PlayArrowOutlinedIcon from "@mui/icons-material/PlayArrowOutlined"
import {
  Alert,
  Button,
  CardContent,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableRow,
  TextFieldProps,
  Typography
} from "@mui/material"
import { useMutation, useQuery } from "@tanstack/react-query"
import { delay } from "es-toolkit"
import { useCallback, useMemo } from "react"
import { FieldValues, useForm, UseFormReturn, useWatch } from "react-hook-form"
import Markdown from "react-markdown"
import { useParams } from "react-router"
import {
  ApplicationModuleHeader,
  ErrorAlert,
  FormTextField,
  JsonBlock,
  Page,
  PageBreadcrumb,
  PageSectionCard,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import { ExecuteRequestMutation } from "../../commands"
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

  const pageContent = adminRequest && (
    <PageContent request={adminRequest} />
  )

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

  const form = useForm({
    mode: "all",
    defaultValues: getDefaultValues(request),
  })

  const formValues = useWatch(form)

  const requestPreviewBody = useMemo(() => {
    return formatRequestObject(formValues, request)
  }, [formValues, request])

  const executeMutation = useMutation(ExecuteRequestMutation)

  const handleSubmit = useCallback(async (formFields: any) => {
    const body = formatRequestObject(formFields, request)
    await executeMutation.mutateAsync({
      fqcn: request.fullClassName,
      requestType: request.type,
      body: body
    })
    await delay(10)
    document.getElementById("response-bottom")?.scrollIntoView({ behavior: "smooth" })
  }, [executeMutation.mutateAsync, request])

  const handleExecuteClicked = useCallback(async () => {
    executeMutation.reset()
    await form.handleSubmit(handleSubmit)()
  }, [executeMutation.mutateAsync, handleSubmit])

  const handleClearClicked = executeMutation.reset

  const errors = Object.entries(form.formState.errors)

  return (
    <>
      <RequestDetailsSection request={request} />
      <RequestParametersSection request={request} form={form} />
      <RequestPreviewSection requestObject={requestPreviewBody} />

      {errors.length > 0 && (
        <Alert severity={"error"}>
          One or more fields have validation errors.
        </Alert>
      )}

      <Button
        variant={"contained"}
        style={{ alignSelf: "flex-start" }}
        onClick={handleExecuteClicked}
        loading={executeMutation.isPending}
        disabled={errors.length > 0}
        startIcon={<PlayArrowOutlinedIcon />}
        children={"Execute"}
      />


      <ResponseSection
        response={executeMutation.data}
        error={executeMutation.error}
        onClear={handleClearClicked}
      />
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
      <Table size={"small"}>
        <TableBody>
          {request.parameters.map(it => (
            <TableRow key={it.field}>
              <TableCell style={{ width: 0, whiteSpace: "nowrap", textAlign: "right" }}>
                <Typography variant={"body2"}>{it.displayName} {it.required ? "*" : ""}</Typography>
              </TableCell>
              <TableCell style={{ width: 0 }}>
                <RequestParameterFormControl key={it.field} parameter={it} form={form} />
              </TableCell>
              <TableCell>
                <Typography variant={"body2"}>{it.helperText}</Typography>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </PageSectionCard>
  )
}

const RequestParameterFormControl = (props: {
  parameter: AdminRequestParameter
  form: UseFormReturn
}) => {
  const { parameter, form } = props

  const baseProps: Partial<TextFieldProps> = useMemo(() => ({
    id: `field-${parameter.field}`,
    required: parameter.required,
    size: "small",
    slotProps: {
      inputLabel: {
        shrink: true,
      }
    },
    ...getTextFieldProps(parameter)
  }), [parameter])

  return (
    <FormTextField
      {...baseProps}
      field={parameter.field}
      control={form.control}
    />
  )
}

const RequestPreviewSection = (props: { requestObject: any }) => {
  return (
    <PageSectionCard heading={"Request Body Preview"}>
      <CardContent>
        <JsonBlock json={JSON.stringify(props.requestObject)} />
      </CardContent>
    </PageSectionCard>
  )
}

const ResponseSection = (props: { response: any | null, error: any | null, onClear: () => void }) => {
  const { response, error, onClear } = props

  return (
    <>
      <ErrorAlert error={error} />

      {response && (
        <>
          <PageSectionCard heading={"Response"}>
            <CardContent>
              <JsonBlock json={JSON.stringify(response.body, null, 2)} />
            </CardContent>
          </PageSectionCard>

          <Button
            id={"clear-button"}
            variant={"contained"}
            style={{ alignSelf: "flex-start" }}
            onClick={onClear}
            startIcon={<ClearOutlinedIcon />}
            children={"Clear"}
          />
        </>
      )}
      <div id={"response-bottom"} />
    </>
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

function formatRequestObject(formValues: FieldValues, request: AdminRequest): any {
  return request.parameters.reduce((acc, curr) => ({
    ...acc,
    [curr.field]: formatParameterValue(formValues[curr.field], curr)
  }), {})
}

function formatParameterValue(value: string | undefined, parameter: AdminRequestParameter): any {
  if (value?.length === 0 && !parameter.required) {
    return null
  }

  if (!value) {
    return null
  }

  switch (parameter.type) {
    case "INTEGER":
      return parseInt(value)
    case "DECIMAL":
      return parseFloat(value)
    default:
      return value
  }
}

function getDefaultValues(request: AdminRequest): any {
  return request.parameters.reduce((acc, curr) => ({
    ...acc,
    [curr.field]: curr.defaultValue ?? undefined,
  }), {})
}

function getTextFieldProps(parameter: AdminRequestParameter): Partial<TextFieldProps> {
  switch (parameter.type) {
    case "STRING":
      return { style: { minWidth: "400px" } }
    case "INTEGER":
      return { type: "number" }
    case "DECIMAL":
      return { type: "number" }
    case "LOCAL_DATE":
      return { type: "date" }
    case "LOCAL_TIME":
      return { type: "time" }
    case "LOCAL_DATE_TIME":
      return { type: "datetime-local" }
    default:
      return {}
  }
}