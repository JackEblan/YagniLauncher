# Yagni Launcher Architecture

This document describes the modular architecture of Yagni Launcher, explaining how each Gradle module is structured and how they relate to Clean Architecture principles.

---

## Table of Contents

- [Overview](#overview)
- [Clean Architecture Layers](#clean-architecture-layers)
- [Module Dependency Graph](#module-dependency-graph)
- [Module Reference](#module-reference)
  - [app](#app)
  - [build-logic](#build-logic)
  - [common](#common)
  - [Domain Layer](#domain-layer)
    - [domain:model](#domainmodel)
    - [domain:repository](#domainrepository)
    - [domain:framework](#domainframework)
    - [domain:use-case](#domainuse-case)
    - [domain:grid](#domaingrid)
    - [domain:common](#domaincommon)
  - [Data Layer](#data-layer)
    - [data:repository](#datarepository)
    - [data:room](#dataroom)
    - [data:datastore](#datadatastore)
    - [data:datastore-proto](#datadatastore-proto)
  - [Presentation Layer](#presentation-layer)
    - [feature:home](#featurehome)
    - [feature:action](#featureaction)
    - [feature:pin](#featurepin)
    - [feature:edit-application-info](#featureedit-application-info)
    - [feature:edit-grid-item](#featureedit-grid-item)
    - [feature:settings:*](#featuresettings)
    - [design-system](#design-system)
    - [ui](#ui)
    - [service](#service)
  - [Framework Layer](#framework-layer)

---

## Overview

Yagni Launcher is organized into 30+ Gradle modules that follow **Clean Architecture**. The architecture separates the codebase into three primary layers:

1. **Domain** — Pure Kotlin business logic and entity definitions, independent of Android.
2. **Data** — Implementations of repositories, local database (Room), and preferences (Proto DataStore).
3. **Presentation** — Jetpack Compose UI features, ViewModels, services, and the design system.

A **Framework** layer wraps Android system APIs (e.g., `PackageManager`, `LauncherApps`, `WallpaperManager`) behind interfaces defined in the domain, keeping the domain layer free of Android dependencies.

---

## Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                    │
│  feature:*, design-system, ui, service                  │
├─────────────────────────────────────────────────────────┤
│                    Framework Layer                      │
│  framework:* (Android API wrappers)                     │
├─────────────────────────────────────────────────────────┤
│                      Data Layer                         │
│  data:repository, data:room, data:datastore             │
├─────────────────────────────────────────────────────────┤
│                     Domain Layer                        │
│  domain:model, domain:repository, domain:framework,     │
│  domain:use-case, domain:grid, domain:common            │
└─────────────────────────────────────────────────────────┘
```

Dependencies flow **inward only**: outer layers depend on inner layers, never the reverse.

---

## Module Dependency Graph

```
app
├── feature:home
├── feature:action
├── feature:pin
├── feature:edit-application-info
├── feature:edit-grid-item
├── feature:settings:*
├── common
├── design-system
├── data:repository
├── domain:*
├── framework:*
├── ui
└── service

domain:use-case
├── domain:repository
├── domain:framework
├── domain:grid
├── domain:common
└── domain:model

data:repository
├── data:datastore
├── data:room
└── domain:repository

data:datastore
├── data:datastore-proto
└── domain:repository

data:room
├── domain:model
└── domain:repository

framework:*
├── domain:framework
└── common

feature:*
├── domain:use-case
├── domain:repository
├── domain:framework
└── design-system
```

---

## Module Reference

### `app`

**Layer:** Entry point
**Namespace:** `com.eblan.launcher`

The application module. It wires together all other modules — sets up Hilt dependency injection, declares the `Application` class, and hosts the root `Activity` with Jetpack Compose navigation. This module depends on all feature, data, framework, and common modules.

---

### `build-logic`

**Layer:** Build tooling

Contains custom Gradle convention plugins that standardize build configuration across all modules (Android library setup, Compose configuration, Hilt setup, etc.). Modules opt in by applying these plugins in their `build.gradle.kts` files, following a convention-over-configuration approach.

---

### `common`

**Layer:** Application infrastructure
**Namespace:** `com.eblan.launcher.common`

Provides application-wide Hilt modules for:

- **Icon key generation** — `IconKeyGenerator` implementation for producing cache keys from app component info.
- **Coroutine dispatchers** — Binds `CoroutineDispatcher` instances for `Main`, `Default`, `IO`, and `Unconfined` to the qualifiers defined in `domain:common`.

---

## Domain Layer

The domain layer is a set of pure Kotlin (non-Android) modules. It defines entities, repository interfaces, framework abstractions, use cases, and grid algorithms. Nothing in this layer imports Android SDK classes.

### `domain:model`

**Layer:** Domain — Entities
**Namespace:** `com.eblan.launcher.domain.model`

Defines all domain entity and value-object classes (80+ data classes/sealed classes). Key groups:

| Group | Examples |
|---|---|
| Settings | `UserData`, `HomeSettings`, `AppDrawerSettings`, `GestureSettings`, `ExperimentalSettings` |
| Grid & Layout | `GridItem`, `GridItemData`, `GridItemSettings`, `HomeData`, `PageItem` |
| Applications | `EblanApplicationInfo`, `EblanApplicationInfoTag`, `ApplicationInfoGridItem` |
| Widgets | `EblanAppWidgetProviderInfo`, `WidgetGridItem` |
| Shortcuts | `EblanShortcutInfo`, `EblanShortcutConfig`, `ShortcutInfoGridItem` |
| Framework results | `LauncherAppsEvent`, `EblanUser`, `ManagedProfileResult` |
| Operations | `MoveGridItemResult`, `SyncEblanApplicationInfo`, `EditPageData` |

Uses Kotlin serialization for JSON support.

---

### `domain:repository`

**Layer:** Domain — Interfaces
**Namespace:** `com.eblan.launcher.domain.repository`

Defines 14 repository interfaces that represent all data operations the domain needs. Implementations live in `data:repository`.

| Interface | Responsibility |
|---|---|
| `GridRepository` | CRUD for all grid items; exposes a reactive `Flow` |
| `UserDataRepository` | Read/write all user settings |
| `EblanApplicationInfoRepository` | App metadata: sync, query by tag, update order indexes |
| `EblanApplicationInfoTagRepository` | Tag CRUD |
| `EblanApplicationInfoTagCrossRefRepository` | App-to-tag relationship management |
| `ApplicationInfoGridItemRepository` | Apps placed on the grid |
| `FolderGridItemRepository` | Folders and their contents |
| `WidgetGridItemRepository` | Widgets on the grid |
| `ShortcutInfoGridItemRepository` | Shortcuts on the grid |
| `ShortcutConfigGridItemRepository` | Shortcut configurations on the grid |
| `EblanAppWidgetProviderInfoRepository` | Widget provider metadata |
| `EblanShortcutConfigRepository` | Custom shortcut configurations |
| `EblanShortcutInfoRepository` | Available shortcuts |
| `EblanIconPackInfoRepository` | Icon pack metadata |

All repositories expose reactive `Flow`-based properties for live data and `suspend` functions for one-off operations.

---

### `domain:framework`

**Layer:** Domain — System service interfaces
**Namespace:** `com.eblan.launcher.domain.framework`

Defines interfaces that abstract Android system APIs, keeping domain and use-case code free of Android dependencies:

| Interface | Wraps |
|---|---|
| `LauncherAppsWrapper` | `LauncherApps` — app listing, shortcuts, events |
| `PackageManagerWrapper` | `PackageManager` — package info, system features |
| `WallpaperManagerWrapper` | `WallpaperManager` — wallpaper colors, dark text hints |
| `ResourcesWrapper` | `Resources` — system theme, resource access |
| `FileManager` | File I/O for icon storage/cache |
| `IconPackManager` | Icon pack enumeration and icon retrieval |
| `AppWidgetManagerWrapper` | `AppWidgetManager` — widget provider info |
| `AppWidgetHostWrapper` | `AppWidgetHost` — widget hosting |

Implementations of these interfaces are in the `framework:*` modules.

---

### `domain:use-case`

**Layer:** Domain — Application business logic
**Namespace:** `com.eblan.launcher.domain.usecase`

Contains 38 use cases that orchestrate domain logic. Each use case is a Hilt-injectable class with a single primary responsibility. Groups:

**Home screen data**
- `GetHomeDataUseCase` — Combines `userDataFlow`, `gridItemsFlow`, `folderGridItemWrappersFlow`, and wallpaper color changes into a single `HomeData` flow.
- `GetApplicationThemeUseCase` — Resolves the active application theme.

**Application management**
- `GetEblanApplicationInfosByUserUseCase` — Lists apps for a given user/profile.
- `GetEblanApplicationInfosByLabelAndTagUseCase` — Searches and filters apps with pagination support.
- `GetEblanApplicationInfoTagUseCase` — Retrieves tags associated with an app.
- `GetEblanAppWidgetProviderInfosByLabelUseCase` — Searches available widgets.
- `GetEblanShortcutInfosUseCase` / `GetEblanShortcutConfigsByLabelUseCase` — Lists available shortcuts.
- `UpdateEblanApplicationInfosIndexesUseCase` — Persists app sort order.

**Grid operations**
- `MoveGridItemUseCase` — Moves a grid item; delegates collision detection to `domain:grid`.
- `ResizeGridItemUseCase` — Resizes a grid item with bounds validation.
- `UpdateGridItemsAfterMoveUseCase` — Persists the result of a move operation.
- `GetGridItemByIdUseCase`, `GetFolderGridItemsUseCase`, `GetFolderGridItemsByIdUseCase` — Queries.
- `MoveFolderGridItemUseCase` — Handles cascading folder moves.
- `CachePageItemsUseCase` / `UpdatePageItemsUseCase` — Manage page-edit state.

**Launcher app events**
- `SyncDataUseCase` — Initial sync of installed apps, widgets, and shortcuts.
- `AddPackageUseCase` / `RemovePackageUseCase` / `ChangePackageUseCase` — React to app install/uninstall/update events.
- `ChangeShortcutsUseCase` — Handles shortcut change events.

**Icon pack management**
- `GetIconPackFilePathsUseCase`, `UpdateIconPackInfosUseCase`, `DeleteIconPackInfoUseCase` — Full lifecycle of icon pack metadata.

**Pinned items**
- `GetPinGridItemUseCase`, `AddPinShortcutToHomeScreenUseCase`, `AddPinWidgetToHomeScreenUseCase` — Handle system pin requests.

---

### `domain:grid`

**Layer:** Domain — Algorithm
**Namespace:** `com.eblan.launcher.domain.grid`

Implements the grid layout algorithms:

- **`GridItemConstraints.kt`** — Validates that a grid item stays within page bounds.
- **`MoveGridItem.kt`** — `resolveConflicts()` — BFS-based collision resolution that cascades displaced items to free cells. Supports three resolution strategies: Left, Right, Center.
- **`ResizeGridItem.kt`** — Adjusts item span while respecting grid boundaries.

This is a pure Kotlin module with no Android or data dependencies, making the algorithms fully unit-testable.

---

### `domain:common`

**Layer:** Domain — Cross-cutting abstractions
**Namespace:** `com.eblan.launcher.domain.common`

Provides shared domain-level abstractions:

- **`EblanDispatchers`** — Enum listing coroutine dispatcher types (`Default`, `Main`, `IO`, `Unconfined`).
- **`Dispatcher`** — `@Qualifier` annotation used with Hilt for injecting specific dispatchers.
- **`IconKeyGenerator`** — Interface for generating cache keys from app component information.

Only depends on `javax.inject`.

---

## Data Layer

The data layer provides concrete implementations of the repository interfaces and manages all persistence concerns.

### `data:repository`

**Layer:** Data — Repository implementations
**Namespace:** `com.eblan.launcher.data.repository`

Contains 19 repository implementations (prefixed `Default`). Each class implements a domain repository interface and is provided via a Hilt `RepositoryModule`.

Key implementations and their data sources:

| Implementation | Data sources |
|---|---|
| `DefaultUserDataRepository` | `data:datastore` |
| `DefaultGridRepository` | `data:room` (grid item DAOs) |
| `DefaultEblanApplicationInfoRepository` | `data:room` + `domain:framework` |
| `DefaultFolderGridItemRepository` | `data:room` |
| `DefaultWidgetGridItemRepository` | `data:room` |
| `DefaultShortcutInfoGridItemRepository` | `data:room` |
| `DefaultEblanIconPackInfoRepository` | `data:room` |

**Data mappers** in this module convert between Room entities and domain models (e.g., `EblanApplicationInfoMapper`, `FolderGridItemMapper`).

---

### `data:room`

**Layer:** Data — Local SQLite database
**Namespace:** `com.eblan.launcher.data.room`

Defines the Room database (`EblanDatabase`, currently at schema version 15) with 12 entities and 12 DAOs:

**Entities**

| Entity | Purpose |
|---|---|
| `EblanApplicationInfoEntity` | Installed app metadata |
| `ApplicationInfoGridItemEntity` | App placement on the grid |
| `WidgetGridItemEntity` | Widget placement |
| `ShortcutInfoGridItemEntity` | Shortcut placement |
| `FolderGridItemEntity` | Folder content |
| `EblanApplicationInfoTagEntity` | Tag definitions |
| `EblanApplicationInfoTagCrossRefEntity` | App-to-tag relationships |
| `EblanAppWidgetProviderInfoEntity` | Widget provider metadata |
| `EblanShortcutInfoEntity` | Shortcut metadata |
| `EblanIconPackInfoEntity` | Icon pack info |
| `EblanShortcutConfigEntity` | Shortcut configuration |
| `ShortcutConfigGridItemEntity` | Shortcut config grid placement |

**Migration strategy:** Auto-migrations handle most schema upgrades; custom migrations exist for non-trivial changes (v5→6, v8→9, v9→10).

---

### `data:datastore`

**Layer:** Data — Structured preferences
**Namespace:** `com.eblan.launcher.data.datastore`

Manages user settings using **Proto DataStore** for type-safe, versioned persistence:

- `UserDataStore` — Exposes a `Flow<UserData>` and suspend functions to update each settings group.
- `UserDataSerializer` — Handles proto serialization/deserialization.
- `DataStoreMapper` — Converts between proto-generated types and domain `UserData`.
- `DataStoreMigration` — Handles data migration between proto schema versions.

Settings groups: `HomeSettings`, `AppDrawerSettings`, `GeneralSettings`, `GestureSettings`, `ExperimentalSettings`.

---

### `data:datastore-proto`

**Layer:** Data — Schema definitions
**Type:** JVM library

Holds `.proto` files from which Kotlin classes are generated at build time:

| Proto file | Purpose |
|---|---|
| `home_settings.proto` | Grid size, dock configuration |
| `app_drawer_settings.proto` | Drawer type, columns, app ordering |
| `grid_item_settings.proto` | Per-item appearance customization |
| `gesture_settings.proto` | Gesture-to-action bindings |
| `general_settings.proto` | Theme and text color |
| `experimental_settings.proto` | Feature flags |

---

## Presentation Layer

### `feature:home`

**Layer:** Presentation — Main launcher screen
**Namespace:** `com.eblan.launcher.feature.home`

The main home screen. The primary entry point for the user experience.

**Key components:**

- `HomeScreen.kt` — Root composable; renders the grid pager, dock, and floating controls.
- `HomeViewModel.kt` — Central ViewModel (~650 lines); injected with 20+ use cases and repositories. Manages:
  - Grid item drag-and-drop movement and resizing.
  - Page editing (adding/removing items).
  - App drawer, widget picker, and shortcut picker integration.
  - Folder creation and management.
  - Package install/remove/update event handling.
- `HomeUiState` — Sealed class: `Loading` | `Success(HomeData)`.

**Supporting composables:** `Grid`, `GridItem`, `PageIndicator`, `SearchBar`, `ApplicationScreen`, `PrivateSpaceScreen`, `HorizontalApplicationScreen`, `ShortcutInfoMenu`.

---

### `feature:action`

**Layer:** Presentation
**Namespace:** `com.eblan.launcher.feature.action`

Provides the UI for selecting and binding actions to gestures (e.g., double-tap launches an app, swipe opens a specific screen). Uses `ActionViewModel` backed by domain use cases.

---

### `feature:pin`

**Layer:** Presentation
**Namespace:** `com.eblan.launcher.feature.pin`

Handles system-initiated pin requests — when another app asks to pin a shortcut or widget to the home screen.

---

### `feature:edit-application-info`

**Layer:** Presentation
**Namespace:** `com.eblan.launcher.feature.editapplicationinfo`

Screen for customizing an app's label, icon, and tags. Includes `UpdateTagDialog` for managing app tags.

---

### `feature:edit-grid-item`

**Layer:** Presentation
**Namespace:** `com.eblan.launcher.feature.editgriditem`

Screen for customizing a grid item's appearance: size, rotation, background color, icon size, label size and color, and icon/label visibility.

---

### `feature:settings:*`

**Layer:** Presentation — Settings screens

Six settings modules, each providing one settings screen:

| Module | Screen |
|---|---|
| `settings:settings` | Main settings entry point |
| `settings:home` | Home screen grid and dock settings |
| `settings:app-drawer` | App drawer layout and ordering |
| `settings:general` | Theme and text color |
| `settings:gestures` | Gesture-to-action bindings |
| `settings:experimental` | Feature flags |

Each follows the same pattern: `*Screen.kt` + `*ViewModel.kt` + dependency on `domain:repository` and `domain:use-case`.

---

### `design-system`

**Layer:** Presentation — Shared UI component library
**Namespace:** `com.eblan.launcher.designsystem`

Centralized Material Design 3 component library used by all feature modules:

- `Theme.kt` — Application-wide Material 3 theming.
- `EblanLauncherIcons.kt` — Custom icon definitions.
- `Animation.kt` — Reusable animation helpers.
- `Dialog.kt` — Standard dialog components.
- `RadioButton.kt` — Custom radio button.

Depends on Compose Material 3, Coil (image loading), and `domain:model`.

---

### `ui`

**Layer:** Presentation — Shared UI utilities
**Namespace:** `com.eblan.launcher.ui`

Shared dialogs and settings composables used across multiple feature modules:

**Dialogs:** `ColorPickerDialog`, `EblanActionDialog`, `IconPackInfoFilesDialog`, `RadioOptionsDialog`, `SelectApplicationDialog`, `TextColorDialog`, `TextFieldDialog`, `CustomLabelDialog`.

**Settings composables:** `Settings`, `EblanActionSettings`, `GridItemSettings`.

Also exports all `framework:*` modules as API dependencies, so feature modules can access framework implementations through a single dependency on `:ui`.

---

### `service`

**Layer:** Presentation — Android services
**Namespace:** `com.eblan.launcher.service`

Background services for system integration:

- `EblanAccessibilityService` — Detects when the accessibility service is running.
- `EblanNotificationListenerService` — Listens for notifications.
- `IconPackInfoService` — Broadcasts icon pack status updates to interested components.

---

## Framework Layer

Each `framework:*` module provides a concrete Android implementation of one interface from `domain:framework`. This keeps Android SDK imports out of the domain layer and makes all system interactions replaceable and testable.

| Module | Interface | Android API wrapped |
|---|---|---|
| `framework:launcher-apps` | `LauncherAppsWrapper` | `LauncherApps` |
| `framework:package-manager` | `PackageManagerWrapper` | `PackageManager` |
| `framework:wallpaper-manager` | `WallpaperManagerWrapper` | `WallpaperManager` |
| `framework:user-manager` | — | `UserManager` |
| `framework:widget-manager` | `AppWidgetManagerWrapper`, `AppWidgetHostWrapper` | `AppWidgetManager`, `AppWidgetHost` |
| `framework:icon-pack-manager` | `IconPackManager` | Intent-based icon pack enumeration |
| `framework:file-manager` | `FileManager` | File I/O |
| `framework:resources` | `ResourcesWrapper` | `Resources` |
| `framework:accessibility-manager` | — | `AccessibilityManager` |
| `framework:notification-manager` | — | `NotificationManager` |
| `framework:settings` | — | `Settings` |
| `framework:image-serializer` | — | `Drawable` → file serialization |

Each module contains:
1. An implementation class (e.g., `DefaultWallpaperManagerWrapper`).
2. A Hilt module binding the implementation to its interface.
