# android-retrolambda-lombok
A modified version of lombok ast that allows lint to run on java 8 sources without error.

## Usage
All you have to do is modify you `build.gradle` as such
```groovy
buildscript {
    repositories {
        maven {
            url  "http://dl.bintray.com/evant/maven"
        }
        ...
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:<version>'
        classpath "me.tatarka:gradle-retrolambda:<version>"
        classpath 'me.tatarka.retrolambda.projectlombok:lombok.ast:0.2.3.a1'
    }

    // Exclude the version that the android plugin depends on.
    configurations.classpath.exclude group: 'com.android.tools.external.lombok'
}
```

## Limitations
- Currently you must run gradle with java 8. This may or may not be fixed in the future depending on if I can get lombok to compile with a lower java version.
