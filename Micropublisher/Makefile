APP=PhotoShare
JAR_LIB_PATH=../../src/libhaggle/jni
JAR_LIB_NAME=org.haggle.jar
JAR_LIB_DEPS=${JAR_LIB_PATH}/${JAR_LIB_NAME}
EXTRA_JARS=$(patsubst %,libs/%,$(notdir ${JAR_LIB_DEPS}))
SRC=src/org/haggle/${APP}/*.java
DEBUG=bin/${APP}-debug
RELEASE=bin/${APP}-unsigned
ANT=ant -q
TOP_BIN_DIR=../../bin

all: debug

debug: ${DEBUG}.apk
release: ${RELEASE}.apk

${JAR_LIB_DEPS}:
	$(MAKE) -C ${JAR_LIB_PATH}

${EXTRA_JARS}: ${JAR_LIB_DEPS}
	mkdir -p libs
	cp -f ${JAR_LIB_DEPS} libs/

${DEBUG}.apk: ${EXTRA_JARS} build.xml ${SRC}
	${ANT} debug
	cp -f $@ ${TOP_BIN_DIR}

${RELEASE}.apk: ${EXTRA_JARS} build.xml ${SRC}
	${ANT} release
	cp -f $@ ${TOP_BIN_DIR}

deploy: debug
	adb uninstall org.haggle.${APP}
	adb install ${DEBUG}.apk

clean:
	rm -f *~
	ant -q clean

