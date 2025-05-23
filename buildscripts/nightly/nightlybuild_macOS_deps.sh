#!/bin/bash

# Build external open-source library dependencies for Micro-Manager macOS build.
#
# All libraries are currently built as static-only libraries, except for
# libgphoto2 and its dependency libltdl, which need to be shared to work
# (because libgphoto2 uses libltdl for dynamic loading, and building a static
# version would not allow that due to libtool and libltdl's design).
#
# However, using dynamic libraries for dependencies could potentially simplify
# the build by eliminating the need to explicitly specify transitive
# dependencies in many cases. This would require that we write an install
# script that rewrites the library paths using @rpath or (better yet)
# @loader_path; see dyld(1).
#
# Third-party packages not represented here:
# - Build tools: should be installed using Homebrew (swig, cmake, autoconf,
#   automake, libtool, pkg-config, python3)
# - JDK: Apple JDK 6 from http://support.apple.com/kb/DL1572
#        Apple Java for OS X 2013-005 Developer Package (download requires
#        Apple Developer account)
# - Device-specific proprietary frameworks (currently in /Library/Frameworks)

# This script places and builds everything under $MM_DEPS_PREFIX.
# $MM_DEPS_PREFIX/downloads - tarballs
# $MM_DEPS_PREFIX/src - source and build (VPATH is not used)
# $MM_DEPS_PREFIX/include, $MM_DEPS_PREFIX/lib, etc. - staged libraries

set -e

usage() { echo "Usage: $0 [-d]" 1>&2; exit 1; }

do_download=no
while getopts ":d" o; do
   case "$o" in
      d) do_download=yes ;;
      *) usage ;;
   esac
done


##
## Setup
##

source "`dirname $0`/nightlybuild_macOS_defs.sh"

# GNU libtool (i.e. any libtoolized project) can mess around with the value of
# MACOSX_DEPLOYMENT_TARGET, so passing the correct compiler and linker flags
# (clang -mmacosx-version-min=10.9; ld -macosx_version_min 10.9) is not enough;
# we need to set this environment variable. It is also simpler than using
# command line flags.  Do the same for SDKROOT (instead of clang -isysroot; ld
# -syslibroot).
# Note that manually running e.g. `make' on directories configured with this
# script requires manually setting these environment variables. Failure to do
# so will result in broken binaries.
export MACOSX_DEPLOYMENT_TARGET=$MM_MACOSX_VERSION_MIN
export SDKROOT=$MM_MACOSX_SDKROOT


##
## Download
##

mkdir -p "$MM_DEPS_PREFIX"/downloads
cd "$MM_DEPS_PREFIX"/downloads
if [ "$do_download" = yes ]; then
   [ -f boost_1_85_0.tar.bz2 ] || curl -LO https://archives.boost.io/release/1.85.0/source/boost_1_85_0.tar.bz2
   [ -f libusb-1.0.18.tar.bz2 ] || curl -LO http://sourceforge.net/projects/libusb/files/libusb-1.0/libusb-1.0.18/libusb-1.0.18.tar.bz2
   [ -f libusb-compat-0.1.5.tar.bz2 ] || curl -LO http://sourceforge.net/projects/libusb/files/libusb-compat-0.1/libusb-compat-0.1.5/libusb-compat-0.1.5.tar.bz2
   [ -f hidapi-0.8.0-rc1.tar.gz ] || curl -LO https://github.com/signal11/hidapi/archive/hidapi-0.8.0-rc1.tar.gz
   [ -f libexif-0.6.21.tar.bz2 ] || curl -L -o libexif-0.6.21.tar.bz2 http://sourceforge.net/projects/libexif/files/libexif/0.6.21/libexif-0.6.21.tar.bz2/download
   [ -f libtool-2.5.4.tar.gz ] || curl -LO https://ftpmirror.gnu.org/libtool/libtool-2.5.4.tar.gz
   [ -f libgphoto2-2.5.2.tar.bz2 ] || curl -L -o libgphoto2-2.5.2.tar.bz2 http://sourceforge.net/projects/gphoto/files/libgphoto/2.5.2/libgphoto2-2.5.2.tar.bz2/download
   [ -f FreeImage3180.zip ] || curl -LO http://downloads.sourceforge.net/freeimage/FreeImage3180.zip
   [ -f libdc1394-2.2.1.tar.gz ] || curl -L -o libdc1394-2.2.1.tar.gz http://sourceforge.net/projects/libdc1394/files/libdc1394-2/2.2.1/libdc1394-2.2.1.tar.gz/download
   [ -f opencv-2.4.13.6.zip ] || curl -L -o opencv-2.4.13.6.zip https://github.com/opencv/opencv/archive/refs/tags/2.4.13.6.zip
   [ -f msgpack-cxx-7.0.0.tar.gz ] || curl -LO https://github.com/msgpack/msgpack-c/releases/download/cpp-7.0.0/msgpack-cxx-7.0.0.tar.gz
