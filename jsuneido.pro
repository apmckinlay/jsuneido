# Need to supply input jSuneido raw JAR as -injars on command line
# Need to supply output jSuneido finished JAR as -outjars on the command line
-injars lib/asm-all-5.0.4.jar (!META-INF/MANIFEST.MF)
-injars lib/jsr305-1.3.9.jar (!META-INF/MANIFEST.MF)
-injars lib/guava-22.0.jar (!META-INF/MANIFEST.MF)
-injars lib/trove-3.0.3.jar (!META-INF/MANIFEST.MF)
-libraryjars lib/lucene-core-4.10.3.jar
-libraryjars lib/lucene-analyzers-common-4.10.3.jar
-libraryjars lib/lucene-queryparser-4.10.3.jar
-libraryjars lib/lucene-highlighter-4.10.3.jar
-libraryjars <java.home>/lib/rt.jar

-dontoptimize
-dontobfuscate

-keep public class suneido.** {
    public *;
}
-keep public class suneido.runtime.Ops {
    public static java.lang.Throwable exception(java.lang.Object);
}

-keep class suneido.immudb.BtreeKey
-keep class suneido.compiler.ParseFunction$Context

-keep class org.objectweb.asm.MethodVisitor

-dontnote com.google.common.**

-dontwarn java.lang.invoke.MethodHandle
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.crypto.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keepattributes *Annotation*
