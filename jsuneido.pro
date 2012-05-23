-injars jsuneido.jar
-injars lib/asm-all-3.3.jar (!META-INF/MANIFEST.MF)
-injars lib/jsr305-1.3.9.jar (!META-INF/MANIFEST.MF)
-injars lib/guava-11.0.1.jar (!META-INF/MANIFEST.MF)
-injars lib/trove-3.0.0.jar (!META-INF/MANIFEST.MF)
-libraryjars lib/lucene-core-3.1.0.jar
-libraryjars /System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Classes/classes.jar
-outjars jsuneido-dist.jar

-dontoptimize
-dontobfuscate

-keep public class suneido.** {
	<methods>;
}

-keep class org.objectweb.asm.Label
-keep class com.google.common.base.Splitter
-keep class com.google.common.collect.BiMap
-keep class com.google.common.collect.ImmutableList
-keep class suneido.database.query.Row$Which
-keep class suneido.database.query.Join$Type
-keep class suneido.language.ParseFunction$Context
-keep class suneido.language.AstCompile$VarArgs
-keep class suneido.language.AstCompile$ExprOption
-keep class suneido.language.AstCompile$ExprType

-dontnote com.google.common.**
