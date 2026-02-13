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
  description = "🐾✨ Discord bot made to manage my servers made in Java";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs = {nixpkgs, ...}: let
    systems = ["x86_64-linux" "aarch64-darwin"];
    eachSystem = nixpkgs.lib.genAttrs systems;

    nixpkgsFor = system:
      import nixpkgs {
        inherit system;
      };
  in {
    formatter = eachSystem (system: (nixpkgsFor system).alejandra);
    devShells = eachSystem (system: let
      pkgs = nixpkgsFor system;
    in {
      default = pkgs.callPackage ./nix/devshell.nix {};
    });
  };
}
