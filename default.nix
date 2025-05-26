{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
      pkgs.libpulseaudio
      pkgs.libGL
      pkgs.glfw
      pkgs.openal
      pkgs.stdenv.cc.cc.lib
  ];
}
