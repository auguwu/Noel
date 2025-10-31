# 🐾✨ Noel: Discord bot made to manage my servers made in Java
# Copyright 2021-2025 Noel Towa <cutie@floofy.dev>
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
    temurin-bin,
    bazel_8,
    clang_21,
    mkShell
}: mkShell {
    packages = [
        temurin-bin
        bazel_8
        clang_21
    ];

    name = "noel-bot-dev";
    shellHook = ''
        # For some reason, the new Bazel plugin for IntelliJ and for building
        # require a C++ toolchain... weird?
        export CC="${clang_21}/bin/clang"
        export CXX="${clang_21}/bin/clang++"
    '';
}
