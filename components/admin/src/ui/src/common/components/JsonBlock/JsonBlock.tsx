import { Skeleton } from "@mui/material"
import * as React from "react"
import { useMemo } from "react"


export type JsonBlockProps = {
  json: string | null
  maxHeight?: number | string
  scroll?: boolean
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

  const style: React.CSSProperties = {
    maxHeight: props.maxHeight,
    overflow: props.maxHeight ? "scroll" : undefined
  }

  return (
    <pre style={style}>{formatted}</pre>
  )
}