---
title: Index
---

# Components

[ufw-core](./core)
: Provides the basic utilities used by all the other components.

[ufw-database](./database)
: Provides interfaces to the database used by the rest of the components.

[ufw-managed](./managed)
: Manages the life-cycle of `Managed` objects.

[ufw-job-queue](./job-queue)
: The Job Queue component is simple database-backed job queue.

# Dependency Relationships

```mermaid
graph LR
    ufw-job-queue --> ufw-database
    ufw-job-queue --> ufw-managed
    ufw-job-queue --> ufw-core
    ufw-database --> ufw-core
    ufw-managed --> ufw-core
    ufw-key-value-store --> ufw-core
    ufw-key-value-store --> ufw-database
    ufw-key-value-store --> ufw-managed

    click ufw-job-queue href "./job-queue"
    click ufw-key-value-store href "./key-value-store"
    click ufw-database href "./database"
    click ufw-managed href "./managed"
    click ufw-core href "./core"
```