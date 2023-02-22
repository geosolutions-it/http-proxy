![Build Status](https://github.com/geosolutions-it/http-proxy/actions/workflows/CI.yml/badge.svg)
[![Build Status](https://travis-ci.org/geosolutions-it/http-proxy.svg?branch=1.0.x)](https://travis-ci.org/geosolutions-it/http-proxy)


http-proxy is a simple Java based HTTP Proxy that can be used to make cross origin calls from JavaScript based web applications.

It is free and Open Source, for information on the license please see the LICENSE file.

## Release process 

- [Release process](https://github.com/geosolutions-it/geostore/wiki/Release-Process)

## Relevant Workflows

- CI (`CI.yml`): Automatically does tests for Pull request or commits on `master`. For commits on the main repo (e.g. when PR are merged on `master` or stable branches, the workflow publish also the artifacts on [GeoSolutions Maven Repository](https://maven.geo-solutions.it)
- **[Cut release branch](https://github.com/geosolutions-it/http-proxy/actions/workflows/cut-major-branch.yml)**: (`cut-major-branch.yml`): Manual workflow that allows to create a stable branch named `<current-version>.x` and create a pull request for updating `master` branch `-SNAPSHOT` version with the new data. 
