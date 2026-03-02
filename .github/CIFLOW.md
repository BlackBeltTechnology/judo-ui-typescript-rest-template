# CI/CD Flow and Branching Strategy

This document describes the Git branching strategy and GitHub Actions CI/CD pipelines used by the project. The workflow is based on GitFlow with automated build, test, deploy, and release processes.

## Branch Strategy

The project uses a GitFlow-based branching model. All development happens on `develop`, releases are cut from release branches, and `master` always reflects the latest production release.

| Branch Pattern | Base Branch | Purpose |
|---------------|-------------|---------|
| `develop` | — | Main development branch; latest active-version sources |
| `feature/JNG-<number>_<summary>` | `develop` | New features for the next release |
| `release/<version>` or `<version>` | `develop` | Stabilization branch for an upcoming release |
| `bugfix/JNG-<number>_<summary>` | release branch | Fixes applied during release testing (before merge to master) |
| `support/JNG-<number>_<summary>` | release branch | Minor changes to a previous release |
| `hotfix/JNG-<number>_<summary>` | `master` | Critical fixes applied to both master and develop |
| `master` | — | Latest released version |

```mermaid
gitGraph
    commit id: "initial"
    branch develop
    commit id: "dev-1"
    branch feature/JNG-1
    commit id: "feat-1"
    commit id: "feat-2"
    checkout develop
    merge feature/JNG-1
    commit id: "dev-2"
    branch release/1.0
    commit id: "rc-1"
    branch bugfix/JNG-4
    commit id: "fix-1"
    checkout release/1.0
    merge bugfix/JNG-4
    checkout develop
    merge release/1.0
    checkout master
    merge release/1.0 id: "v1.0"
    checkout develop
    commit id: "dev-3"
```

## Version Numbers

Versions follow semantic versioning with these rules:

| Event | Version Change |
|-------|---------------|
| Start a `feature/` branch | No version change |
| Start a `release/` branch | 2nd number on `develop` incremented |
| Start a `bugfix/` branch | No version change (applied to release branch pre-release) |
| Start a `support/` branch | 3rd number incremented |
| Start a `hotfix/` branch | 4th number incremented |

On `develop` and `increment/*` branches, the version includes a timestamp and commit ID: `major.minor.qualifier.date_commitId_branchName`.

On `master` and `release/*` branches, the version is the clean semantic version from `pom.xml` (without `-SNAPSHOT`).

## GitHub Actions Workflows

### build.yml — Build, Test, Deploy

This is the main CI workflow. It triggers on pushes to `develop` and pull requests targeting `develop`, `master`, `increment/*`, or `release/*`.

```mermaid
flowchart TD
    Trigger["Push to develop\nor PR to develop/master/release/*"]
    Trigger --> BranchCheck{Branch type?}

    BranchCheck -->|"master, release/*"| CleanVersion["Set version from pom.xml\n(without -SNAPSHOT)"]
    BranchCheck -->|"develop, increment/*"| TimestampVersion["Set version:\nmajor.minor.qualifier.date_commitId_branch"]

    CleanVersion --> Build["Build & deploy to Nexus"]
    TimestampVersion --> Build

    Build --> Tag["Create git tag\nv<version>"]

    Tag --> IsMergeable{Is increment/*\nor release/*?}
    IsMergeable -->|Yes| MergeTag["Create tag\nmerge-pr/<version>"]
    MergeTag --> TriggerMerge["Triggers merge-pr-tagged.yml"]

    Tag --> IsDevelop{Is develop?}
    IsDevelop -->|Yes| Changelog["Build changelog"]
    Changelog --> PreRelease["Create GitHub pre-release"]
```

### merge-pr-tagged.yml — Auto-Merge Pull Requests

Triggered when a `merge-pr/*` tag is pushed. Routes the merge based on version format.

```mermaid
flowchart TD
    Trigger["Push on merge-pr/* tag"]
    Trigger --> ExtractVersion["Get version from tag name"]
    ExtractVersion --> VersionCheck{Version format?}

    VersionCheck -->|"major.minor.qualifier\n(release)"| MergeMaster["Merge PR to master"]
    MergeMaster --> TriggerRelease["Triggers create-release-on-master.yml"]

    VersionCheck -->|"Other format\n(increment)"| SquashDevelop["Squash PR to develop"]
    SquashDevelop --> TriggerBuild["Triggers build.yml"]

    MergeMaster --> Cleanup["Delete merge-pr/<version> tag"]
    SquashDevelop --> Cleanup
```

### create-release-on-master.yml — Create Production Release

Triggered by pushes to `master`. Creates the official GitHub release with a changelog.

```mermaid
flowchart LR
    Trigger["Push on master"] --> GetVersion["Get version from tag"]
    GetVersion --> Changelog["Build changelog"]
    Changelog --> Release["Create GitHub release\n(latest)"]
```

### release.yml — Start a Release

Manually triggered with a version number (or `auto` to use the version from `pom.xml`).

```mermaid
flowchart TD
    Trigger["Manual trigger\nwith version"]
    Trigger --> VersionCheck{Given version?}
    VersionCheck -->|"'auto'"| FromPom["Use version from pom.xml\n(without -SNAPSHOT)"]
    VersionCheck -->|"Specific"| UseGiven["Use given version"]

    FromPom --> CalcNext["Next version = qualifier + 1"]
    UseGiven --> CalcNext

    CalcNext --> MasterPR["Create PR on master\nwith release version"]
    CalcNext --> DevelopPR["Create PR on develop\nwith next version"]

    MasterPR --> BuildMaster["Triggers build.yml"]
    DevelopPR --> BuildDevelop["Triggers build.yml"]
```

### Complete Workflow Interaction

```mermaid
graph TD
    subgraph "Development"
        Dev["develop branch"]
        Feature["feature/ branches"]
        Feature -->|merge| Dev
    end

    subgraph "Release Process"
        ReleaseYml["release.yml\n(manual trigger)"]
        ReleaseBranch["release/ branch"]
        Bugfix["bugfix/ branches"]
        Bugfix -->|merge| ReleaseBranch
    end

    subgraph "CI/CD"
        BuildYml["build.yml"]
        MergePR["merge-pr-tagged.yml"]
        CreateRelease["create-release-on-master.yml"]
    end

    subgraph "Production"
        Master["master branch"]
        GHRelease["GitHub Release"]
    end

    Dev -->|push/PR| BuildYml
    ReleaseYml -->|creates PRs| BuildYml
    ReleaseBranch -->|PR| BuildYml
    BuildYml -->|"merge-pr/* tag"| MergePR
    MergePR -->|"release merge"| Master
    Master -->|push| CreateRelease
    CreateRelease --> GHRelease
    MergePR -->|"increment squash"| Dev
```

## Development Rules

> **Important:** Every commit must include a JIRA ticket number (e.g., `JNG-123`). There is no commit without a ticket number.

Issue tracking is managed in [JIRA](https://blackbelt.atlassian.net/jira/dashboards).
