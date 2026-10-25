# 🐾✨ Noel: Discord bot made to manage my servers made in Java
# Copyright 2021-2026 Noel Towa <cutie@floofy.dev>, et al.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
{
  llvmPackages_21,
  temurin-bin-25,
  bazel_9,
  mkShell,
  lib,
  coreutils,
  writeText,
  stdenv
}: let
    toolsPath = lib.makeBinPath (stdenv.initialPath ++ [
        llvmPackages_21.bintools
        llvmPackages_21.clang
        coreutils
    ]);

    nixBazelrc = writeText ".nix.bazelrc" ''
        common --repo_env=CC=${llvmPackages_21.clang}/bin/clang
        common --repo_env=AR=${llvmPackages_21.bintools}/bin/ar
        common --repo_env=PATH=${toolsPath}
        common --repo_env=JAVA_HOME=${temurin-bin-25}
    '';
in
mkShell {
  packages = [
    temurin-bin-25
    bazel_9
    llvmPackages_21.bintools
    llvmPackages_21.clang
  ];

  name = "noel-bot-dev";
  shellHook = ''
    rm .nix.bazelrc
    ln -s ${nixBazelrc} .nix.bazelrc
  '';
}
