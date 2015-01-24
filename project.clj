(defproject farbetter/mealy "0.7.9-SNAPSHOT"
  :description "State machine using core.async chans and running in a go block"
  :url "https://github.com/farbetter/mealy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[com.cemerick/clojurescript.test "0.3.3"]
            [lein-cljsbuild "1.0.3"]
            [lein-release "1.0.6"]]

  :deploy-repositories [["clojars" {:creds :gpg}]]
  :lein-release {:scm :git
                 :deploy-via :clojars}

  :profiles {:dev
             {:plugins [[com.keminglabs/cljx "0.5.0"]]
              :repl-options
              {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl
                                  cljx.repl-middleware/wrap-cljx]}}}

  :dependencies
  [[com.taoensso/encore "1.19.1"]
   [com.taoensso/timbre "3.3.1"]
   [org.clojure/clojure "1.6.0"]
   [org.clojure/clojurescript "0.0-2665"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :repl-options
  {:init-ns farbetter.mealy}

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/src/clj"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/src/cljs"
                   :rules :cljs}
                  {:source-paths ["test/cljx"]
                   :output-path "target/generated/test/clj"
                   :rules :clj}
                  {:source-paths ["test/cljx"]
                   :output-path "target/generated/test/cljs"
                   :rules :cljs}]}

  :cljsbuild
  {:builds
   {"code-simple" {:source-paths ["target/generated/src/cljs"]
                   :jar true
                   :compiler
                   {:output-to "target/simple/main.js"
                    :output-dir "target/simple"
                    :static-fns true
                    :optimizations :simple}}
    "test-simple" {:source-paths ["target/generated/src/cljs"
                                  "target/generated/test/cljs"]
                   :compiler
                   {:output-to
                    "target/test/simple/test_main.js"
                    :output-dir "target/test/simple"
                    :optimizations :simple}}}

   :test-commands
   {"slimer-simple" ["slimerjs" :runner
                     "target/test/simple/test_main.js"]
    "phantom-simple" ["phantomjs" :runner
                      "target/test/simple/test_main.js"]}}

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :source-paths ["target/generated/src/clj" "src/clj"]
  :resource-paths ["target/generated/src/cljs"]
  :test-paths ["target/generated/test/clj"]

  :aliases
  {"testclj" ["do"
              "clean,"
              "cljx" "once,"
              "test"]
   "testcljs" ["do"
               "clean,"
               "cljx" "once,"
               "cljsbuild" "once" "code-simple" "test-simple,"
               "cljsbuild" "test" "slimer-simple"]
   "testall" ["do"
              "testclj,"
              "testcljs"]})
