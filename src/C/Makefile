CC = gcc
FLAGS = -o

MDL = modules/*.c
LIB = libs/*.c

I = -I/usr/include -I/usr/local/include -Isrc/include
L = -L/usr/lib -L/usr/lib/x86_64-linux-gnu -L/usr/local/lib

example:
	${CC} ${FLAGS} ./out/$@ file.c
	./out/$@

clean:
	rm -r ./out/*
