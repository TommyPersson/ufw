---
title: Introduction
---

# Job Queue: Introduction

The Job Queue component is simple database-backed job queue. It allows you to schedule asynchronous tasks easily with
transactional safety using the `UnitOfWork` interface from the [Database component](../database) 

1. See [Installation](./installation.md) for setup instructions
2. See [Usage](./usage.md) for usage instructions

## Dependencies

```mermaid
graph LR
    ufw-job-queue --> ufw-database
    ufw-job-queue --> ufw-managed
    ufw-job-queue --> ufw-core
    ufw-database --> ufw-core
    ufw-managed --> ufw-core

    click ufw-database href "../database"
    click ufw-managed href "../managed"
    click ufw-core href "../core"
```