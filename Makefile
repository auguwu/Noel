# 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
# Copyright 2021-2023 Noel <cutie@floofy.dev>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Usage: `make help`
.PHONY: help
help: ## Prints the help usage on Noel's Makefile.
	@printf "\033[34m─────────────────────────────────────────────────────────────────────────────────────\033[0m\n"
	@printf "Noel uses Make to automate most repetitive Gradle tasks (i.e, :bot:installDist), but\n"
	@printf "Gradle is our main build-system and this Makefile doesn't give granular control over\n"
	@printf "Gradle execution."
	@printf "\n"
	@printf "\n:: Usage ::\n"
	@printf "make <target>\n"
	@printf "\n:: Targets ::\n"
	@awk 'BEGIN {FS = ":.*##"; } /^[a-zA-Z_-]+:.*?##/ { printf "  make \033[36m%-20s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 25) } ' $(MAKEFILE_LIST)

.PHONY: run
run: build ## Builds and runs Noel
	@./bot/build/install/noel/bin/noel

.PHONY: build
build: spotless ## Runs the `spotless` target, and builds Noel
	@./gradlew :bot:installDist
	@chmod +x ./bot/build/install/noel/bin/noel

.PHONY: spotless
spotless: ## Runs the Spotless formatter on the project
	@./gradlew spotlessApply

# Not recommended but whatever
.PHONY: kill-gradle-daemons
kill-gradle-daemons: ## Kills all the Gradle daemons
	@pkill -f '.*GradleDaemon.*'
