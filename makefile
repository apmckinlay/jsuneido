# NOTE: requires GNU Make 4 for "file" function
# on Windows cp and rm are required (e.g. from gnuwin32)

rwildcard=$(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2) $(filter $(subst *,%,$2),$d))

SRC:=$(call rwildcard,src/main,*.java)

JDK:=$(JAVA_HOME)/bin

TARGET:=target

.PHONY: all classes jar proguard clean

all: compile jar proguard

compile:
	$(file >srcs, $(SRC))
	"$(JDK)/javac" -d $(TARGET) -cp "lib/*" --release 8 @srcs

jar:
	"$(JDK)/jar" -cfm $(TARGET)/jsuneido.jar manifest.mf -C $(TARGET) suneido
	cp $(TARGET)/jsuneido.jar .

proguard:
	$(JDK)/java -jar lib2/proguard.jar -injars $(TARGET)/jsuneido.jar @jsuneido.pro -outjars jsuneido.jar

JRE_MODS:=--add-modules java.management,jdk.management,jdk.httpserver,java.xml,java.logging,jdk.unsupported

jre:
	rm -rf $(TARGET)/jre
	"$(JDK)/jlink" -p "$(JAVA_HOME)/jmods" $(JRE_MODS) --compress=2 --vm=server --output target/jre

clean:
	rm -rf srcs $(TARGET)/suneido $(TARGET)/*.jar $(TARGET)/*.class $(TARGET)/jre
