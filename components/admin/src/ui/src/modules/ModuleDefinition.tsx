import { RouteObject } from "react-router"

export interface ModuleDefinition {
  moduleId: string
  navItemTitle: string
  indexRoute: string
  routes: RouteObject[]
  icon?: any
}