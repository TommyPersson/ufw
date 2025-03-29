import { RouteObject } from "react-router"

export interface ModuleDefinition {
  moduleId: string
  navItems: {
    title: string,
    route: string,
    icon?: any
  }[]
  routes: RouteObject[]
}