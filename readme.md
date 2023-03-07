![Build Status](https://github.com/geosolutions-it/http-proxy/actions/workflows/CI.yml/badge.svg)


http-proxy is a simple Java based HTTP Proxy that can be used to make cross origin calls from JavaScript based web applications.

It is free and Open Source, for information on the license please see the LICENSE file.

## Release process 

This Project follow the same convention of GeoStore

- *Cut the major release branch* : run the [Cut release branch](https://github.com/geosolutions-it/http-proxy/actions/workflows/cut-major-branch.yml) workflow.
- Run the [Release process](https://github.com/geosolutions-it/http-proxy/actions/workflows/release.yml) with the following parameters: 
   - branch: use the release branch (created by the previous workflow), e.g. 1.4.x
   - version: number of the stable version: e.g. 1.4.0
   - base: number of the base version (e.g. `1.4` ). Used to restore snapshot to <BASE>-SNAPSHOT. 

## Relevant Workflows

- CI (`CI.yml`): Automatically does tests for Pull request or commits on `master`. For commits on the main repo (e.g. when PR are merged on `master` or stable branches, the workflow publish also the artifacts on [GeoSolutions Maven Repository](https://maven.geo-solutions.it)
- **[Cut release branch](https://github.com/geosolutions-it/http-proxy/actions/workflows/cut-major-branch.yml)**: (`cut-major-branch.yml`): Manual workflow that allows to create a stable branch named `<current-version>.x` and create a pull request for updating `master` branch `-SNAPSHOT` version with the new data. 
- Release (`release.yml`): 
   - Fixes maven versions
   - Commits changes and tag the commit 
   - Restores the SNAPSHOTS on the branch
   - Creates a Github release on the tagged commit
   
   
