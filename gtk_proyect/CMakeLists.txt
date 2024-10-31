cmake_minimum_required(VERSION 3.10)
project(gtk_project)

# Especifica la versión de C++
set(CMAKE_CXX_STANDARD 11)

# Buscar GTK3
find_package(PkgConfig REQUIRED)
pkg_check_modules(GTK REQUIRED gtk+-3.0)

# Incluir las rutas de GTK y enlazar las bibliotecas
include_directories(${GTK_INCLUDE_DIRS})
link_directories(${GTK_LIBRARY_DIRS})
add_definitions(${GTK_CFLAGS_OTHER})

# Crear el ejecutable
add_executable(gtk_project main.c)

# Enlazar GTK al ejecutable
target_link_libraries(gtk_project ${GTK_LIBRARIES})
