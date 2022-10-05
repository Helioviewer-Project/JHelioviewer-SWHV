
cmake_minimum_required(VERSION 2.8)

project(JHV_UM)

macro(build_doc target input)
    get_filename_component(base ${input} NAME_WE)
    set(outputHTML ${base}.html)
    add_custom_command(OUTPUT ${outputHTML} COMMAND /bin/sh -c \"
                            find ${CMAKE_CURRENT_SOURCE_DIR} \\\( -name \\*.png -or -name \\*.jpg -or -name \\*.css -or -name \\*.xslt -or -name \\*.js -or -name \\*.sh \\\) -exec cp {} ${CMAKE_CURRENT_BINARY_DIR} \\\; &&
                            cp -r ${CMAKE_CURRENT_SOURCE_DIR}/new ${CMAKE_CURRENT_BINARY_DIR} &&
                            cp -p ${CMAKE_CURRENT_SOURCE_DIR}/${input} ${CMAKE_CURRENT_BINARY_DIR} &&
                            mmd-xslt ${input} &&
                            sh ${CMAKE_CURRENT_BINARY_DIR}/youtube.sh ${CMAKE_CURRENT_BINARY_DIR}/${outputHTML} &&
                            cp ${CMAKE_CURRENT_BINARY_DIR}/${outputHTML} ${CMAKE_CURRENT_SOURCE_DIR}/${outputHTML}\"
                            DEPENDS ${input} WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR})
    add_custom_target(${target} ALL DEPENDS ${outputHTML})
endmacro()

build_doc(swhv_sum swhv_sum.mmd)
