-injars jsuneido.jar
-libraryjars /System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Classes/classes.jar
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
-keep class com.google.common.collect.ImmutableList
-keep class suneido.language.CompileGenerator$Stack
-keep class suneido.database.query.Row$Which
-keep class suneido.database.query.Join$Type