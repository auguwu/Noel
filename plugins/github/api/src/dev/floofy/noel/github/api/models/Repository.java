/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2026 Noel Towa <cutie@floofy.dev>
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package dev.floofy.noel.github.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/// [Read more in the GitHub API documentation for `GET /repos/{owner}/{repo}`](https://docs.github.com/en/rest/repos/repos?apiVersion=2026-03-10#get-a-repository)
public record Repository(
        int id,
        String name,
        String fullName,
        @JsonProperty("private") boolean isPrivate,
        String htmlUrl,
        String description,
        boolean fork,
        @JsonProperty("stargazers_count") int stars,
        @JsonProperty("open_issues_count") int openIssues,
        License license
) {
    public record License(
            String name,
            @JsonProperty("spdx_id") String spdxId
    ) {}
}
