-injars immudb.jar
-injars lib/asm-all-3.3.jar (!META-INF/MANIFEST.MF)
-injars lib/jsr305-1.3.9.jar (!META-INF/MANIFEST.MF)
-injars lib/guava-r09.jar (!META-INF/MANIFEST.MF)
-injars lib/trove-3.0.0.jar (!META-INF/MANIFEST.MF)
-libraryjars lib/lucene-core-3.1.0.jar
-libraryjars /System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Classes/classes.jar
-outjars immudb-dist.jar

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

-keep class suneido.immudb.Database
-keep class suneido.immudb.ReadTransaction
-keep class suneido.immudb.Record
-keep class suneido.immudb.Table
-keep class suneido.immudb.DbHashTrie
-keep class gnu.trove.list.array.TIntArrayList

-dontnote com.google.common.**
