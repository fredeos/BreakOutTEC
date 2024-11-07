CC = gcc
FLAGS = -o

NET = C/modules/network/*.c
LIB = C/libs/*.c

I = -I/usr/include -I/usr/local/include -Isrc/include
L = -L/usr/lib -L/usr/lib/x86_64-linux-gnu -L/usr/local/lib

CUUID = -luuid
CJSON = -ljson-c

client:
	${CC} ${NET} ${I} ${L} `pkg-config --cflags gtk+-3.0` -o ./C/out/$@ ./C/game.c `pkg-config --libs gtk+-3.0` ${CUUID} ${CJSON}
	./C/out/$@

java_compile:
	cd java/Breakout; mvn compile

c_tests:
	${CC} ${NET} ${I} ${L} -o ./C/out/$@ ./C/test.c ${CUUID} ${CJSON}
	./C/out/$@

java_test:
	cd java/Breakout; mvn exec:java -Dexec.mainClass="breakout.app.Program"

server:
	cd java/Breakout; mvn exec:java -Dexec.mainClass="breakout.app.View.MainWindow"
	
clean:
	rm -r ./out/*