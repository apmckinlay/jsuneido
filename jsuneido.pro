-injars jsuneido-raw.jar
-injars lib/asm-all-5.0.2.jar (!META-INF/MANIFEST.MF)
-injars lib/jsr305-1.3.9.jar (!META-INF/MANIFEST.MF)
-injars lib/guava-17.0.jar (!META-INF/MANIFEST.MF)
-injars lib/trove-3.0.3.jar (!META-INF/MANIFEST.MF)
-libraryjars lib/lucene-core-4.8.1.jar
-libraryjars lib/lucene-analyzers-common-4.8.1.jar
-libraryjars lib/lucene-queryparser-4.8.1.jar
-libraryjars  <java.home>/lib/rt.jar
-outjars jsuneido.jar

-dontoptimize
-dontobfuscate

-keep public class suneido.** {
    public *;
}

# Keep JSDI platform-specific factory classes
# If you want to make a JAR without platform-specific code, it should be done
# by excluding the relevant abi.XXX package at the compile/raw JAR stage, before
# ProGuard runs.
-keep class suneido.jsdi.DllFactory { protected <init>(suneido.jsdi.JSDI); }
-keep class suneido.jsdi.Factory { protected <init>(suneido.jsdi.JSDI); }
-keep class suneido.jsdi.abi.x86.DllFactoryX86 { <init>(suneido.jsdi.JSDI); }
-keep class suneido.jsdi.abi.x86.FactoryX86 { <init>(suneido.jsdi.JSDI); }
-keep class suneido.jsdi.abi.x86.ThunkManagerX86 { <init>(suneido.jsdi.JSDI); }

-keep class suneido.immudb.BtreeKey
-keep class suneido.language.ParseFunction$Context

-keep class org.objectweb.asm.MethodVisitor

-dontnote com.google.common.**

-dontwarn java.lang.invoke.MethodHandle
-dontwarn com.google.common.util.concurrent.ServiceManager

-keepattributes *Annotation*
