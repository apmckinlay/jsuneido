-injars jsuneido-raw.jar
-injars lib/asm-all-4.2.jar (!META-INF/MANIFEST.MF)
-injars lib/jsr305-1.3.9.jar (!META-INF/MANIFEST.MF)
-injars lib/guava-16.0.jar (!META-INF/MANIFEST.MF)
-injars lib/trove-3.0.3.jar (!META-INF/MANIFEST.MF)
-libraryjars lib/lucene-core-3.6.2.jar
-libraryjars  <java.home>/lib/rt.jar
-outjars jsuneido.jar

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