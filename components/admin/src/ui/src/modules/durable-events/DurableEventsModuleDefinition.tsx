import EventNoteOutlinedIcon from '@mui/icons-material/EventNoteOutlined';
import { EventDetailsPage } from "./ui/pages/EventDetailsPage"
import { EventListPage } from "./ui/pages/EventListPage"
import { EventQueueIndexPage } from "./ui/pages/EventQueueIndexPage.tsx"
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { EventQueueDetailsPage } from "./ui/pages/EventQueueDetailsPage"

export const DurableEventsModuleDefinition: ModuleDefinition = {
  moduleId: "durable-events",
  navItemTitle: "Durable Events",
  indexRoute: "durable-events",
  icon: <EventNoteOutlinedIcon />,
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