fi

cat >sha1sums <<EOF
ed58c632befe0d299b39f9e23de1fc20d03870d7  boost_1_85_0.tar.bz2
5f7bbf42a4d6e6b88d5e7666958c80f8455ee915  libusb-1.0.18.tar.bz2
062319276d913c753a4b1341036e6a2e42abccc9  libusb-compat-0.1.5.tar.bz2
5e72a4c7add8b85c8abcdd360ab8b1e1421da468  hidapi-0.8.0-rc1.tar.gz
a52219b12dbc8d33fc096468591170fda71316c0  libexif-0.6.21.tar.bz2
77227188ead223ed8ba447301eda3761cb68ef57  libtool-2.5.4.tar.gz
6b70ff6feec62a955bef1fc9a2b16dd07f0e277a  libgphoto2-2.5.2.tar.bz2
38daa9d8f1bca2330a2eaa42ec66fbe6ede7dce9  FreeImage3180.zip
b92c9670b68c4e5011148f16c87532bef2e5b808  libdc1394-2.2.1.tar.gz
a6c3d6ac8091e3311fc44125e017dd1e88e74825  opencv-2.4.13.6.zip
37bbdbf69ef44392c7af215b9cb419891a9e1c9c  msgpack-cxx-7.0.0.tar.gz
EOF
shasum -c sha1sums || { echo "SHA1 checksum mismatch or missing file; remove file and rerun with -d flag"; exit 1; }


##
## Build
##

mkdir -p "$MM_DEPS_PREFIX"/src
cd "$MM_DEPS_PREFIX"/src

#
# Boost
#

tar xf ../downloads/boost_1_85_0.tar.bz2
pushd boost_1_85_0
./bootstrap.sh
cat >project-config.jam <<EOF
import option ;
import feature ;
using clang : ${MM_ARCH} : clang++ ${MM_ARCH_FLAGS} ;
project : default-build <toolset>clang-${MM_ARCH} ;
option.set keep-going : false ;
EOF
./b2 --prefix=${MM_DEPS_PREFIX} link=static threading=multi architecture=x86 address-model=64 \
  cflags="${MM_CFLAGS}" cxxflags="${MM_CXXFLAGS}" \
  --with-atomic --with-chrono --with-date_time --with-filesystem --with-system --with-thread \
  $MM_PARALLELMAKEFLAG install
popd


#
# libusb-1.0
#

tar xjf ../downloads/libusb-1.0.18.tar.bz2
pushd libusb-1.0.18
eval ./configure $MM_DEPS_CONFIGUREFLAGS --enable-static --disable-shared --with-pic
make $MM_PARALLELMAKEFLAG
make install
popd


#
# libusb-compat
# (depends on libusb-1.0)
#

tar xjf ../downloads/libusb-compat-0.1.5.tar.bz2
pushd libusb-compat-0.1.5
eval ./configure $MM_DEPS_CONFIGUREFLAGS --enable-static --disable-shared --with-pic PKG_CONFIG_PATH=$MM_DEPS_PREFIX/lib/pkgconfig
make $MM_PARALLELMAKEFLAG
make install
popd
 

