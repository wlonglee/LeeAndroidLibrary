# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("lame")

#设置生成的so文件路径
SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../../libs/${ANDROID_ABI})

#生成动态共享库
add_library(
        # 设置编译成so库的名称
        lame-lib

        # 生成动态库或共享库，此处如果SHARED改为STATIC，其含义是生成静态库
        SHARED

        # 提供一个需要编译的源文件的相对路径(),native-lib.cpp就是需要编译的源文件
        native-lib.cpp
        lame/bitstream.c
        lame/encoder.c
        lame/fft.c
        lame/gain_analysis.c
        lame/id3tag.c
        lame/lame.c
        lame/mpglib_interface.c
        lame/newmdct.c
        lame/presets.c
        lame/psymodel.c
        lame/quantize.c
        lame/quantize_pvt.c
        lame/reservoir.c
        lame/set_get.c
        lame/tables.c
        lame/takehiro.c
        lame/util.c
        lame/vbrquantize.c
        lame/VbrTag.c
        lame/version.c
        )

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        lame-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})