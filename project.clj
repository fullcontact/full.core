(defproject fullcontact/full.core "0.10.0-SNAPSHOT"
  :description "FullContact's core Clojure(Script) library - logging, configuration and sugar."
  :url "https://github.com/fullcontact/full.core"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :deploy-repositories [["releases" {:url "https://clojars.org/repo/" :creds :gpg}]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [org.slf4j/jul-to-slf4j "1.7.12"]
                 [me.moocar/logback-gelf "0.12" :exclusions [org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]
                 [clj-yaml "0.4.0" :exclusions [org.yaml/snakeyaml]]
                 [org.yaml/snakeyaml "1.15"]
                 [commons-codec/commons-codec "1.10"]]
  :aliases {"at" ["test-refresh"]
            "ats" ["do" "clean," "cljsbuild" "auto" "test"]}
  :aot :all
  :cljsbuild {:test-commands {"test" ["phantomjs" :runner "target/test.js"]}
              :builds [
                       {:id "test"
                        :notify-command ["phantomjs" :cljs.test/runner "target/test.js"]
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]}
  :profiles {:dev {:plugins [[com.jakemccrary/lein-test-refresh "0.15.0"]
                             [lein-cljsbuild "1.1.3"]
                             [com.cemerick/clojurescript.test "0.3.3"]]}})
