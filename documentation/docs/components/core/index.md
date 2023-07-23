---
title: Introduction
---

# Core: Introduction

The purpose of the Core component is to provide utilities to the other UFW components.

Among other things:

* The `InstantSource` to use as the time provider for all components.
* The MicroMeter `MeterRegistry` used to record metrics in all components.
* Options for the Jackson `ObjectMapper` used for serializing objects in all components.

Apart from initial configuration, you rarely interact with the Core component directly.

See [Core: Installation](./installation.md) for configuration details.