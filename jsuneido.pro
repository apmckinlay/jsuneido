# Need to supply input jSuneido raw JAR as -injars on command line
# Need to supply output jSuneido finished JAR as -outjars on the command line
-injars lib/asm-all-5.0.2.jar (!META-INF/MANIFEST.MF)
-injars lib/jsr305-1.3.9.jar (!META-INF/MANIFEST.MF)
-injars lib/guava-17.0.jar (!META-INF/MANIFEST.MF)
-injars lib/trove-3.0.3.jar (!META-INF/MANIFEST.MF)
-libraryjars lib/lucene-core-4.8.1.jar
-libraryjars lib/lucene-analyzers-common-4.8.1.jar
-libraryjars lib/lucene-queryparser-4.8.1.jar
-libraryjars  <java.home>/lib/rt.jar

-dontoptimize
-dontobfuscate

-keep public class suneido.** {
    public *;
}

-keep class suneido.immudb.BtreeKey
-keep class suneido.language.ParseFunction$Context

-keep class org.objectweb.asm.MethodVisitor

-dontnote com.google.common.**

-dontwarn java.lang.invoke.MethodHandle
-dontwarn com.google.common.util.concurrent.ServiceManager

-keepattributes *Annotation*
