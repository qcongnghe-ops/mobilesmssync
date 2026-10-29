#!/bin/sh
exec java -classpath "$0/../gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