#
# HIDAPI
#

tar xzf ../downloads/hidapi-0.8.0-rc1.tar.gz
pushd hidapi-hidapi-0.8.0-rc1
patch -p2 <<'END_OF_PATCH'
--- a/hidapi-hidapi-0.8.0-rc1/configure.ac	2021-12-10 12:07:50.000000000 -0600
+++ b/hidapi-hidapi-0.8.0-rc1/configure.ac	2021-12-10 12:08:29.000000000 -0600
@@ -20,7 +20,6 @@
 
 AC_CONFIG_MACRO_DIR([m4])
 AM_INIT_AUTOMAKE([foreign -Wall -Werror])
-AC_CONFIG_MACRO_DIR([m4])
 
 m4_ifdef([AM_PROG_AR], [AM_PROG_AR])
 LT_INIT
END_OF_PATCH
./bootstrap
eval ./configure $MM_DEPS_CONFIGUREFLAGS --enable-static --disable-shared --with-pic
make
make install
popd


#
# libexif
# (dependency of libgphoto2)
#

tar xjf ../downloads/libexif-0.6.21.tar.bz2
pushd libexif-0.6.21
eval ./configure $MM_DEPS_CONFIGUREFLAGS --enable-static --disable-shared --with-pic PKG_CONFIG_PATH=$MM_DEPS_PREFIX/lib/pkgconfig
make $MM_PARALLELMAKEFLAG
make install
popd


#
# libtool
#

tar xzf ../downloads/libtool-2.5.4.tar.gz
pushd libtool-2.5.4
eval ./configure $MM_DEPS_CONFIGUREFLAGS --enable-shared --disable-static --enable-ltdl-install
make $MM_PARALLELMAKEFLAG
make install
popd


#
# libgphoto2
# Currently, we use a shared library and rewrite the paths in the Makefile.am
# install target for libmmgr_dal_GPhoto.
#

tar xjf ../downloads/libgphoto2-2.5.2.tar.bz2
pushd libgphoto2-2.5.2

# The configure script requires explicit LTDLINCL, LIBLTDL, and LDFLAGS to find
# libltdl (which must of course be dual-arch).

# libxml2 from the Mac OS X SDK (i.e. the dylib bundled with Mac OS X) is used.

# _DARWIN_C_SOURCE needs to be defined for flock() calls to compile (see
# sys/fcntl.h).

# --without-libusb is key to prevent errors when both libusb-1.0 and libusb (0.1 or compat) are installed.


eval ./configure $MM_DEPS_CONFIGUREFLAGS_NOCPPLD "CPPFLAGS=\"\$MM_CPPFLAGS -isystem \$SDKROOT/usr/include/libxml2 -D_DARWIN_C_SOURCE\" LDFLAGS=\"\$MM_LDFLAGS\"" LTDLINCL=-I$MM_DEPS_PREFIX LIBLTDL=-lltdl PKG_CONFIG_PATH=$MM_DEPS_PREFIX/lib/pkgconfig --without-libusb "LIBUSB1_LIBS=\"-lusb-1.0 -framework IOKit -framework CoreFoundation\" LIBUSB1_CFLAGS=\"-I\$MM_DEPS_PREFIX/include/libusb-1.0\"" --enable-shared --disable-static
make $MM_PARALLELMAKEFLAG
make install
popd


#
# FreeImage
#

unzip -oq ../downloads/FreeImage3180.zip
pushd FreeImage

# FreeImage comes with a Makefile.osx, but it is hardcoded to use outdated
# tools and is therefore useless. Replace the makefile with a minimal version
# for building a fat static library.
cat > Makefile.clang <<'END_OF_MAKEFILE'
include Makefile.srcs

