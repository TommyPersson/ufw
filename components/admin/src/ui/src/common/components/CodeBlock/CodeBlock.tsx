import { Skeleton } from "@mui/material"
import * as React from "react"

import classes from "./CodeBlock.module.css"

export type CodeBlockProps = {
  code: string
  isLoading?: boolean
  style?: React.CSSProperties
}

export const CodeBlock = (props: CodeBlockProps) => {
  const { code, isLoading, style } = props

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
    <pre className={classes.CodeBlock} style={style}>{code}</pre>
  )
}