#!/usr/bin/env bash


ver=$(cat pom.xml|grep \<version\> | head -n1 | sed 's/.*>\(.*\)<.*/\1/')
echo "ver=${ver}"

cat src/main/java/com/dyrnq/distops/Constants.java

# ========== JS syntax check ==========
echo "Checking JS syntax..."
js_errors=0
while IFS= read -r jsfile; do
  node -c "$jsfile" 2>/dev/null || { echo "  FAIL: $jsfile"; js_errors=$((js_errors + 1)); }
done < <(find src/main/resources/WEB-INF/static/js -name '*.js')
if [ "$js_errors" -gt 0 ]; then
  echo "ERROR: $js_errors JS file(s) have syntax errors, aborting build."
  exit 1
fi
echo "JS syntax check passed."

#export JAVA_HOME=/usr/lib/jvm/corretto21
#export PATH=$PATH:${JAVA_HOME}/bin
# -Xms4G -Xmx4G

jvm_opts_array=();
TOTAL_MEM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
THRESHOLD_KB=4194304
if [ "$TOTAL_MEM_KB" -gt "$THRESHOLD_KB" ]; then
    jvm_opts_array+=("-Xms4G -Xmx4G")
fi



export MAVEN_OPTS="${jvm_opts_array[*]} -XX:+UseZGC -XX:+ZGenerational"
# 注意：-XX:+ZGenerational 是 JDK 21 的核心特性，它让 ZGC 能够像 G1 一样处理短命对象（Maven 构建过程中会产生海量的短命对象），性能显著提升。

mvn_opts_array=();
if [ -e ./settings.xml ]; then
  mvn_opts_array+=("-s ./settings.xml")
fi

./mvnw org.codehaus.mojo:versions-maven-plugin:2.16.2:display-dependency-updates  ${mvn_opts_array[*]}
./mvnw antrun:run@"Execute native2ascii-files"                                    ${mvn_opts_array[*]}
./mvnw clean package -Dmaven.test.skip=true                                       ${mvn_opts_array[*]}




