# Keep JSDI platform-specific factory classes
# If you want to make a JAR without platform-specific code, it should be done
# by excluding the relevant abi.XXX package at the compile/raw JAR stage, before
# ProGuard runs.
-keep class suneido.jsdi.DllFactory { protected <init>(suneido.jsdi.JSDI); }
-keep class suneido.jsdi.Factory { protected <init>(suneido.jsdi.JSDI); }
-keep class suneido.jsdi.abi.amd64.DllFactory64 { <init>(suneido.jsdi.JSDI); }
-keep class suneido.jsdi.abi.amd64.Factory64 { <init>(suneido.jsdi.JSDI); }
-keep class suneido.jsdi.abi.amd64.ThunkManager64 { <init>(suneido.jsdi.JSDI); }