CC = cc-from-cmdline
CXX = cxx-from-cmdline
CFLAGS = -Os -fexceptions -fvisibility=hidden -DPIC -fno-common
CXXFLAGS = $(CFLAGS) -Wno-ctor-dtor-privacy
CPPFLAGS = -DNO_LCMS $(MM_CPPFLAGS) $(INCLUDE)
LIBTOOL = /usr/bin/libtool

TARGET = freeimage
STATICLIB = lib$(TARGET).a
HEADER = Source/FreeImage.h

MODULES = $(SRCS:.c=.o)
MODULES := $(MODULES:.cpp=.o)

all: dist

dist: FreeImage
	cp $(STATICLIB) Dist
	cp $(HEADER) Dist

FreeImage: $(STATICLIB)

$(STATICLIB): $(MODULES)
	$(LIBTOOL) -static -o $@ $(MODULES)

clean:
	rm -f Dist/$(STATICLIB) Dist/$(HEADER) $(MODULES) $(STATICLIB)
END_OF_MAKEFILE

# Some of the source files have CRLF newlines. Always use --ignore-whitespace
# to patch.

# Disable the JXR (JPEG XR) plugin, which does not build on macOS.
sed -i '' 's/[^ ]*LibJXR[^ ]*//g' Makefile.srcs
sed -i '' 's/[^ ]*PluginJXR[^ ]*//g' Makefile.srcs
patch --ignore-whitespace -p1 <<'END_OF_PATCH'
--- a/Source/FreeImage/Plugin.cpp
+++ b/Source/FreeImage/Plugin.cpp
@@ -272,9 +272,6 @@ FreeImage_Initialise(BOOL load_local_plugins_only) {
 			s_plugins->AddNode(InitPICT);
 			s_plugins->AddNode(InitRAW);
 			s_plugins->AddNode(InitWEBP);
-#if !(defined(_MSC_VER) && (_MSC_VER <= 1310))
-			s_plugins->AddNode(InitJXR);
-#endif // unsupported by MS Visual Studio 2003 !!!
 			
 			// external plugin initialization
 
END_OF_PATCH

# Patch to fix wrong fdopen() #define (missing #include)
patch --ignore-whitespace -p1 <<'END_OF_PATCH'
--- a/Source/ZLib/zutil.h
+++ b/Source/ZLib/zutil.h
@@ -136,6 +136,7 @@ extern z_const char * const z_errmsg[10]; /* indexed by 2-zlib_error */
 #    if defined(__MWERKS__) && __dest_os != __be_os && __dest_os != __win32_os
 #      include <unix.h> /* for fdopen */
 #    else
+#      include <stdio.h>
 #      ifndef fdopen
 #        define fdopen(fd,mode) NULL /* No fdopen() */
 #      endif
END_OF_PATCH

# Patch to add another missing #include
patch --ignore-whitespace -p1 <<'END_OF_PATCH'
--- a/Source/ZLib/gzguts.h
+++ b/Source/ZLib/gzguts.h
@@ -33,6 +33,8 @@
 
 #ifdef _WIN32
 #  include <stddef.h>
+#else
+#  include <unistd.h>
 #endif
 
 #if defined(__TURBOC__) || defined(_MSC_VER) || defined(_WIN32)
END_OF_PATCH

# Patch to remove an obsolete #include that no longer works
# See https://github.com/pnggroup/libpng/pull/529
patch --ignore-whitespace -p1 <<'END_OF_PATCH'
--- a/Source/LibPNG/pngpriv.h
+++ b/Source/LibPNG/pngpriv.h
@@ -514,18 +514,8 @@
     */
 #  include <float.h>
 
-#  if (defined(__MWERKS__) && defined(macintosh)) || defined(applec) || \
-    defined(THINK_C) || defined(__SC__) || defined(TARGET_OS_MAC)
-   /* We need to check that <math.h> hasn't already been included earlier
-    * as it seems it doesn't agree with <fp.h>, yet we should really use
-    * <fp.h> if possible.
-    */
-#    if !defined(__MATH_H__) && !defined(__MATH_H) && !defined(__cmath__)
-#      include <fp.h>
-#    endif
-#  else
-#    include <math.h>
-#  endif
+#  include <math.h>
+
 #  if defined(_AMIGA) && defined(__SASC) && defined(_M68881)
    /* Amiga SAS/C: We must include builtin FPU functions when compiling using
     * MATH=68881
END_OF_PATCH

make -f Makefile.clang $MM_PARALLELMAKEFLAG CC="$MM_CC" CXX="$MM_CXX" MM_CPPFLAGS="$MM_CPPFLAGS"
cp Dist/FreeImage.h $MM_DEPS_PREFIX/include
cp Dist/libfreeimage.a $MM_DEPS_PREFIX/lib

pushd Wrapper/FreeImagePlus
# This one doesn't even come with a makefile(!)
cat > Makefile.clang <<'END_OF_MAKEFILE'
CC = cc-from-cmdline
CXX = cxx-from-cmdline
CFLAGS = -Os -fexceptions -fvisibility=hidden -DPIC -fno-common
CXXFLAGS = $(CFLAGS) -Wno-ctor-dtor-privacy
CPPFLAGS = -DNO_LCMS $(MM_CPPFLAGS) -I. -I../../Source
LIBTOOL = /usr/bin/libtool

TARGET = freeimageplus
STATICLIB = lib$(TARGET).a
HEADER = FreeImagePlus.h

SRCS = $(wildcard src/*.cpp)
MODULES = $(SRCS:.c=.o)
MODULES := $(MODULES:.cpp=.o)

all: dist

dist: FreeImagePlus
	cp *.a Dist
	cp $(HEADER) Dist

FreeImagePlus: $(STATICLIB)

$(STATICLIB): $(MODULES)
	$(LIBTOOL) -static -o $@ $(MODULES)

clean:
	rm -f Dist/$(STATICLIB) Dist/$(HEADER) $(MODULES) $(STATICLIB)
END_OF_MAKEFILE

make -f Makefile.clang $MM_PARALLELMAKEFLAG CC="$MM_CC" CXX="$MM_CXX" MM_CPPFLAGS="$MM_CPPFLAGS"
cp Dist/FreeImagePlus.h $MM_DEPS_PREFIX/include
cp Dist/libfreeimageplus.a $MM_DEPS_PREFIX/lib
popd # Wrapper

popd # FreeImage


#
# libdc1394-2
#

tar xzf ../downloads/libdc1394-2.2.1.tar.gz
pushd libdc1394-2.2.1

# Skip broken check for IOKit (patch configure, not configure.in, because
# autoreconf does not run -- looks like some m4 files are missing from the
# tarball)
patch -p2 <<'END_OF_PATCH'
--- a/libdc1394-2.2.1/configure 2013-01-28 02:47:43.000000000 +0000
+++ b/libdc1394-2.2.1/configure   2014-01-16 23:28:55.000000000 +0000
@@ -13609,13 +13609,13 @@

     ;;
 *-*-darwin*)
-    { $as_echo "$as_me:${as_lineno-$LINENO}: checking for IOMasterPort in -lIOKit" >&5
-$as_echo_n "checking for IOMasterPort in -lIOKit... " >&6; }
+    { $as_echo "$as_me:${as_lineno-$LINENO}: checking for IOMasterPort in -framework IOKit" >&5
+$as_echo_n "checking for IOMasterPort in -framework IOKit... " >&6; }
 if test "${ac_cv_lib_IOKit_IOMasterPort+set}" = set; then :
   $as_echo_n "(cached) " >&6
 else
   ac_check_lib_save_LIBS=$LIBS
-LIBS="-lIOKit  $LIBS"
+LIBS="-framework IOKit  $LIBS"
 cat confdefs.h - <<_ACEOF >conftest.$ac_ext
 /* end confdefs.h.  */

END_OF_PATCH

# Patch makefiles to fix library flag
patch -p2 <<'END_OF_PATCH'
--- a/libdc1394-2.2.1/dc1394/macosx/Makefile.am 2013-01-27 18:43:18.000000000 -0800
+++ b/libdc1394-2.2.1/dc1394/macosx/Makefile.am   2014-01-16 15:29:43.000000000 -0800
@@ -10,7 +10,7 @@

 AM_CFLAGS = -I..
 libdc1394_macosx_la_LDFLAGS = -framework CoreFoundation -framework Carbon
-libdc1394_macosx_la_LIBADD = -lIOKit
+libdc1394_macosx_la_LIBADD = -framework IOKit
 libdc1394_macosx_la_SOURCES =  \
        control.c \
        capture.c \
END_OF_PATCH
patch -p2 <<'END_OF_PATCH'
--- a/libdc1394-2.2.1/dc1394/macosx/Makefile.in 2013-01-27 18:47:45.000000000 -0800
+++ b/libdc1394-2.2.1/dc1394/macosx/Makefile.in   2014-01-16 15:31:28.000000000 -0800
@@ -265,7 +265,7 @@

 AM_CFLAGS = -I..
 libdc1394_macosx_la_LDFLAGS = -framework CoreFoundation -framework Carbon
-libdc1394_macosx_la_LIBADD = -lIOKit
+libdc1394_macosx_la_LIBADD = -framework IOKit
 libdc1394_macosx_la_SOURCES = \
        control.c \
        capture.c \
END_OF_PATCH

eval ./configure $MM_DEPS_CONFIGUREFLAGS --disable-shared --enable-static --with-pic --disable-sdltest --disable-examples "LIBS=\"-framework IOKit\""
make $MM_PARALLELMAKEFLAG
make install
popd


#
# OpenCV
#

############### TODO check deployment target and sdkroot; set cflags and cxxflags (esp. -v)

unzip -oq ../downloads/opencv-2.4.13.6.zip
pushd opencv-2.4.13.6

patch -p1 <<'END_OF_PATCH'
--- opencv-2.4.13.6/CMakeLists.txt	2018-02-21 12:27:31.000000000 -0600
+++ opencv-patched/CMakeLists.txt	2025-05-09 22:29:23.007048086 -0500
@@ -36,23 +36,12 @@
 # --------------------------------------------------------------
 # Top level OpenCV project
 # --------------------------------------------------------------
-if(CMAKE_GENERATOR MATCHES Xcode AND XCODE_VERSION VERSION_GREATER 4.3)
-  cmake_minimum_required(VERSION 3.0)
-elseif(IOS)
-  cmake_minimum_required(VERSION 3.0)
-else()
-  cmake_minimum_required(VERSION 2.8.12.2)
-endif()
+cmake_minimum_required(VERSION 3.5)
 
 if(POLICY CMP0026)
   cmake_policy(SET CMP0026 NEW)
 endif()
 
-if (POLICY CMP0042)
-  # silence cmake 3.0+ warnings about MACOSX_RPATH
-  cmake_policy(SET CMP0042 OLD)
-endif()
-
 # must go before the project command
 set(CMAKE_CONFIGURATION_TYPES "Debug;Release" CACHE STRING "Configs" FORCE)
 if(DEFINED CMAKE_BUILD_TYPE AND CMAKE_VERSION VERSION_GREATER "2.8")
END_OF_PATCH

# OpenCV modules: highgui depends on imgproc; imgproc depends on core; OpenCVgrabber requires highgui and core
mkdir -p build-for-mm && cd build-for-mm
PKG_CONFIG_PATH=$MM_DEPS_PREFIX/lib/pkgconfig cmake \
-DBUILD_DOCS:BOOL=OFF \
-DBUILD_EXAMPLES:BOOL=OFF \
-DBUILD_PERF_TESTS:BOOL=OFF \
-DBUILD_SHARED_LIBS:BOOL=OFF \
-DBUILD_TESTS:BOOL=OFF \
-DBUILD_ZLIB:BOOL=OFF \
-DBUILD_opencv_core:BOOL=ON \
-DBUILD_opencv_imgproc:BOOL=ON \
-DBUILD_opencv_highgui:BOOL=ON \
-DBUILD_opencv_apps:BOOL=OFF \
-DBUILD_opencv_calib3d:BOOL=OFF \
-DBUILD_opencv_contrib:BOOL=OFF \
-DBUILD_opencv_features2d:BOOL=OFF \
-DBUILD_opencv_flann:BOOL=OFF \
-DBUILD_opencv_gpu:BOOL=OFF \
-DBUILD_opencv_java:BOOL=OFF \
-DBUILD_opencv_legacy:BOOL=OFF \
-DBUILD_opencv_ml:BOOL=OFF \
-DBUILD_opencv_nonfree:BOOL=OFF \
-DBUILD_opencv_objdetect:BOOL=OFF \
-DBUILD_opencv_ocl:BOOL=OFF \
-DBUILD_opencv_photo:BOOL=OFF \
-DBUILD_opencv_python:BOOL=OFF \
-DBUILD_opencv_stitching:BOOL=OFF \
-DBUILD_opencv_superres:BOOL=OFF \
-DBUILD_opencv_ts:BOOL=OFF \
-DBUILD_opencv_video:BOOL=OFF \
-DBUILD_opencv_videostab:BOOL=OFF \
-DBUILD_opencv_world:BOOL=OFF \
-DCMAKE_BUILD_TYPE:STRING=RelWithDebInfo \
-DCMAKE_C_COMPILER:STRING=/usr/bin/clang \
-DCMAKE_C_FLAGS:STRING="-v" \
-DCMAKE_CXX_COMPILER:STRING=/usr/bin/clang++ \
-DCMAKE_CXX_FLAGS:STRING="-v" \
-DCMAKE_INSTALL_PREFIX="$MM_DEPS_PREFIX" \
-DCMAKE_OSX_ARCHITECTURES:STRING="x86_64" \
-DCMAKE_OSX_DEPLOYMENT_TARGET:STRING=10.9 \
-DCMAKE_OSX_SYSROOT:STRING=$SDKROOT \
-DCMAKE_VERBOSE_MAKEFILE:BOOL=ON \
-DENABLE_PRECOMPILED_HEADERS:BOOL=ON \
-DWITH_1394:BOOL=ON \
-DWITH_CUDA:BOOL=OFF \
-DWITH_EIGEN:BOOL=OFF \
-DWITH_FFMPEG:BOOL=ON \
-DWITH_JASPER:BOOL=OFF \
-DWITH_JPEG:BOOL=OFF \
-DWITH_OPENCL:BOOL=OFF \
-DWITH_OPENEXR:BOOL=OFF \
-DWITH_OPENMP:BOOL=OFF \
-DWITH_OPENNI:BOOL=OFF \
-DWITH_PNG:BOOL=OFF \
-DWITH_TIFF:BOOL=OFF \
-DZLIB_INCLUDE_DIR:STRING=$SDKROOT/usr/include \
-DZLIB_LIBRARY:STRING=$SDKROOT/usr/lib/libz.dylib \
..
make $MM_PARALLELMAKEFLAG
make install
popd

#
# msgpack-c
#

tar xzf ../downloads/msgpack-cxx-7.0.0.tar.gz
pushd msgpack-cxx-7.0.0
mkdir -p build-for-mm && cd build-for-mm
cmake \
-DBUILD_SHARED_LIBS=OFF \
-DCMAKE_BUILD_TYPE=RelWithDebInfo \
-DCMAKE_INSTALL_PREFIX=$MM_DEPS_PREFIX \
-DCMAKE_VERBOSE_MAKEFILE=ON \
-DCMAKE_OSX_ARCHITECTURES=x86_64 \
-DCMAKE_OSX_DEPLOYMENT_TARGET=10.9 \
-DCMAKE_OSX_SYSROOT=$SDKROOT \
-DMSGPACK_USE_BOOST=OFF \
..
make $MM_PARALLELMAKEFLAG
make install
popd

#
#
#

echo "Finished building dependencies"
