import * as React from "react"

export const ClassNameText = React.memo((props: {
  fqcn: string,
  variant: "full" | "with-short-package" | "only-class"
}) => {
  const { fqcn, variant } = props
  const lastDotIndex = fqcn.lastIndexOf(".")
  const pkg = fqcn.substring(0, lastDotIndex - 1)
  const shortPkg = pkg.split(".").map(it => it[0]).join(".")
  const className = fqcn.substring(lastDotIndex + 1)

  if (variant === "full") {
    return <code>{fqcn}</code>
  } else if (variant === "with-short-package") {
    return <code>{shortPkg}.<strong>{className}</strong></code>
  } else if (variant === "only-class") {
    return <code>{className}</code>
  } else {
    return <code>{fqcn}</code>
  }
})