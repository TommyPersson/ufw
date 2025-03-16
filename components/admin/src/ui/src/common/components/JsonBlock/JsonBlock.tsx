import { Skeleton } from "@mui/material"
import { useMemo } from "react"


export type JsonBlockProps = {
  json: string | null
  isLoading?: boolean
}

export const JsonBlock = (props: JsonBlockProps) => {
  const { json, isLoading } = props

  const formatted = useMemo(() => JSON.stringify(JSON.parse(json ?? "{}"), null, 2), [json])

  if (isLoading) {
    return (
      <>
        <Skeleton variant={"text"} />
        <Skeleton variant={"text"} />
        <Skeleton variant={"text"} />
      </>
    )
  }

  return (
    <pre>{formatted}</pre>
  )
}