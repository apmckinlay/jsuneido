-injars jsuneido.jar
-libraryjars  <java.home>/lib/rt.jar
-libraryjars lib/jsr305-1.3.9.jar
-outjars jsuneido-dist.jar

-dontoptimize
-dontobfuscate

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

-keep public class suneido.language.Ops {
	<methods>;
}

-keep public class suneido.language.Su* {
	<methods>;
}

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