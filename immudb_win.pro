-injars immudb.jar
-injars lib/asm-all-3.3.jar (!META-INF/MANIFEST.MF)
-injars lib/jsr305-1.3.9.jar (!META-INF/MANIFEST.MF)
-injars lib/guava-12.0.1.jar (!META-INF/MANIFEST.MF)
-injars lib/trove-3.0.3.jar (!META-INF/MANIFEST.MF)
-libraryjars lib/lucene-core-3.1.0.jar
-libraryjars  <java.home>/lib/rt.jar
-outjars immudb-dist.jar

-dontoptimize
-dontobfuscate

-keep public class suneido.** {
    public *;
}

-keep class suneido.immudb.BtreeKey
-keep class suneido.language.ParseFunction$Context

-keep class org.objectweb.asm.MethodVisitor

-dontnote com.google.common.**
