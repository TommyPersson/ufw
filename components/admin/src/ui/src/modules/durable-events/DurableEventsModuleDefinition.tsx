import EventNoteOutlinedIcon from "@mui/icons-material/EventNoteOutlined"
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { EventDetailsPage } from "./ui/pages/EventDetailsPage"
import { EventListPage } from "./ui/pages/EventListPage"
import { EventQueueDetailsPage } from "./ui/pages/EventQueueDetailsPage"
import { EventQueueIndexPage } from "./ui/pages/EventQueueIndexPage.tsx"

export const DurableEventsModuleDefinition: ModuleDefinition = {
  moduleId: "durable-events",
  navItems: [{
    title: "Durable Events",
    route: "durable-events",
    icon: <EventNoteOutlinedIcon />,
  }],
  routes: [
    {
      path: "durable-events",
      children: [
        {
          index: true,
          Component: EventQueueIndexPage,
        },
        {
          path: "queues/:queueId/details",
          Component: EventQueueDetailsPage,
        },
        {
          path: "queues/:queueId/events/:eventState",
          Component: EventListPage,
        },
        {
          path: "queues/:queueId/events/by-id/:eventId/details",
          Component: EventDetailsPage
        }
      ]
    }
  ]
}
