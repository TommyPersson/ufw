import { createBrowserRouter } from "react-router"

export type Router = ReturnType<typeof createBrowserRouter>

export let routerHolder = {
  router: undefined as Router | undefined
}