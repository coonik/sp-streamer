#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG="`dirname "$PRG"`/$link"
  fi
done

APP_HOME=`dirname "$PRG"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

# OS specific support. $var _must_ be set to either true or false.
cygwin=false
msys=false
darwin=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  Darwin* )
    darwin=true
    ;;
esac

# For Darwin, add path to native OSX libraries
if $darwin; then
  DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS -Djava.library.path=$APP_HOME/lib"
fi

# Look for the user's JDK installation
if [ -z "$JAVA_HOME" ] ; then
  if $darwin; then
    if [ -x '/usr/libexec/java_home' ]; then
      JAVA_HOME=`/usr/libexec/java_home`
    fi
  fi
fi

if [ -z "$JAVA_HOME" ] ; then
  echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
  exit 1
fi

JAVA_EXEC="$JAVA_HOME/bin/java"

# Check if Java executable exists
if [ ! -x "$JAVA_EXEC" ]; then
  echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
  echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation."
  exit 1
fi

# Change directory to APP_HOME
cd "$APP_HOME"

exec "$JAVA_EXEC" $DEFAULT_JVM_OPTS -cp "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"