import { RouteObject } from "react-router"
import { CoreIndexPage } from "./modules/core/ui/pages/CoreIndexPage"
import Root from "./modules/core/ui/Root.tsx"
import { allModuleDefinitions } from "./modules/modules.ts"

export const routes: RouteObject[] = [
  {
    path: "/",
    Component: Root,
    children: [
      ...allModuleDefinitions.flatMap(it => it.routes),
      {
        index: true,
        Component: CoreIndexPage,
      }
    ]
  }
]